import numpy as np
import laspy
import json
import os
import time
import argparse
import datetime
# 提取点云数据中点云坐标并归一化输出至json文件
# python point_to_json.py --input 输入文件.las --output 输出目录
# python point_to_json.py --input D:\点云数据\电力线.las --output D:\点云数据\JSON导出 --normalization_file D:\点云数据\电力线_threejs_params.json


def load_normalization_params(normalization_file):
    """加载归一化参数JSON文件"""
    try:
        with open(normalization_file, 'r', encoding='utf-8') as f:
            params = json.load(f)
        
        # 验证必要参数
        if 'normalization' not in params or 'scale' not in params['normalization'] or 'translate' not in params['normalization']:
            print("错误: 归一化参数文件格式不正确")
            return None
        
        print(f"加载归一化参数: scale={params['normalization']['scale']:.6f}")
        return params
        
    except Exception as e:
        print(f"错误: 无法加载归一化参数文件: {e}")
        return None


def apply_normalization(points, normalization_params):
    """对点云应用归一化变换"""
    if normalization_params is None:
        return points
    
    scale = normalization_params['normalization']['scale']
    translate = normalization_params['normalization']['translate']
    translate_vector = np.array([translate['x'], translate['y'], translate['z']])
    
    # 归一化: (原始坐标 + 平移) * 缩放
    return (points + translate_vector) * scale


def generate_unique_filename(input_file, output_dir):
    """生成唯一的输出文件名，避免重复"""
    # 获取输入文件的基本名称（不含路径和扩展名）
    base_name = os.path.splitext(os.path.basename(input_file))[0]
    
    # 添加时间戳确保唯一性
    timestamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
    
    # 生成唯一文件名
    filename = f"{base_name}_{timestamp}.json"
    
    # 返回完整路径
    return os.path.join(output_dir, filename)


def main():
    parser = argparse.ArgumentParser(description="提取LAS文件XYZ坐标并输出JSON")
    parser.add_argument("--input", "-i", required=True, help="输入LAS文件路径")
    parser.add_argument("--output", "-o", required=True, help="输出JSON文件目录路径")
    parser.add_argument("--normalization_file", "-nf", help="归一化参数JSON文件路径")
    parser.add_argument("--max_points", "-mp", type=int, help="最大点数限制")
    
    args = parser.parse_args()
    
    print("="*50)
    print("LAS坐标提取器")
    print("="*50)
    
    # 检查输入文件
    if not os.path.exists(args.input):
        print(f"错误: 输入文件不存在: {args.input}")
        return 1
    
    # 检查输出目录
    if not os.path.exists(args.output):
        print(f"输出目录不存在，正在创建: {args.output}")
        try:
            os.makedirs(args.output)
        except Exception as e:
            print(f"错误: 无法创建输出目录: {e}")
            return 1
    elif not os.path.isdir(args.output):
        print(f"错误: 指定的输出路径不是目录: {args.output}")
        return 1
    
    # 加载归一化参数
    normalization_params = None
    if args.normalization_file:
        normalization_params = load_normalization_params(args.normalization_file)
    
    try:
        # 加载LAS文件
        print(f"加载LAS文件: {args.input}")
        las = laspy.read(args.input)
        coordinates = np.vstack((las.x, las.y, las.z)).transpose()
        print(f"总点数: {len(coordinates):,}")
        
        # 采样
        if args.max_points and len(coordinates) > args.max_points:
            step = len(coordinates) // args.max_points
            coordinates = coordinates[::step][:args.max_points]
            print(f"采样后点数: {len(coordinates):,}")
        
        # 归一化
        if normalization_params:
            coordinates = apply_normalization(coordinates, normalization_params)
            print("已应用归一化")
        
        # 生成唯一的输出文件名
        output_file = generate_unique_filename(args.input, args.output)
        
        # 输出JSON
        json_data = {
            "pointCount": len(coordinates),
            "coordinateSystem": "normalized" if normalization_params else "original",
            "coordinates": coordinates.round(6).tolist()
        }
        
        print(f"保存JSON文件: {output_file}")
        with open(output_file, 'w') as f:
            json.dump(json_data, f, separators=(',', ':'))
        
        file_size = os.path.getsize(output_file) / (1024*1024)
        print(f"完成! 文件大小: {file_size:.1f} MB")
        
        return 0
        
    except Exception as e:
        print(f"错误: {e}")
        return 1


if __name__ == "__main__":
    start_time = time.time()
    exit_code = main()
    end_time = time.time()
    print(f"运行时间: {end_time - start_time:.2f} 秒")