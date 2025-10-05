import os
import subprocess
import argparse
import glob
import sys
import time
import json
import re
from pathlib import Path

# 大型las文件分段代码 - 增强版，支持归一化参数提取
# python lastile_wrapper.py --input 输入文件.las --output_dir 输出目录 --prefix 输出前缀
# python lastile_wrapper.py --input D:\点云数据\大型点云.las --output_dir D:\点云数据\输出 --prefix 小块点云 --tile_size 500 --buffer 10
# 主要用这个产生分段、json、头部文件等等：python lastile_wrapper.py --input D:\点云数据\大型点云.las --output_dir D:\点云数据\输出 --prefix 电力线点云 --generate_threejs_params

# === 写死 EXE 路径 ===
LASINFO_EXE = r"C:\Users\31591\Desktop\project\src\main\resources\python\lastools\lasinfo64.exe"
LASTILE_EXE = r"C:\Users\31591\Desktop\project\src\main\resources\python\lastools\lastile64.exe"

def parse_lasinfo_file(lasinfo_file):
    """
    解析lasinfo.txt文件，提取归一化参数
    
    参数:
        lasinfo_file (str): lasinfo.txt文件路径
        
    返回:
        dict: 包含归一化参数的字典，如果解析失败返回None
    """
    params = {}
    
    try:
        with open(lasinfo_file, 'r', encoding='utf-8', errors='ignore') as f:
            content = f.read()
        
        # 提取各种参数使用正则表达式
        patterns = {
            'min_xyz': r'min x y z:\s+([\d.-]+)\s+([\d.-]+)\s+([\d.-]+)',
            'max_xyz': r'max x y z:\s+([\d.-]+)\s+([\d.-]+)\s+([\d.-]+)',
            'scale_xyz': r'scale factor x y z:\s+([\d.-]+)\s+([\d.-]+)\s+([\d.-]+)',
            'offset_xyz': r'offset x y z:\s+([\d.-]+)\s+([\d.-]+)\s+([\d.-]+)',
            'point_count': r'number of point records:\s+(\d+)',
            'version': r'version major\.minor:\s+([\d.]+)',
            'point_format': r'point data format:\s+(\d+)',
        }
        
        for key, pattern in patterns.items():
            match = re.search(pattern, content)
            if match:
                if key in ['min_xyz', 'max_xyz', 'scale_xyz', 'offset_xyz']:
                    params[key] = [float(match.group(1)), float(match.group(2)), float(match.group(3))]
                elif key == 'point_count':
                    params[key] = int(match.group(1))
                else:
                    params[key] = match.group(1)
        
        # 检查是否提取到了基本参数
        required_params = ['min_xyz', 'max_xyz', 'scale_xyz', 'offset_xyz', 'point_count']
        missing_params = [p for p in required_params if p not in params]
        
        if missing_params:
            print(f"警告: 未能从lasinfo文件中提取到以下参数: {missing_params}")
            return None
        
        return params
        
    except Exception as e:
        print(f"解析lasinfo文件时发生错误: {e}")
        return None

def calculate_normalization_params(las_params):
    """
    基于LAS参数计算Three.js需要的归一化参数
    
    参数:
        las_params (dict): 从lasinfo文件解析的参数
        
    返回:
        dict: Three.js归一化参数
    """
    if not las_params:
        return None
    
    min_x, min_y, min_z = las_params['min_xyz']
    max_x, max_y, max_z = las_params['max_xyz']
    scale_x, scale_y, scale_z = las_params['scale_xyz']
    offset_x, offset_y, offset_z = las_params['offset_xyz']
    
    # 计算实际坐标范围
    range_x = max_x - min_x
    range_y = max_y - min_y
    range_z = max_z - min_z
    
    # 计算中心点
    center_x = (min_x + max_x) / 2
    center_y = (min_y + max_y) / 2
    center_z = 20
    
    # 计算最大范围（用于统一缩放）
    max_range = max(range_x, range_y, range_z)
    
    # 建议的归一化缩放因子（将数据缩放到[-1, 1]范围）
    normalization_scale = 1.0
    
    # Three.js归一化参数
    threejs_params = {
        "original_bounds": {
            "min": {"x": min_x, "y": min_y, "z": min_z},
            "max": {"x": max_x, "y": max_y, "z": max_z}
        },
        "center": {
            "x": center_x,
            "y": center_y,
            "z": center_z
        },
        "range": {
            "x": range_x,
            "y": range_y,
            "z": range_z,
            "max": max_range
        },
        "scale_factors": {
            "x": scale_x,
            "y": scale_y,
            "z": scale_z
        },
        "offset": {
            "x": offset_x,
            "y": offset_y,
            "z": offset_z
        },
        "normalization": {
            "scale": normalization_scale,
            "translate": {
                "x": -center_x,
                "y": -center_y,
                "z": -center_z
            }
        },
        "point_count": las_params['point_count'],
        "metadata": {
            "version": las_params.get('version', 'unknown'),
            "point_format": las_params.get('point_format', 'unknown')
        }
    }
    
    return threejs_params

