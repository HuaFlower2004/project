import os
import subprocess
import argparse
import time
import sys

# 调用powerline_extractor.py 进行电力线提取
# python process_tiles.py --input 输入文件.las --output 输出目录 --script 电力线提取脚本.py
# python process_tiles.py --input D:\点云数据\电力线点云.las --output D:\点云数据\提取结果 --script D:\脚本\powerline_extractor.py

def extract_powerline(input_las, output_dir, powerline_script, min_z_threshold=20.0, python_exe=None):
    """
    从单个LAS文件提取电力线
    
    参数:
        input_las: 输入LAS文件路径
        output_dir: 输出目录路径
        powerline_script: 电力线提取脚本路径
        min_z_threshold: 高程阈值，低于此值的点会被过滤
        python_exe: Python解释器路径，默认为当前Python
    """
    # 检查输入文件是否存在
    if not os.path.isfile(input_las):
        print(f"错误: 输入文件 '{input_las}' 不存在")
        return False
    
    # 确保输出目录存在
    os.makedirs(output_dir, exist_ok=True)
    
    # 如果未指定Python路径，尝试查找Python 3.10
    if python_exe is None:
        # 尝试几个常见的Python 3.10路径
        potential_paths = [
            r"C:\Users\25235\AppData\Local\Programs\Python\Python310\python.exe",
            r"C:\Python310\python.exe",
            r"C:\Program Files\Python310\python.exe",
            r"C:\Program Files (x86)\Python310\python.exe",
            sys.executable  # 当前Python解释器
        ]
        
        for path in potential_paths:
            if os.path.exists(path):
                python_exe = path
                break
    
    if python_exe is None:
        python_exe = "python"  # 默认使用系统PATH中的python
    
    print(f"将使用Python解释器: {python_exe}")
    
    # 记录开始时间
    start_time = time.time()
    
    # 构建命令 - 注意这里改变了参数传递方式
    cmd = [
        python_exe,
        powerline_script,
        input_las,
        output_dir,  # 直接传递输出目录，不使用-output_dir参数
        str(min_z_threshold)
    ]
    
    print(f"\n执行命令: {' '.join(cmd)}")
    
    # 执行命令
    process = subprocess.Popen(
        cmd, 
        stdout=subprocess.PIPE, 
        stderr=subprocess.STDOUT,
        text=True,
        bufsize=1
    )
    
    # 实时显示输出
    print("\n[电力线提取开始]\n")
    for line in iter(process.stdout.readline, ''):
        print(line, end='')
        sys.stdout.flush()
    print("\n[电力线提取结束]\n")
    
    # 等待进程结束
    return_code = process.wait()
    
    # 计算总耗时
    total_time = time.time() - start_time
    
    if return_code == 0:
        print(f"\n处理成功!")
        print(f"总耗时: {total_time:.2f}秒 ({total_time/60:.2f}分钟)")
        print(f"结果保存在: {os.path.abspath(output_dir)}")
        
        # 列出生成的文件
        output_files = [f for f in os.listdir(output_dir) if f.endswith('.las')]
        if output_files:
            print(f"\n生成的文件:")
            for file in output_files:
                print(f"  - {file}")
        return True
    else:
        print(f"\n处理失败，返回码: {return_code}")
        print(f"总耗时: {total_time:.2f}秒 ({total_time/60:.2f}分钟)")
        return False

def main():
    parser = argparse.ArgumentParser(description="从LAS文件提取电力线")
    parser.add_argument("--input", "-i", required=True, help="输入LAS文件路径")
    parser.add_argument("--output", "-o", required=True, help="输出目录路径")
    parser.add_argument("--script", "-s", required=True, help="电力线提取脚本路径")
    parser.add_argument("--threshold", "-t", type=float, default=20.0, help="高程阈值，默认为20.0米")
    parser.add_argument("--python", "-p", help="Python解释器路径，默认自动查找Python 3.10")
    
    args = parser.parse_args()
    
    # 提取电力线
    success = extract_powerline(
        args.input,
        args.output,
        args.script,
        args.threshold,
        args.python
    )
    
    return 0 if success else 1

if __name__ == "__main__":
    sys.exit(main())