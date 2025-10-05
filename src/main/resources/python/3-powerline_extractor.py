import numpy as np
import open3d as o3d
import os
import laspy
from sklearn.linear_model import LinearRegression
from sklearn.preprocessing import PolynomialFeatures
import matplotlib.pyplot as plt
from tqdm import tqdm
import time
from scipy.spatial import cKDTree
from sklearn.cluster import DBSCAN
from sklearn.decomposition import PCA
from multiprocessing import Pool, cpu_count
import sys  # 添加以支持命令行参数
import gc


#电力线提取脚本


# 设置中文字体支持
plt.rcParams['font.sans-serif'] = ['SimHei']  # 用于显示中文标签
plt.rcParams['axes.unicode_minus'] = False   # 正常显示负号

def power_line_extraction(input_las, output_directory, use_regions=True, region_size=200.0, overlap=20.0, n_processes=None, min_z_threshold=20.0):
    """
    基于模型残差聚类的电力线精细提取方法
    参数:
        input_las: 输入LAS文件路径
        output_directory: 输出目录路径
        use_regions: 是否使用区域划分处理
        region_size: 区域大小（米）
        overlap: 区域重叠大小（米）
        n_processes: 并行处理的进程数，None表示使用CPU核心数
    """
    # 记录开始时间
    start_time = time.time()
    
    # 1. 读取点云数据
    print("正在读取点云数据...")
    las_data = laspy.read(input_las)
    points = np.vstack((las_data.x, las_data.y, las_data.z)).T
    print(f"点云大小: {len(points):,} 点")
    
    # 在这里添加绝对高程过滤
    print(f"正在过滤绝对高程低于 {min_z_threshold}m 的点...")
    # 先记录原始点数
    original_point_count = len(points)
    print(f"原始点云大小: {original_point_count:,} 点")
    
    # 过滤低于阈值的点
    z_mask = points[:, 2] >= min_z_threshold
    points = points[z_mask]
    
    # 记录过滤后点数
    filtered_point_count = len(points)
    print(f"过滤后点云大小: {filtered_point_count:,} 点")
    print(f"过滤掉的点数: {original_point_count - filtered_point_count:,} 点 ({(original_point_count - filtered_point_count) / original_point_count * 100:.2f}%)")
    
    print(f"点云大小: {len(points):,} 点")
    # 确定是否使用区域划分处理
    if use_regions:
        print("\n使用区域划分并行处理...")
        power_lines, noise_points = parallel_region_processing(points, las_data, region_size, overlap, n_processes)
    else:
        # 使用原始的单区域处理
        power_lines, noise_points = process_single_region(points, las_data)
    
    # 保存结果
    print("正在保存结果...")
    os.makedirs(output_directory, exist_ok=True)
    save_results(power_lines, noise_points, output_directory, las_data)
    
    # 计算总耗时
    total_time = time.time() - start_time
    print(f"处理完成! 总耗时: {total_time:.2f}秒 ({total_time/60:.2f}分钟)")
    return power_lines, noise_points

# 以下是所有原始函数，保持不变...
def process_single_region(points, las_data):
    """
    处理单个区域（传统方法）
    """
    # 2. 点云预处理 - 统计分析
    print("正在进行点云统计分析...")
    stats = analyze_point_cloud(points)
    
    # 3. 高程阈值分割提取远离地面点
    print("正在使用高程阈值分割提取远离地面点...")
    height_threshold = find_optimal_height_threshold(points)
    print(f"最优高程阈值: {height_threshold:.2f}m")
    high_points = extract_high_points(points, height_threshold)
    print(f"远离地面点数量: {len(high_points):,}")
    
    # 4. 基于维度特征和方向特征粗提取电力线点
    print("正在基于维度特征和方向特征粗提取电力线点...")
    powerline_candidates = extract_powerline_candidates(high_points, stats['avg_distance'])
    print(f"电力线候选点数量: {len(powerline_candidates):,}")
    
    # 5. 基于模型残差聚类精细提取单根电力线
    print("正在基于模型残差聚类精细提取单根电力线...")
    power_lines, noise_points, residuals, models = extract_powerlines_by_residual_clustering(powerline_candidates)
    print(f"检测到 {len(power_lines)} 根电力线")
    
    return power_lines, noise_points