def save_normalization_params(params, output_file):
    """
    保存归一化参数到JSON文件
    
    参数:
        params (dict): 归一化参数
        output_file (str): 输出JSON文件路径
    """
    try:
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(params, f, indent=2, ensure_ascii=False)
        print(f"归一化参数已保存到: {output_file}")
        return True
    except Exception as e:
        print(f"保存归一化参数时发生错误: {e}")
        return False

def run_lasinfo(input_file, output_dir, prefix):
    """
    调用lasinfo.exe分析LAS文件头部信息
    
    参数:
        input_file (str): 输入LAS/LAZ文件路径
        output_dir (str): 输出目录路径
        prefix (str): 输出文件前缀
    """
    # 构建输出文件名
    output_file = os.path.join(output_dir, f"{prefix}_lasinfo.txt")
    
    # 构建命令
    cmd = [LASINFO_EXE, "-i", input_file, "-o", output_file]
    
    print(f"\n{'='*70}")
    print(f"开始执行lasinfo分析文件: {input_file}")
    print(f"输出将保存到: {output_file}")
    print(f"{'='*70}")
    
    try:
        # 使用Popen来实时获取输出
        process = subprocess.Popen(
            cmd, 
            stdout=subprocess.PIPE, 
            stderr=subprocess.STDOUT,  # 将stderr重定向到stdout
            text=True,
            bufsize=1  # 行缓冲
        )
        
        # 实时读取并显示输出
        print("\n[lasinfo执行过程输出开始]\n")
        for line in iter(process.stdout.readline, ''):
            print(line, end='')  # 实时打印每行输出
            sys.stdout.flush()   # 确保输出立即显示
        
        # 等待进程结束并获取返回码
        return_code = process.wait()
        print("\n[lasinfo执行过程输出结束]\n")
        
        if return_code == 0:
            print(f"lasinfo分析完成，结果保存在: {output_file}")
            return output_file
        else:
            print(f"lasinfo分析失败，返回码: {return_code}")
            return None
    
    except Exception as e:
        print(f"\nlasinfo执行过程中发生异常: {e}")
        return None

