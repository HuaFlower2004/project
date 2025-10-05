import os
import glob
import laspy
import numpy as np
import argparse
import sys
import time
from tqdm import tqdm

# 提取出的电力线合并脚本
# 这个好像不怎么需要使用
# python merge_powerlines.py --input_dir 输入目录 --output_dir 输出目录
# python merge_powerlines.py --input_dir D:\点云数据\提取结果 --output_dir D:\点云数据\合并结果
# python merge_powerlines.py --input_dir D:\点云数据\提取结果 --output_dir D:\点云数据\合并结果 --file_pattern "powerline_*.las"

def get_available_filename(base_path):
    """
    生成一个可用的文件名，如果文件已存在则添加序号
    
    参数:
        base_path: 基本文件路径
        
    返回:
        可用的文件路径
    """
    if not os.path.exists(base_path):
        return base_path
    
    # 分解文件路径
    directory = os.path.dirname(base_path)
    filename = os.path.basename(base_path)
    name, ext = os.path.splitext(filename)
    
    # 尝试添加序号
    counter = 1
    while True:
        new_path = os.path.join(directory, f"{name}-{counter}{ext}")
        if not os.path.exists(new_path):
            return new_path
        counter += 1

def merge_powerline_files(input_dir, output_dir, file_pattern="*/all_power_lines.las", output_filename="merged_powerlines.las", auto_increment=True):
    """
    合并多个瓦片中提取出的电力线为一个LAS文件
    
    参数:
        input_dir: 输入目录，包含各瓦片处理结果
        output_dir: 输出目录路径
        file_pattern: 电力线文件匹配模式
        output_filename: 输出文件名
        auto_increment: 是否自动为输出文件添加序号
    """
    # 确保输出目录存在
    os.makedirs(output_dir, exist_ok=True)
    
    # 构建完整的输出文件路径
    output_file = os.path.join(output_dir, output_filename)
    
    # 如果启用自动添加序号，获取可用的文件名
    if auto_increment:
        output_file = get_available_filename(output_file)
        output_filename = os.path.basename(output_file)
    
    # 查找所有电力线文件
    powerline_files = glob.glob(os.path.join(input_dir, file_pattern))
    
    if not powerline_files:
        print(f"错误: 在 {input_dir} 中没有找到匹配 '{file_pattern}' 的文件")
        return False
    
    print(f"找到 {len(powerline_files)} 个电力线文件，准备合并...")
    
    # 读取所有电力线点
    all_points = []
    all_classifications = []
    header_template = None
    
    for idx, file_path in enumerate(tqdm(powerline_files, desc="读取电力线文件")):
        try:
            las_data = laspy.read(file_path)
            
            # 如果是第一个文件，保存头信息作为模板
            if header_template is None:
                header_template = las_data.header
            
            # 提取点坐标和分类信息
            points = np.vstack((las_data.x, las_data.y, las_data.z)).T
            classifications = las_data.classification
            
            # 根据需要重新分配分类值（可选）
            # 这里可以对分类值进行修改，例如添加偏移以区分不同瓦片的电力线
            # classifications = classifications + (idx * 10)  # 每个瓦片的分类值增加10
            
            all_points.append(points)
            all_classifications.append(classifications)
            
            print(f"读取文件: {file_path} - {len(points):,} 个点")
            
        except Exception as e:
            print(f"警告: 处理文件 {file_path} 时出错: {e}")
    
    # 检查是否有点需要合并
    if not all_points:
        print("错误: 没有有效的点可以合并")
        return False
    
    # 合并所有点
    merged_points = np.vstack(all_points)
    merged_classifications = np.hstack(all_classifications)
    
    print(f"合并了 {len(merged_points):,} 个点")
    
    # 创建输出文件
    out_las = laspy.create(file_version=header_template.version)
    out_las.x = merged_points[:, 0]
    out_las.y = merged_points[:, 1]
    out_las.z = merged_points[:, 2]
    out_las.classification = merged_classifications
    
    # 保存合并后的文件
    out_las.write(output_file)
    print(f"合并完成，结果保存到: {output_file}")
    
    # 保存合并信息
    timestamp = time.strftime("%Y-%m-%d %H:%M:%S")
    info_file = os.path.join(output_dir, f"{os.path.splitext(output_filename)[0]}_info.txt")
    with open(info_file, "w") as f:
        f.write(f"合并时间: {timestamp}\n")
        f.write(f"输入目录: {input_dir}\n")
        f.write(f"文件模式: {file_pattern}\n")
        f.write(f"合并文件数: {len(powerline_files)}\n")
        f.write(f"合并点数: {len(merged_points):,}\n")
        f.write("\n合并的文件列表:\n")
        for file_path in powerline_files:
            f.write(f"- {file_path}\n")
    
    print(f"合并信息保存到: {info_file}")
    
    return True

def main():
    parser = argparse.ArgumentParser(description="合并多个瓦片中提取出的电力线为一个LAS文件")
    parser.add_argument("--input_dir", "-i", required=True, help="输入目录，包含各瓦片处理结果")
    parser.add_argument("--output_dir", "-o", required=True, help="输出目录路径")
    parser.add_argument("--file_pattern", "-p", default="*/all_power_lines.las", help="电力线文件匹配模式 (默认: '*/all_power_lines.las')")
    parser.add_argument("--output_filename", "-f", default="merged_powerlines.las", help="输出文件名 (默认: 'merged_powerlines.las')")
    parser.add_argument("--no_auto_increment", "-n", action="store_true", help="禁用自动为输出文件添加序号")
    
    args = parser.parse_args()
    
    # 检查输入目录是否存在
    if not os.path.isdir(args.input_dir):
        print(f"错误: 输入目录 '{args.input_dir}' 不存在")
        return 1
    
    # 合并电力线文件
    success = merge_powerline_files(
        args.input_dir,
        args.output_dir,
        args.file_pattern,
        args.output_filename,
        not args.no_auto_increment
    )
    
    return 0 if success else 1

if __name__ == "__main__":
    sys.exit(main())