def parallel_region_processing(points, las_data, region_size=200.0, overlap=20.0, n_processes=None):
    """
    使用区域划分和并行处理提取电力线
    """
    # 确定使用的进程数
    
    n_processes = 2  # 使用CPU核心数减1，至少1个进程
    
    print(f"将使用 {n_processes} 个进程进行并行处理")
    
    # 区域划分
    regions = divide_point_cloud(points, region_size, overlap)
    print(f"点云已划分为 {len(regions)} 个区域")
    
    # 并行处理各区域
    with Pool(n_processes) as pool:
        results = list(tqdm(pool.imap(process_region, regions), 
                           total=len(regions), 
                           desc="区域处理进度"))
    
    # 合并结果
    power_lines, noise_points = merge_results(results)
    print(f"合并后共检测到 {len(power_lines)} 根电力线")
    
    return power_lines, noise_points

def divide_point_cloud(points, region_size=200.0, overlap=20.0):
    """
    将点云划分为重叠的区域
    """
    # 获取点云的水平范围
    x_min, y_min = np.min(points[:, 0]), np.min(points[:, 1])
    x_max, y_max = np.max(points[:, 0]), np.max(points[:, 1])
    
    # 计算区域数量
    effective_size = region_size - overlap
    x_regions = max(1, int(np.ceil((x_max - x_min) / effective_size)))
    y_regions = max(1, int(np.ceil((y_max - y_min) / effective_size)))
    
    print(f"空间范围: X[{x_min:.2f}, {x_max:.2f}], Y[{y_min:.2f}, {y_max:.2f}]")
    print(f"划分为 {x_regions}x{y_regions} = {x_regions*y_regions} 个区域")
    
    regions = []
    for i in range(x_regions):
        # 计算区域x范围（添加重叠）
        x_start = x_min + i * effective_size
        x_end = min(x_max, x_start + region_size)
        # 确保最后一个区域包含边界
        if i == x_regions - 1:
            x_end = x_max
            
        for j in range(y_regions):
            # 计算区域y范围（添加重叠）
            y_start = y_min + j * effective_size
            y_end = min(y_max, y_start + region_size)
            # 确保最后一个区域包含边界
            if j == y_regions - 1:
                y_end = y_max
            
            # 找出在当前区域内的点
            mask = ((points[:, 0] >= x_start) & (points[:, 0] <= x_end) & 
                    (points[:, 1] >= y_start) & (points[:, 1] <= y_end))
            region_points = points[mask]
            
            # 只处理有足够点的区域
            if len(region_points) > 100:
                regions.append((region_points, (x_start, x_end, y_start, y_end), (i, j)))
    
    return regions

def process_region(region_data):
    """
    处理单个区域的点云数据
    """
    region_points, region_bounds, region_coords = region_data
    i, j = region_coords
    
    print(f"处理区域 ({i},{j}): 点数={len(region_points)}")
    
    # 计算该区域的统计特性
    stats = analyze_point_cloud(region_points, silent=True)
    
    # 为该区域计算最优高程阈值
    height_threshold = find_optimal_height_threshold(region_points)
    
    # 提取高空点
    high_points = extract_high_points(region_points, height_threshold)
    
    # 如果高空点太少，可能该区域没有电力线
    if len(high_points) < 50:
        return [], [], region_bounds
    
    # 提取电力线候选点
    powerline_candidates = extract_powerline_candidates(high_points, stats['avg_distance'])
    
    # 如果候选点太少，可能该区域没有电力线
    if len(powerline_candidates) < 10:
        return [], [], region_bounds
    
    # 基于残差聚类提取电力线
    power_lines, noise_points, _, _ = extract_powerlines_by_residual_clustering(powerline_candidates)
    
    return power_lines, noise_points, region_bounds

def merge_results(results):
    """
    合并来自不同区域的结果，处理重叠部分
    """
    all_power_lines = []
    all_noise_points = []
    
    # 收集所有区域的结果
    for power_lines, noise_points, region_bounds in results:
        all_power_lines.extend(power_lines)
        if len(noise_points) > 0:
            all_noise_points.append(noise_points)
    
    # 处理重叠区域中的电力线
    # 这里可以实现更复杂的融合逻辑，如电力线匹配和合并
    # 目前简单起见，我们只做基本去重
    
    # 合并噪声点
    if all_noise_points:
        merged_noise = np.vstack(all_noise_points)
    else:
        merged_noise = np.array([]).reshape(0, 3)
    
    return all_power_lines, merged_noise