def run_lastile(input_file, output_dir, output_prefix, tile_size=1000, buffer_size=20, 
                demo=True, files_are_flightlines=False, cores=1, format="las"):
    """
    调用lastile64.exe进行LAS/LAZ文件的瓦片划分
    
    参数:
        input_file (str): 输入LAS/LAZ文件路径
        output_dir (str): 输出目录路径
        output_prefix (str): 输出文件前缀
        tile_size (int): 瓦片大小，默认为1000
        buffer_size (int): 缓冲区大小，默认为20
        demo (bool): 是否使用demo模式，默认为True
        files_are_flightlines (bool): 输入文件是否为航线数据，默认为False
        cores (int): 使用的核心数，默认为1
        format (str): 输出格式，"las"或"laz"，默认为"las"
    """
    # 确保输出目录存在
    os.makedirs(output_dir, exist_ok=True)
    
    # 先运行lasinfo分析文件头部
    lasinfo_file = run_lasinfo(input_file, output_dir, output_prefix)
    
    # 构建lastile命令
    cmd = [LASTILE_EXE]
    
    # 添加输入文件
    cmd.extend(["-i", input_file])
    
    # 设置瓦片大小
    cmd.extend(["-tile_size", str(tile_size)])
    
    # 设置缓冲区
    if buffer_size > 0:
        cmd.extend(["-buffer", str(buffer_size)])
    
    # 设置输出目录和前缀
    cmd.extend(["-odir", output_dir])
    cmd.extend(["-o", f"{output_prefix}.{format}"])
    
    # 设置输出格式
    if format.lower() == "las":
        cmd.append("-olas")
    else:
        cmd.append("-olaz")
    
    # 如果是航线数据
    if files_are_flightlines:
        cmd.append("-files_are_flightlines")
    
    # 设置核心数
    if cores > 1:
        cmd.extend(["-cores", str(cores)])
    
    # 使用demo模式
    if demo:
        cmd.append("-demo")
    
    # 执行命令并实时显示输出
    print("\n" + "=" * 70)
    print("开始执行lastile瓦片划分命令:", " ".join(cmd))
    print("=" * 70)
    
    try:
        # 使用Popen来实时获取输出
        process = subprocess.Popen(
            cmd, 
            stdout=subprocess.PIPE, 
            stderr=subprocess.STDOUT,  # 将stderr重定向到stdout
            text=True,
            bufsize=1  # 行缓冲
        )
        
        # 实时读取并显示输出
        print("\n[lastile执行过程输出开始]\n")
        for line in iter(process.stdout.readline, ''):
            print(line, end='')  # 实时打印每行输出
            sys.stdout.flush()   # 确保输出立即显示
        
        # 等待进程结束并获取返回码
        return_code = process.wait()
        print("\n[lastile执行过程输出结束]\n")
        
        if return_code == 0:
            print("lastile命令执行成功，返回码:", return_code)
            return True, lasinfo_file
        else:
            print("lastile命令执行失败，返回码:", return_code)
            return False, lasinfo_file
    
    except Exception as e:
        print(f"\nlastile执行过程中发生异常: {e}")
        return False, lasinfo_file