def analyze_point_cloud(points, silent=False):
    """
    分析点云特征以辅助参数选择
    """
    # 计算点云空间分布
    if not silent:
        print("正在分析点云空间分布...")
    with tqdm(total=1, disable=silent) as pbar:
        x_range = np.max(points[:, 0]) - np.min(points[:, 0])
        y_range = np.max(points[:, 1]) - np.min(points[:, 1])
        z_range = np.max(points[:, 2]) - np.min(points[:, 2])
        
        # 计算点云密度
        volume = x_range * y_range * z_range
        density = len(points) / volume if volume > 0 else 0
        pbar.update(1)
    
    # 计算点间平均距离（使用随机抽样加速计算）
    if not silent:
        print("正在计算点间平均距离...")
    with tqdm(total=1, disable=silent) as pbar:
        sample_size = min(10000, len(points))
        indices = np.random.choice(len(points), sample_size, replace=False)
        sampled_points = points[indices]
        
        tree = cKDTree(sampled_points)
        distances, _ = tree.query(sampled_points, k=2)  # 最近邻距离（第一个是自身）
        avg_distance = np.mean(distances[:, 1])
        median_distance = np.median(distances[:, 1])
        pbar.update(1)
    
    if not silent:
        print(f"点云空间范围: X[{x_range:.2f}m], Y[{y_range:.2f}m], Z[{z_range:.2f}m]")
        print(f"点云密度: {density:.6f} 点/立方米")
        print(f"点平均间距: {avg_distance:.2f}m")
        print(f"点中位间距: {median_distance:.2f}m")
        
        # 根据分析结果给出建议
        print("\n基于点云特性的建议参数:")
        print(f"建议区块大小: {max(avg_distance * 3, 2.0):.2f}m")
        print(f"建议残差聚类eps参数: {min(2.5, avg_distance * 0.8):.2f}")
        print(f"建议最小样本数: {max(5, int(density * 10))}")
    
    return {
        "density": density,
        "avg_distance": avg_distance,
        "median_distance": median_distance,
        "x_range": x_range,
        "y_range": y_range,
        "z_range": z_range
    }

def find_optimal_height_threshold(points):
    """
    找到最优的高程阈值，使用归一化高程处理
    """
    # 创建网格并归一化高程
    with tqdm(total=1, desc="高程归一化", disable=len(points) < 10000) as pbar:
        # 网格大小
        grid_size = 10.0  # 10米网格
        
        # 获取点云的水平范围
        x_min, y_min = np.min(points[:, 0]), np.min(points[:, 1])
        x_max, y_max = np.max(points[:, 0]), np.max(points[:, 1])
        
        # 计算网格数量
        x_grids = int(np.ceil((x_max - x_min) / grid_size))
        y_grids = int(np.ceil((y_max - y_min) / grid_size))
        
        # 初始化归一化高程数组
        normalized_heights = np.zeros(len(points))
        
        # 对每个网格进行处理
        for i in range(x_grids):
            for j in range(y_grids):
                # 计算当前网格的边界
                x_start, x_end = x_min + i * grid_size, x_min + (i + 1) * grid_size
                y_start, y_end = y_min + j * grid_size, y_min + (j + 1) * grid_size
                
                # 找出在当前网格内的点
                mask = ((points[:, 0] >= x_start) & (points[:, 0] < x_end) & 
                        (points[:, 1] >= y_start) & (points[:, 1] < y_end))
                grid_points = points[mask]
                
                if len(grid_points) > 0:
                    # 找到网格内的最低点高程
                    min_z = np.min(grid_points[:, 2])
                    
                    # 归一化该网格内的点高程
                    normalized_heights[mask] = grid_points[:, 2] - min_z
        pbar.update(1)
    
    # 统计归一化高程分布并找到突变点作为阈值
    with tqdm(total=1, desc="寻找最优阈值", disable=len(points) < 10000) as pbar:
        # 使用直方图统计高程分布
        hist, bin_edges = np.histogram(normalized_heights, bins=100, range=(0, 100))
        
        # 找到直方图的突变点 - 尝试使用更宽松的条件
        threshold_idx = 0
        for i in range(1, len(hist)):
            if hist[i] < hist[i-1] * 0.3 and hist[i-1] > len(points) * 0.005:  # 更宽松的条件
                threshold_idx = i
                break
        
        # 如果没有明显突变点，使用经验值
        if threshold_idx == 0:
            # 根据点云高度范围动态调整默认阈值
            z_range = np.max(points[:, 2]) - np.min(points[:, 2])
            if z_range > 100:  # 山地
                threshold = 20.0
            else:  # 平原
                threshold = 15.0
        else:
            threshold = bin_edges[threshold_idx]
            
        # 确保阈值在合理范围内
        threshold = max(8.0, min(threshold, 30.0))
        pbar.update(1)
    
    return threshold

def extract_high_points(points, height_threshold):
    """
    根据高程阈值提取远离地面的点
    """
    with tqdm(total=1, desc="提取高空点", disable=len(points) < 10000) as pbar:
        # 创建网格并归一化高程
        grid_size = 10.0  # 10米网格
        
        # 获取点云的水平范围
        x_min, y_min = np.min(points[:, 0]), np.min(points[:, 1])
        x_max, y_max = np.max(points[:, 0]), np.max(points[:, 1])
        
        # 计算网格数量
        x_grids = int(np.ceil((x_max - x_min) / grid_size))
        y_grids = int(np.ceil((y_max - y_min) / grid_size))
        
        # 初始化归一化高程数组和高点索引
        normalized_heights = np.zeros(len(points))
        high_point_indices = []
        
        # 对每个网格进行处理
        for i in range(x_grids):
            for j in range(y_grids):
                # 计算当前网格的边界
                x_start, x_end = x_min + i * grid_size, x_min + (i + 1) * grid_size
                y_start, y_end = y_min + j * grid_size, y_min + (j + 1) * grid_size
                
                # 找出在当前网格内的点
                mask = ((points[:, 0] >= x_start) & (points[:, 0] < x_end) & 
                        (points[:, 1] >= y_start) & (points[:, 1] < y_end))
                grid_indices = np.where(mask)[0]
                grid_points = points[mask]
                
                if len(grid_points) > 0:
                    # 找到网格内的最低点高程
                    min_z = np.min(grid_points[:, 2])
                    
                    # 归一化该网格内的点高程
                    grid_heights = grid_points[:, 2] - min_z
                    normalized_heights[grid_indices] = grid_heights
                    
                    # 提取高于阈值的点
                    high_indices = grid_indices[grid_heights > height_threshold]
                    high_point_indices.extend(high_indices)
        pbar.update(1)
    
    # 返回高空点
    return points[high_point_indices]

def compute_dimension_features(points):
    """
    计算点云的维度特征 (线性度、平面度、球度)
    """
    # 至少需要3个点才能计算维度特征
    if len(points) < 3:
        return 0, 0, 1  # 默认为球状
    
    # 计算点云的协方差矩阵
    centroid = np.mean(points, axis=0)
    centered_points = points - centroid
    cov_matrix = np.cov(centered_points, rowvar=False)
    
    # 计算特征值和特征向量
    try:
        eigenvalues, eigenvectors = np.linalg.eigh(cov_matrix)
        
        # 按降序排列特征值及对应特征向量
        idx = eigenvalues.argsort()[::-1]
        eigenvalues = eigenvalues[idx]
        eigenvectors = eigenvectors[:, idx]
        
        # 防止除零错误
        if eigenvalues[0] <= 0:
            return 0, 0, 1
        
        # 计算维度特征
        linearity = (eigenvalues[0] - eigenvalues[1]) / eigenvalues[0]
        planarity = (eigenvalues[1] - eigenvalues[2]) / eigenvalues[0]
        sphericity = eigenvalues[2] / eigenvalues[0]
        
        # 返回主方向及维度特征
        return linearity, planarity, sphericity, eigenvectors[:, 0]
    except:
        return 0, 0, 1, np.array([0, 0, 1])  # 出错时返回默认值