def main():
    parser = argparse.ArgumentParser(description="使用lastile64对LAS/LAZ文件进行瓦片划分并生成Three.js归一化参数")
    parser.add_argument("--input", "-i", required=True, help="输入文件路径 (例如: 'data.las')")
    parser.add_argument("--output_dir", "-o", required=True, help="输出目录路径")
    parser.add_argument("--prefix", "-p", required=True, help="输出文件前缀")
    parser.add_argument("--tile_size", "-t", type=int, default=1000, help="瓦片大小 (默认: 1000)")
    parser.add_argument("--buffer", "-b", type=int, default=20, help="缓冲区大小 (默认: 20)")
    parser.add_argument("--format", "-f", choices=["las", "laz"], default="las", help="输出格式 (默认: las)")
    parser.add_argument("--flightlines", action="store_true", help="输入文件是否为航线数据")
    parser.add_argument("--cores", "-c", type=int, default=1, help="使用的核心数 (默认: 1)")
    parser.add_argument("--no_demo", action="store_true", help="不使用demo模式 (默认使用demo)")
    parser.add_argument("--generate_threejs_params", "-threejs", action="store_true", 
                       help="生成Three.js归一化参数JSON文件")
    parser.add_argument("--only_params", action="store_true", 
                       help="仅生成归一化参数，不执行瓦片划分")
    
    args = parser.parse_args()
    
    # 检查输入文件是否存在
    input_file = args.input
    if not os.path.exists(input_file):
        print(f"错误: 文件 '{input_file}' 不存在")
        return False
    
    print(f"\n将处理文件: {input_file}")
    
    print("\n处理配置:")
    print(f"  输出目录: {args.output_dir}")
    print(f"  输出前缀: {args.prefix}")
    if not args.only_params:
        print(f"  瓦片大小: {args.tile_size}")
        print(f"  缓冲区大小: {args.buffer}")
        print(f"  输出格式: {args.format}")
        print(f"  航线数据: {'是' if args.flightlines else '否'}")
        print(f"  使用核心数: {args.cores}")
        print(f"  Demo模式: {'否' if args.no_demo else '是'}")
    print(f"  生成Three.js参数: {'是' if args.generate_threejs_params else '否'}")
    print(f"  仅生成参数: {'是' if args.only_params else '否'}")
    
    start_time = time.time()
    
    # 确保输出目录存在
    os.makedirs(args.output_dir, exist_ok=True)
    
    success = True
    lasinfo_file = None
    
    if args.only_params:
        # 仅生成归一化参数
        print("\n" + "=" * 70)
        print("仅执行lasinfo分析以生成归一化参数")
        print("=" * 70)
        lasinfo_file = run_lasinfo(args.input, args.output_dir, args.prefix)
        success = lasinfo_file is not None
    else:
        # 运行完整的lastile处理
        success, lasinfo_file = run_lastile(
            input_file=input_file,
            output_dir=args.output_dir,
            output_prefix=args.prefix,
            tile_size=args.tile_size,
            buffer_size=args.buffer,
            demo=not args.no_demo,
            files_are_flightlines=args.flightlines,
            cores=args.cores,
            format=args.format
        )
    
    # 生成Three.js归一化参数
    if (success and lasinfo_file and args.generate_threejs_params) or args.only_params:
        print("\n" + "=" * 70)
        print("开始生成Three.js归一化参数")
        print("=" * 70)
        
        # 解析lasinfo文件
        las_params = parse_lasinfo_file(lasinfo_file)
        
        if las_params:
            print("成功解析LAS参数:")
            print(f"  点数量: {las_params['point_count']:,}")
            print(f"  X范围: {las_params['min_xyz'][0]:.2f} ~ {las_params['max_xyz'][0]:.2f}")
            print(f"  Y范围: {las_params['min_xyz'][1]:.2f} ~ {las_params['max_xyz'][1]:.2f}")
            print(f"  Z范围: {las_params['min_xyz'][2]:.2f} ~ {las_params['max_xyz'][2]:.2f}")
            
            # 计算Three.js归一化参数
            threejs_params = calculate_normalization_params(las_params)
            
            if threejs_params:
                # 保存到JSON文件
                json_file = os.path.join(args.output_dir, f"{args.prefix}_threejs_params.json")
                if save_normalization_params(threejs_params, json_file):
                    print(f"\nThree.js归一化参数:")
                    print(f"  数据中心: ({threejs_params['center']['x']:.2f}, {threejs_params['center']['y']:.2f}, {threejs_params['center']['z']:.2f})")
                    print(f"  最大范围: {threejs_params['range']['max']:.2f}")
                    print(f"  建议缩放因子: {threejs_params['normalization']['scale']:.6f}")
                    print(f"  JSON文件: {os.path.abspath(json_file)}")
                else:
                    print("保存Three.js参数失败")
            else:
                print("计算Three.js归一化参数失败")
        else:
            print("解析lasinfo文件失败，无法生成Three.js参数")
    
    end_time = time.time()
    elapsed_time = end_time - start_time
    
    print("\n" + "=" * 70)
    print(f"总耗时: {elapsed_time:.2f} 秒 ({time.strftime('%H:%M:%S', time.gmtime(elapsed_time))})")
    
    if success:
        if not args.only_params:
            print(f"瓦片划分完成，输出文件在目录: {os.path.abspath(args.output_dir)}")
            # 尝试列出生成的文件
            try:
                output_files = glob.glob(os.path.join(args.output_dir, f"{args.prefix}*.{args.format}"))
                if output_files:
                    print(f"\n生成了 {len(output_files)} 个瓦片文件")
                    for idx, file in enumerate(output_files[:10], 1):  # 只显示前10个
                        print(f"  {idx}. {os.path.basename(file)}")
                    if len(output_files) > 10:
                        print(f"  ... 以及 {len(output_files) - 10} 个其他文件")
            except Exception as e:
                print(f"注意: 无法列出生成的文件: {e}")
        
        # 显示lasinfo文件位置
        if lasinfo_file and os.path.exists(lasinfo_file):
            print(f"\nLAS文件头部分析结果保存在: {os.path.abspath(lasinfo_file)}")
        
        # 显示Three.js参数文件位置
        if args.generate_threejs_params or args.only_params:
            json_file = os.path.join(args.output_dir, f"{args.prefix}_threejs_params.json")
            if os.path.exists(json_file):
                print(f"Three.js归一化参数保存在: {os.path.abspath(json_file)}")
    else:
        if args.only_params:
            print("生成归一化参数失败，请检查上面的错误信息")
        else:
            print("瓦片划分失败，请检查上面的错误信息")
    print("=" * 70)

if __name__ == "__main__":
    main()