def extract_powerline_candidates(high_points, avg_distance):
    """
    基于维度特征和方向特征提取电力线候选点
    """
    # 自适应参数
    min_radius = max(1.0, avg_distance * 0.5)
    max_radius = max(5.0, avg_distance * 3.0)
    
    # 创建KD树加速邻域搜索
    tree = cKDTree(high_points)
    
    # 提取电力线候选点
    candidates = []
    
    with tqdm(total=len(high_points), desc="维度特征提取", disable=len(high_points) < 1000) as pbar:
        for i, point in enumerate(high_points):
            # 使用固定半径搜索邻域点
            radius = max(min_radius, avg_distance * 2)
            indices = tree.query_ball_point(point, radius)
            
            if len(indices) < 3:
                pbar.update(1)
                continue
            
            # 计算维度特征和主方向
            linearity, planarity, sphericity, main_direction = compute_dimension_features(high_points[indices])
            
            # 电力线特征: 高线性度、低平面度和球度
            if linearity > 0.80:  # 线性度阈值
                # 计算主方向与水平面的夹角
                horizontal_component = np.sqrt(main_direction[0]**2 + main_direction[1]**2)
                vertical_component = abs(main_direction[2])
                
                # 电力线通常与水平面夹角较小
                if horizontal_component > 0.70:  # 水平分量阈值
                    candidates.append(point)
            
            pbar.update(1)
    
    return np.array(candidates) if candidates else np.array([]).reshape(0, 3)

def extract_powerlines_by_residual_clustering(powerline_candidates):
    """
    基于模型残差聚类提取单根电力线
    """
    if len(powerline_candidates) < 10:
        empty_result = [], powerline_candidates, np.array([]), {}
        return empty_result
    
    # 1. 整体拟合 - 水平投影拟合直线
    with tqdm(total=1, desc="水平投影拟合", disable=len(powerline_candidates) < 1000) as pbar:
        xy_points = powerline_candidates[:, :2]
        
        # 计算主方向
        pca = PCA(n_components=2)
        pca.fit(xy_points)
        direction = pca.components_[0]
        
        # 使用主方向进行直线拟合
        # 假设直线方程为: y = kx + b 或 x = ky + b (取决于主方向)
        if abs(direction[0]) > abs(direction[1]):  # x方向主导
            X = xy_points[:, 0].reshape(-1, 1)
            y = xy_points[:, 1]
            model_h = LinearRegression()
            model_h.fit(X, y)
            k, b = model_h.coef_[0], model_h.intercept_
            is_x_dominant = True
        else:  # y方向主导
            X = xy_points[:, 1].reshape(-1, 1)
            y = xy_points[:, 0]
            model_h = LinearRegression()
            model_h.fit(X, y)
            k, b = model_h.coef_[0], model_h.intercept_
            is_x_dominant = False
        pbar.update(1)
    
    # 2. 建立铅垂坐标系，计算每个点在铅垂坐标系中的位置
    with tqdm(total=1, desc="建立铅垂坐标系", disable=len(powerline_candidates) < 1000) as pbar:
        s_coords = []
        
        if is_x_dominant:
            for point in powerline_candidates:
                # 计算点到直线的投影
                s = (point[0] + k*(point[1] - b)) / (1 + k**2)
                s_coords.append(s)
        else:
            for point in powerline_candidates:
                # 计算点到直线的投影
                s = (point[1] + k*(point[0] - b)) / (1 + k**2)
                s_coords.append(s)
        
        s_coords = np.array(s_coords)
        pbar.update(1)
    
    # 3. 在铅垂坐标系中拟合抛物线 z = As² + Bs + C
    with tqdm(total=1, desc="拟合抛物线模型", disable=len(powerline_candidates) < 1000) as pbar:
        poly = PolynomialFeatures(degree=2)
        s_poly = poly.fit_transform(s_coords.reshape(-1, 1))
        z_model = LinearRegression()
        z_model.fit(s_poly, powerline_candidates[:, 2])
        A, B, C = z_model.coef_[2], z_model.coef_[1], z_model.intercept_
        pbar.update(1)
    
    # 4. 计算模型残差
    with tqdm(total=1, desc="计算模型残差", disable=len(powerline_candidates) < 1000) as pbar:
        residuals = []
        
        for i, point in enumerate(powerline_candidates):
            # 水平残差 - 点到直线的距离
            if is_x_dominant:
                H_i = (k*point[0] - point[1] + b) / np.sqrt(1 + k**2)
            else:
                H_i = (point[0] - k*point[1] - b) / np.sqrt(1 + k**2)
            
            # 垂直残差 - 点到抛物线的距离
            V_i = A*s_coords[i]**2 + B*s_coords[i] + C - point[2]
            
            residuals.append([H_i, V_i])
        
        residuals = np.array(residuals)
        pbar.update(1)
        # gc.collect()
    
    # 5. 对残差进行密度聚类
    with tqdm(total=1, desc="残差密度聚类", disable=len(powerline_candidates) < 1000) as pbar:
        # 确定DBSCAN参数
        eps = 10.0  # 根据相邻电力线间距调整
        min_samples = 15  # 最小样本数
        
        db = DBSCAN(eps=eps, min_samples=min_samples).fit(residuals)
        labels = db.labels_
        pbar.update(1)
    
    # 6. 提取单根电力线
    n_clusters = len(set(labels)) - (1 if -1 in labels else 0)
    print(n_clusters)
    power_lines = []
    models = {}
    
    with tqdm(total=n_clusters, desc="提取单根电力线", disable=n_clusters < 10) as pbar:
        for i in range(n_clusters):
            cluster_indices = np.where(labels == i)[0]
            
            # 只保留有足够点的电力线
            if len(cluster_indices) >= min_samples:
                power_lines.append(powerline_candidates[cluster_indices])
                
                # 为每根电力线记录模型参数
                models[i] = {
                    'is_x_dominant': is_x_dominant,
                    'k': k,
                    'b': b,
                    'A': A,
                    'B': B,
                    'C': C,
                    's_coords': s_coords[cluster_indices]
                }
            
            pbar.update(1)
    
    # 噪声点
    noise_indices = np.where(labels == -1)[0]
    noise_points = powerline_candidates[noise_indices]
    
    return power_lines, noise_points, residuals, models

def filter_low_density_points(power_lines, radius=2.0, min_neighbors=5):
    """
    密度过滤，移除低密度区域点
    """
    filtered_lines = []
    for line_points in power_lines:
        if len(line_points) < 10:
            filtered_lines.append(line_points)
            continue
            
        tree = cKDTree(line_points)
        point_counts = []
        
        for point in line_points:
            neighbors = tree.query_ball_point(point, radius)
            point_counts.append(len(neighbors))
            
        density_mask = np.array(point_counts) >= min_neighbors
        
        # 只保留满足密度要求的点，且数量至少为5
        if np.sum(density_mask) >= 5:
            filtered_lines.append(line_points[density_mask])
        elif len(line_points) >= 5:
            filtered_lines.append(line_points)
    
    return filtered_lines

def save_results(power_lines, noise_points, output_directory, original_las, residuals=None, models=None):
    """
    保存电力线提取结果
    """
    # 对电力线进行密度过滤，移除孤立点
   # 替换原来的filter_low_density_points调用
    filtered_power_lines = statistical_outlier_removal(
        filter_low_density_points(power_lines), 
        k=20, 
        std_ratio=2.0
    )
        
    # 保存每根电力线
    for i, line_points in enumerate(filtered_power_lines):
        with tqdm(total=1, desc=f"保存电力线 {i+1}") as pbar:
            line_las = laspy.create(file_version=original_las.header.version)
            line_las.x = line_points[:, 0]
            line_las.y = line_points[:, 1]
            line_las.z = line_points[:, 2]
            
            # 确保分类值不超过31
            classification_value = (i % 31) + 1  # 使用模运算确保在1-31范围内
            line_las.classification = np.ones(len(line_points), dtype=np.uint8) * classification_value
            
            line_las.write(os.path.join(output_directory, f"power_line_{i+1}.las"))
            pbar.update(1)
    
    # 保存合并的电力线
    with tqdm(total=1, desc="保存合并电力线") as pbar:
        all_points = []
        all_classifications = []
        
        for i, line_points in enumerate(filtered_power_lines):
            all_points.append(line_points)
            
            # 确保分类值不超过31
            classification_value = (i % 31) + 1  # 使用模运算确保在1-31范围内
            all_classifications.append(np.ones(len(line_points), dtype=np.uint8) * classification_value)
        
        if all_points:
            all_points = np.vstack(all_points)
            all_classifications = np.hstack(all_classifications)
            
            merged_las = laspy.create(file_version=original_las.header.version)
            merged_las.x = all_points[:, 0]
            merged_las.y = all_points[:, 1]
            merged_las.z = all_points[:, 2]
            merged_las.classification = all_classifications
            merged_las.write(os.path.join(output_directory, "all_power_lines.las"))
        pbar.update(1)
    
    # 保存噪声点
    with tqdm(total=1, desc="保存噪声点") as pbar:
        if len(noise_points) > 0:
            noise_las = laspy.create(file_version=original_las.header.version)
            noise_las.x = noise_points[:, 0]
            noise_las.y = noise_points[:, 1]
            noise_las.z = noise_points[:, 2]
            noise_las.classification = np.zeros(len(noise_points), dtype=np.uint8)
            noise_las.write(os.path.join(output_directory, "noise_points.las"))
        pbar.update(1)
    
    # 保存残差可视化
    if residuals is not None:
        with tqdm(total=1, desc="保存残差可视化") as pbar:
            plt.figure(figsize=(10, 8))
            plt.scatter(residuals[:, 0], residuals[:, 1], c='b', s=1, alpha=0.5)
            plt.xlabel('水平残差 (m)')
            plt.ylabel('垂直残差 (m)')
            plt.title('模型残差分布')
            plt.grid(True)
            plt.savefig(os.path.join(output_directory, "residuals.png"), dpi=300)
            plt.close()
            pbar.update(1)

def statistical_outlier_removal(power_lines, k=20, std_ratio=2.0):
    """
    基于统计分析的杂点滤波，适用于已经提取的电力线
    """
    filtered_lines = []
    
    for line_points in power_lines:
        if len(line_points) < k + 1:
            filtered_lines.append(line_points)
            continue
            
        # 创建KD树
        tree = cKDTree(line_points)
        
        # 计算每个点到其k个最近邻的平均距离
        distances, _ = tree.query(line_points, k=k+1)
        avg_distances = np.mean(distances[:, 1:], axis=1)
        
        # 计算平均距离的均值和标准差
        mean_distance = np.mean(avg_distances)
        std_distance = np.std(avg_distances)
        
        # 过滤离群点
        threshold = mean_distance + std_ratio * std_distance
        mask = avg_distances < threshold
        
        if np.sum(mask) >= 10:  # 至少保留10个点
            filtered_lines.append(line_points[mask])
        else:
            filtered_lines.append(line_points)
    
    return filtered_lines

# 主函数
if __name__ == "__main__":
    print("==========================================")
    print("  基于模型残差聚类的电力线精细提取工具")
    print("  参考麻卫峰等人的方法 - 区域并行版")
    print("==========================================")
    
    # 最小修改：从命令行获取输入和输出路径
    if len(sys.argv) >= 3:
        input_las = sys.argv[1]
        output_directory = sys.argv[2]
        
        # 可选的高程阈值参数
        min_z_threshold = 20.0
        if len(sys.argv) >= 4:
            try:
                min_z_threshold = float(sys.argv[3])
            except ValueError:
                print(f"警告: 无效的高程阈值 '{sys.argv[3]}'，使用默认值 20.0")
        
        print(f"输入文件: {input_las}")
        print(f"输出目录: {output_directory}")
        print(f"高程阈值: {min_z_threshold}m")
        
        # 如果不存在则创建输出目录
        os.makedirs(output_directory, exist_ok=True)
        
        # 提取电力线 - 使用区域划分并行处理
        power_lines, noise_points = power_line_extraction(
            input_las, 
            output_directory, 
            use_regions=True,      # 使用区域划分
            region_size=500.0,     # 区域大小(米)
            overlap=20.0,          # 区域重叠(米)
            n_processes=None,      # 自动使用CPU核心数
            min_z_threshold=min_z_threshold  # 绝对高程阈值
        )
    else:
        # 如果没有提供命令行参数，使用默认值
        input_las = r"input.las"  # 替换为您的输入文件路径
        output_directory = r"output_directory"  # 替换为您想要的输出目录
        
        # 如果不存在则创建输出目录
        os.makedirs(output_directory, exist_ok=True)
        
        # 提取电力线 - 使用区域划分并行处理
        power_lines, noise_points = power_line_extraction(
            input_las, 
            output_directory, 
            use_regions=True,      # 使用区域划分
            region_size=500.0,     # 区域大小(米)
            overlap=20.0,          # 区域重叠(米)
            n_processes=None,      # 自动使用CPU核心数
            min_z_threshold=20.0   # 绝对高程阈值，低于20米的点会被过滤
        )
    
    print("==========================================")
    print(f"  处理完成！结果已保存到: {output_directory}")
    print("==========================================")