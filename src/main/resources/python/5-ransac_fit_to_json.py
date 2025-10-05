import numpy as np
import open3d as o3d
import laspy
import os
import json
import time
import argparse
from sklearn.linear_model import RANSACRegressor, LinearRegression
from sklearn.preprocessing import PolynomialFeatures
from sklearn.decomposition import PCA
from sklearn.cluster import DBSCAN
from sklearn.neighbors import NearestNeighbors
from scipy.spatial.distance import cdist
import matplotlib.pyplot as plt
from tqdm import tqdm

# 拟合电力线，返回归一化json，html报告，txt报告
# 调用归一化参数json文件：python 4-ransac_fit_to_json.py -i D:\programs\脚本慢慢来\powerlines\tile_456000_3120000\all_power_lines.las -o D:\programs\脚本慢慢来\powerlines_line_json -nf D:\programs\脚本慢慢来\tile分块\tile_threejs_params.json
# 可视化结果产看：python 4-ransac_fit_to_json.py --input D:\programs\las数据提取\las数据\麻卫风\all_power_lines.las --output_dir D:\programs\脚本慢慢来\powerlines_line_json --visualize

# 设置中文字体支持
plt.rcParams['font.sans-serif'] = ['SimHei']
plt.rcParams['axes.unicode_minus'] = False


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


def load_normalization_params(normalization_file):
    """
    加载归一化参数JSON文件
    
    参数:
        normalization_file: 归一化参数JSON文件路径
        
    返回:
        dict: 归一化参数，如果加载失败返回None
    """
    try:
        with open(normalization_file, 'r', encoding='utf-8') as f:
            params = json.load(f)
        
        # 验证必要的参数是否存在
        required_keys = ['center', 'normalization']
        if not all(key in params for key in required_keys):
            print(f"警告: 归一化参数文件缺少必要的键: {required_keys}")
            return None
        
        # 验证归一化参数结构
        norm_params = params['normalization']
        if 'scale' not in norm_params or 'translate' not in norm_params:
            print("警告: 归一化参数结构不正确")
            return None
        
        translate = norm_params['translate']
        if not all(key in translate for key in ['x', 'y', 'z']):
            print("警告: 平移参数缺少x,y,z坐标")
            return None
        
        print(f"成功加载归一化参数:")
        print(f"  数据中心: ({params['center']['x']:.2f}, {params['center']['y']:.2f}, {params['center']['z']:.2f})")
        print(f"  缩放因子: {norm_params['scale']:.6f}")
        print(f"  平移量: ({translate['x']:.2f}, {translate['y']:.2f}, {translate['z']:.2f})")
        
        return params
        
    except FileNotFoundError:
        print(f"错误: 找不到归一化参数文件: {normalization_file}")
        return None
    except json.JSONDecodeError as e:
        print(f"错误: 归一化参数文件JSON格式错误: {e}")
        return None
    except Exception as e:
        print(f"错误: 加载归一化参数时发生异常: {e}")
        return None


def apply_normalization(points, normalization_params):
    """
    对点云应用归一化变换
    
    参数:
        points: 原始点云 numpy数组 (N, 3)
        normalization_params: 归一化参数字典
        
    返回:
        numpy数组: 归一化后的点云
    """
    if normalization_params is None:
        print("警告: 归一化参数为空，返回原始坐标")
        return points
    
    try:
        # 获取归一化参数
        scale = normalization_params['normalization']['scale']
        translate = normalization_params['normalization']['translate']
        
        # 创建平移向量
        translate_vector = np.array([translate['x'], translate['y'], translate['z']])
        
        # 应用归一化: (原始坐标 + 平移) * 缩放
        normalized_points = (points + translate_vector) * scale
        
        return normalized_points
        
    except Exception as e:
        print(f"警告: 应用归一化时发生错误: {e}，返回原始坐标")
        return points


class RANSACPowerLineFitting:
    """
    基于RANSAC的电力线重建 - 简单有效的方法，支持归一化输出
    """
    
    def __init__(self, 
                 segment_length=50.0,        # 分段长度
                 overlap_ratio=0.2,          # 重叠比例
                 ransac_threshold=1.5,       # RANSAC阈值(放宽)
                 min_samples_ratio=0.5,      # 最小样本比例(降低)
                 polynomial_degree=1,        # 线性拟合(简化)
                 normalization_file=None):   # 归一化参数文件
        """
        参数:
        - segment_length: 每段的长度(米)
        - overlap_ratio: 相邻段的重叠比例
        - ransac_threshold: RANSAC内点阈值(米) - 放宽到1.5米
        - min_samples_ratio: RANSAC最小样本比例 - 降低到50%
        - polynomial_degree: 1=线性拟合(简化)
        - normalization_file: 归一化参数JSON文件路径
        """
        self.segment_length = segment_length
        self.overlap_ratio = overlap_ratio
        self.ransac_threshold = ransac_threshold
        self.min_samples_ratio = min_samples_ratio
        self.polynomial_degree = polynomial_degree
        
        # 加载归一化参数
        self.normalization_params = None
        if normalization_file:
            self.normalization_params = load_normalization_params(normalization_file)
            if self.normalization_params:
                print("将使用归一化坐标输出结果")
            else:
                print("将使用原始坐标输出结果")
        else:
            print("未指定归一化参数文件，将使用原始坐标")
    
    def load_point_cloud(self, file_path):
        """加载点云文件"""
        print(f"加载点云文件: {file_path}")
        
        ext = os.path.splitext(file_path)[1].lower()
        
        if ext == '.las' or ext == '.laz':
            las = laspy.read(file_path)
            points = np.vstack((las.x, las.y, las.z)).transpose()
            
            # 读取分类信息，用于区分不同的电力线
            if hasattr(las, 'classification'):
                classifications = las.classification
            else:
                classifications = np.ones(len(points), dtype=np.uint8)
            
            print(f"读取LAS文件: {len(points)}个点")
            return points, classifications
        else:
            raise ValueError(f"只支持LAS/LAZ文件格式")
    
    def extract_powerline_segments(self, points, classifications):
        """
        高效提取电力线段 - 避免内存爆炸
        考虑分类信息，将不同分类的点分为不同的电力线
        """
        print("开始提取电力线段...")
        
        # 使用分类信息分组
        unique_classes = np.unique(classifications)
        print(f"发现 {len(unique_classes)} 个不同的电力线分类")
        
        segments = []
        
        for cls in unique_classes:
            # 跳过0分类（通常是噪声点）
            if cls == 0:
                continue
                
            # 提取当前分类的点
            class_points = points[classifications == cls]
            
            if len(class_points) < 10:
                print(f"  分类 {cls} 点数太少，跳过 ({len(class_points)}点)")
                continue
                
            print(f"  处理分类 {cls}: {len(class_points)}点")
            
            # 对于点数很少的情况，直接作为一个段处理
            if len(class_points) < 20:
                print(f"    点数少于20，直接作为一个段处理")
                segments.append({
                    'points': class_points,
                    'classification': cls,
                    'segment_id': len(segments) + 1
                })
                continue
            
            # 使用DBSCAN进一步细分每个分类中的线段
            # 计算自适应eps值
            try:
                k = min(10, len(class_points) - 1)  # 自适应k值，确保不超过点数
                nbrs = NearestNeighbors(n_neighbors=k+1).fit(class_points)  # +1因为包括点自身
                distances, indices = nbrs.kneighbors(class_points)
                
                # 使用第k个近邻的平均距离
                avg_kth_distance = np.mean(distances[:, -1])
                eps = avg_kth_distance * 2.5  # 调整系数
                
                # 确保eps不为0且不太小
                if eps < 0.01:
                    eps = 0.01
                    print(f"    聚类半径太小，设置为最小值: {eps:.2f}米")
                else:
                    print(f"    自适应聚类半径: {eps:.2f}米 (基于{k}近邻)")
                
                # 如果eps太大，强制限制
                if eps > 20.0:
                    eps = 15.0
                    print(f"    限制聚类半径为: {eps:.2f}米")
                
                # DBSCAN聚类
                clustering = DBSCAN(eps=eps, min_samples=min(5, len(class_points) // 2)).fit(class_points)
                labels = clustering.labels_
                
                # 提取聚类
                n_clusters = len(set(labels)) - (1 if -1 in labels else 0)
                n_noise = list(labels).count(-1)
                
                print(f"    聚类结果: {n_clusters}个聚类, {n_noise}个噪声点")
                
                if n_clusters > 0:
                    for i in range(n_clusters):
                        cluster_points = class_points[labels == i]
                        if len(cluster_points) >= min(8, len(class_points) // 2):  # 确保子聚类有足够的点
                            segments.append({
                                'points': cluster_points,
                                'classification': cls,
                                'segment_id': len(segments) + 1
                            })
                else:
                    # 如果聚类失败，将所有点作为一个段
                    print(f"    聚类失败，将所有点作为一个段")
                    segments.append({
                        'points': class_points,
                        'classification': cls,
                        'segment_id': len(segments) + 1
                    })
            except Exception as e:
                print(f"    聚类出错: {e}，将所有点作为一个段")
                segments.append({
                    'points': class_points,
                    'classification': cls,
                    'segment_id': len(segments) + 1
                })
        
        print(f"提取了 {len(segments)} 个有效电力线段")
        
        # 如果没有找到有效段，尝试忽略分类信息直接聚类
        if len(segments) == 0:
            print("  未找到有效段，尝试忽略分类信息...")
            
            try:
                # 对于点数很少的情况，直接作为一个段处理
                if len(points) < 20:
                    print(f"    总点数少于20，直接作为一个段处理")
                    segments.append({
                        'points': points,
                        'classification': 1,  # 默认分类
                        'segment_id': 1
                    })
                    return segments
                
                # 计算自适应eps值
                k = min(10, len(points) - 1)
                nbrs = NearestNeighbors(n_neighbors=k+1).fit(points)
                distances, indices = nbrs.kneighbors(points)
                avg_kth_distance = np.mean(distances[:, -1])
                eps = max(0.01, avg_kth_distance * 2.5)  # 确保eps不为0
                
                # DBSCAN聚类
                clustering = DBSCAN(eps=eps, min_samples=min(5, len(points) // 2)).fit(points)
                labels = clustering.labels_
                n_clusters = len(set(labels)) - (1 if -1 in labels else 0)
                
                if n_clusters > 0:
                    for i in range(n_clusters):
                        cluster_points = points[labels == i]
                        if len(cluster_points) >= min(8, len(points) // 2):
                            segments.append({
                                'points': cluster_points,
                                'classification': 1,  # 默认分类
                                'segment_id': len(segments) + 1
                            })
                else:
                    # 如果聚类失败，将所有点作为一个段
                    segments.append({
                        'points': points,
                        'classification': 1,
                        'segment_id': 1
                    })
            except Exception as e:
                print(f"  忽略分类聚类也失败: {e}，将所有点作为一个段")
                segments.append({
                    'points': points,
                    'classification': 1,
                    'segment_id': 1
                })
            
            print(f"  忽略分类后: {len(segments)} 个电力线段")
        
        return segments
    
    def fit_powerline_segment(self, segment_data):
        """
        拟合单个电力线段
        """
        segment_points = segment_data['points']
        classification = segment_data['classification']
        segment_id = segment_data['segment_id']
        
        if len(segment_points) < 5:
            print(f"  段 {segment_id} 点数太少({len(segment_points)}点)，无法拟合")
            return None, {
                'total_points': len(segment_points),
                'fitted_segments': 0,
                'total_length': 0,
                'classification': classification,
                'segment_id': segment_id
            }
        
        try:
            # PCA确定主方向
            pca = PCA(n_components=3)
            pca.fit(segment_points)
            
            # 投影到主方向
            main_direction = pca.components_[0]
            projected_distances = segment_points.dot(main_direction)
            
            # 按主方向排序
            sorted_indices = np.argsort(projected_distances)
            sorted_points = segment_points[sorted_indices]
            
            # 计算沿主方向的距离
            distances_1d = projected_distances[sorted_indices]
            distances_1d = distances_1d - distances_1d[0]  # 从0开始
            
            # 获取总长度
            total_length = distances_1d[-1] if len(distances_1d) > 0 else 0
            
            # 分段拟合
            segments_fitted = []
            
            # 对于很短的线段或点数很少的情况，不进行分段
            if total_length < self.segment_length or len(segment_points) < 20:
                # 短线段，直接拟合
                curve = self.ransac_fit_segment(sorted_points, distances_1d)
                if curve is not None:
                    curve['classification'] = classification
                    curve['segment_id'] = segment_id
                    curve['subsegment_id'] = 1
                    segments_fitted.append(curve)
            else:
                # 长线段，分段拟合
                n_segments = int(np.ceil(total_length / self.segment_length))
                step = total_length / n_segments
                
                for i in range(n_segments):
                    start_dist = i * step * (1 - self.overlap_ratio)
                    end_dist = (i + 1) * step
                    
                    # 选择当前段的点
                    mask = (distances_1d >= start_dist) & (distances_1d <= end_dist)
                    if np.sum(mask) < 5:
                        continue
                    
                    segment_subset = sorted_points[mask]
                    segment_distances = distances_1d[mask]
                    
                    curve = self.ransac_fit_segment(segment_subset, segment_distances)
                    if curve is not None:
                        curve['classification'] = classification
                        curve['segment_id'] = segment_id
                        curve['subsegment_id'] = i + 1
                        segments_fitted.append(curve)
            
            return segments_fitted, {
                'total_points': len(segment_points),
                'fitted_segments': len(segments_fitted),
                'total_length': total_length,
                'classification': classification,
                'segment_id': segment_id
            }
        
        except Exception as e:
            print(f"  段 {segment_id} 拟合出错: {e}")
            return None, {
                'total_points': len(segment_points),
                'fitted_segments': 0,
                'total_length': 0,
                'classification': classification,
                'segment_id': segment_id
            }
    
    def ransac_fit_segment(self, points, distances_1d):
        """
        使用RANSAC拟合单个段 - 简化版避免复杂依赖
        """
        if len(points) < 5:
            return None
        
        try:
            # 准备数据
            X = distances_1d.reshape(-1, 1)
            
            # 简化：使用线性拟合代替多项式拟合
            # 分别拟合x, y, z坐标
            fitted_coords = []
            inlier_masks = []
            
            for coord_idx in range(3):
                y = points[:, coord_idx]
                
                # 确保最小样本数不超过总样本数
                min_samples = max(3, min(len(points) - 1, int(len(points) * self.min_samples_ratio)))
                
                # RANSAC线性拟合
                ransac = RANSACRegressor(
                    estimator=LinearRegression(),
                    min_samples=min_samples,
                    residual_threshold=self.ransac_threshold,
                    max_trials=50,  # 减少尝试次数
                    random_state=42
                )
                
                ransac.fit(X, y)
                fitted_coords.append(ransac)
                inlier_masks.append(ransac.inlier_mask_)
            
            # 生成拟合曲线
            num_points = min(50, max(2, len(points)))  # 确保至少有2个点，最多50个点
            curve_distances = np.linspace(distances_1d.min(), distances_1d.max(), num_points)
            curve_points = np.zeros((num_points, 3))
            
            X_curve = curve_distances.reshape(-1, 1)
            
            for coord_idx in range(3):
                curve_points[:, coord_idx] = fitted_coords[coord_idx].predict(X_curve)
            
            # 计算整体内点掩码（三个坐标的交集）
            overall_inlier_mask = inlier_masks[0] & inlier_masks[1] & inlier_masks[2]
            
            # 计算拟合参数（直线方程的系数）
            coefs_x = fitted_coords[0].estimator_.coef_[0], fitted_coords[0].estimator_.intercept_
            coefs_y = fitted_coords[1].estimator_.coef_[0], fitted_coords[1].estimator_.intercept_
            coefs_z = fitted_coords[2].estimator_.coef_[0], fitted_coords[2].estimator_.intercept_
            
            return {
                'points': curve_points,
                'inlier_mask': overall_inlier_mask,
                'inlier_ratio': np.sum(overall_inlier_mask) / len(points),
                'rmse': np.mean([
                    np.sqrt(np.mean((points[:, i] - fitted_coords[i].predict(X))**2)) 
                    for i in range(3)
                ]),
                'coefs': {
                    'x': {'slope': float(coefs_x[0]), 'intercept': float(coefs_x[1])},
                    'y': {'slope': float(coefs_y[0]), 'intercept': float(coefs_y[1])},
                    'z': {'slope': float(coefs_z[0]), 'intercept': float(coefs_z[1])}
                },
                'start_point': curve_points[0].tolist(),
                'end_point': curve_points[-1].tolist(),
                'original_points_count': len(points)  # 添加原始点数
            }
            
        except Exception as e:
            print(f"RANSAC拟合失败: {e}")
            return None
    
    def process_powerline_cloud(self, file_path, output_dir=None, save_json=True, visualize=False, save_xyz=False, save_html=True, auto_increment=True):
        """
        处理电力线点云的主函数
        
        参数:
            file_path: 输入LAS文件路径
            output_dir: 输出目录
            save_json: 是否保存JSON文件
            visualize: 是否显示可视化结果
            save_xyz: 是否保存XYZ文件
            save_html: 是否保存HTML报告
            auto_increment: 是否自动为输出文件添加序号
        """
        print("="*80)
        print("开始基于RANSAC的电力线重建")
        print("="*80)
        
        start_process_time = time.time()
        
        # 1. 加载点云
        points, classifications = self.load_point_cloud(file_path)
        
        # 2. 提取电力线段
        powerline_segments = self.extract_powerline_segments(points, classifications)
        
        # 3. 拟合每个段
        all_fitted_curves = []
        all_fit_infos = []
        
        start_fit_time = time.time()
        
        for i, segment in enumerate(powerline_segments):
            print(f"\n处理电力线段 {i+1}/{len(powerline_segments)} (点数: {len(segment['points'])})")
            
            fitted_curves, fit_info = self.fit_powerline_segment(segment)
            
            if fitted_curves:
                all_fitted_curves.extend(fitted_curves)
                all_fit_infos.append(fit_info)
                print(f"  成功拟合 {len(fitted_curves)} 个子段")
            else:
                all_fit_infos.append(fit_info)
                print(f"  拟合失败")
        
        fit_time = time.time() - start_fit_time
        total_time = time.time() - start_process_time
        
        # 4. 结果统计
        print(f"\n{'='*80}")
        print("RANSAC重建完成 - 结果统计")
        print(f"{'='*80}")
        print(f"原始点数: {len(points):,}")
        print(f"电力线段数: {len(powerline_segments)}")
        print(f"拟合曲线数: {len(all_fitted_curves)}")
        print(f"拟合时间: {fit_time:.2f}秒")
        print(f"总处理时间: {total_time:.2f}秒")
        
        success_rate = 0
        avg_inlier_ratio = 0
        avg_rmse = 0
        
        if powerline_segments:
            success_rate = len([info for info in all_fit_infos if info['fitted_segments'] > 0]) / len(powerline_segments) * 100
            print(f"成功率: {success_rate:.1f}%")
        
        if all_fitted_curves:
            avg_inlier_ratio = np.mean([curve['inlier_ratio'] for curve in all_fitted_curves])
            avg_rmse = np.mean([curve['rmse'] for curve in all_fitted_curves])
            print(f"平均内点比例: {avg_inlier_ratio:.2f}")
            print(f"平均RMSE: {avg_rmse:.3f}米")
        
        if all_fit_infos:
            total_segments = sum(info['fitted_segments'] for info in all_fit_infos)
            print(f"总拟合段数: {total_segments}")
        
        # 显示每个段的统计
        for i, info in enumerate(all_fit_infos):
            print(f"  段{info['segment_id']}: {info['total_points']}点 → {info['fitted_segments']}段 "
                  f"(长度{info['total_length']:.1f}m, 分类{info['classification']})")
        
        # 5. 可视化
        if visualize and all_fitted_curves:
            try:
                self.visualize_results(points, powerline_segments, all_fitted_curves)
            except Exception as e:
                print(f"可视化失败: {e}")
        
        # 6. 导出结果
        if output_dir:
            os.makedirs(output_dir, exist_ok=True)
            
            # 构建基本文件名（不含扩展名）
            base_filename = "powerline_curves"
            
            # 如果启用自动添加序号，获取可用的文件名
            if auto_increment:
                json_file = get_available_filename(os.path.join(output_dir, f"{base_filename}.json"))
                base_filename = os.path.splitext(os.path.basename(json_file))[0]
            else:
                json_file = os.path.join(output_dir, f"{base_filename}.json")
            
            # 导出JSON和其他结果
            if save_json:
                # 保存JSON文件
                self.export_json(all_fitted_curves, all_fit_infos, output_dir, f"{base_filename}.json")
                
                # 保存报告文件
                report_file = os.path.join(output_dir, f"{base_filename}_report.txt")
                self.export_report(powerline_segments, all_fitted_curves, all_fit_infos, report_file)
                
                # 如果需要，保存XYZ文件
                if save_xyz:
                    # 创建XYZ目录
                    xyz_dir = os.path.join(output_dir, f"{base_filename}_curves")
                    os.makedirs(xyz_dir, exist_ok=True)
                    
                    # 导出XYZ文件
                    self.export_xyz_files(all_fitted_curves, xyz_dir)
            
            # 导出HTML报告
            if save_html:
                html_file = os.path.join(output_dir, f"{base_filename}_report.html")
                self.export_html_report(
                    all_fitted_curves, 
                    all_fit_infos, 
                    html_file, 
                    total_points=len(points),
                    total_segments=len(powerline_segments),
                    success_rate=success_rate,
                    avg_inlier_ratio=avg_inlier_ratio,
                    avg_rmse=avg_rmse,
                    total_time=total_time,
                    fit_time=fit_time
                )
        
        return points, powerline_segments, all_fitted_curves, all_fit_infos
    
    def visualize_results(self, points, segments, fitted_curves):
        """可视化结果"""
        print("开始可视化...")
        
        # 原始点云（灰色）
        pcd_original = o3d.geometry.PointCloud()
        pcd_original.points = o3d.utility.Vector3dVector(points)
        pcd_original.paint_uniform_color([0.7, 0.7, 0.7])
        
        # 电力线段（不同颜色）
        pcd_segments = []
        colors = plt.cm.tab10(np.linspace(0, 1, 10))  # 只使用10种颜色
        
        for i, segment in enumerate(segments):
            pcd_segment = o3d.geometry.PointCloud()
            pcd_segment.points = o3d.utility.Vector3dVector(segment['points'])
            pcd_segment.paint_uniform_color(colors[i % 10][:3])
            pcd_segments.append(pcd_segment)
        
        # 拟合曲线（红色粗线）
        line_sets = []
        for i, curve_info in enumerate(fitted_curves):
            curve_points = curve_info['points']
            
            line_set = o3d.geometry.LineSet()
            line_set.points = o3d.utility.Vector3dVector(curve_points)
            lines = [[j, j+1] for j in range(len(curve_points)-1)]
            line_set.lines = o3d.utility.Vector2iVector(lines)
            
            # 根据分类使用不同颜色
            cls = curve_info['classification'] % 10  # 取模确保在颜色范围内
            color = colors[cls][:3]
            colors_lines = [color for _ in range(len(lines))]
            line_set.colors = o3d.utility.Vector3dVector(colors_lines)
            line_sets.append(line_set)
        
        # 显示
        geometries = [pcd_original] + pcd_segments + line_sets
        o3d.visualization.draw_geometries(geometries)
    
    def export_json(self, fitted_curves, fit_infos, output_dir, json_filename="powerline_curves.json"):
        """导出JSON格式的拟合结果，支持归一化坐标"""
        print("导出JSON结果...")
        
        # 创建JSON数据结构
        json_data = {
            "metadata": {
                "generator": "RANSAC Power Line Fitting",
                "version": "2.0",
                "date": time.strftime("%Y-%m-%d %H:%M:%S"),
                "totalSegments": len(fit_infos),
                "totalCurves": len(fitted_curves),
                "coordinateSystem": "normalized" if self.normalization_params else "original"
            },
            "parameters": {
                "segmentLength": self.segment_length,
                "overlapRatio": self.overlap_ratio,
                "ransacThreshold": self.ransac_threshold,
                "minSamplesRatio": self.min_samples_ratio,
                "polynomialDegree": self.polynomial_degree
            },
            "segments": [],
            "curves": []
        }
        
        # 如果有归一化参数，添加到metadata中
        if self.normalization_params:
            json_data["normalization"] = {
                "source": "threejs_params",
                "applied": True,
                "originalBounds": self.normalization_params.get("original_bounds"),
                "center": self.normalization_params.get("center"),
                "scale": self.normalization_params["normalization"]["scale"],
                "translate": self.normalization_params["normalization"]["translate"]
            }
        
        # 添加段信息
        for info in fit_infos:
            segment_info = {
                "segmentId": info["segment_id"],
                "classification": int(info["classification"]),
                "totalPoints": int(info["total_points"]),
                "fittedSegments": int(info["fitted_segments"]),
                "totalLength": float(info["total_length"])
            }
            json_data["segments"].append(segment_info)
        
        # 添加曲线信息
        for curve in fitted_curves:
            # 获取原始曲线点
            original_points = curve["points"]
            
            # 应用归一化（如果有归一化参数）
            if self.normalization_params:
                normalized_points = apply_normalization(original_points, self.normalization_params)
                curve_points_to_export = normalized_points
                start_point_to_export = apply_normalization(np.array([curve["start_point"]]), self.normalization_params)[0].tolist()
                end_point_to_export = apply_normalization(np.array([curve["end_point"]]), self.normalization_params)[0].tolist()
            else:
                curve_points_to_export = original_points
                start_point_to_export = curve["start_point"]
                end_point_to_export = curve["end_point"]
            
            # 简化曲线点以减小JSON大小 - 只保留10个点
            simplified_points = []
            if len(curve_points_to_export) > 0:
                step = max(1, len(curve_points_to_export) // 10)
                for i in range(0, len(curve_points_to_export), step):
                    simplified_points.append(curve_points_to_export[i].tolist())
                
                # 确保至少包含起点和终点
                if len(simplified_points) < 2 and len(curve_points_to_export) >= 2:
                    simplified_points = [curve_points_to_export[0].tolist(), curve_points_to_export[-1].tolist()]
            
            curve_info = {
                "segmentId": curve["segment_id"],
                "subsegmentId": curve["subsegment_id"],
                "classification": int(curve["classification"]),
                "inlierRatio": float(curve["inlier_ratio"]),
                "rmse": float(curve["rmse"]),
                "coefficients": curve["coefs"],
                "startPoint": start_point_to_export,
                "endPoint": end_point_to_export,
                "points": simplified_points,
                "originalPointsCount": curve.get("original_points_count", 0)
            }
            
            # 如果有归一化参数，也保存原始坐标（可选）
            if self.normalization_params:
                # 简化原始坐标点
                original_simplified = []
                if len(original_points) > 0:
                    step = max(1, len(original_points) // 10)
                    for i in range(0, len(original_points), step):
                        original_simplified.append(original_points[i].tolist())
                    
                    if len(original_simplified) < 2 and len(original_points) >= 2:
                        original_simplified = [original_points[0].tolist(), original_points[-1].tolist()]
                
                curve_info["originalCoordinates"] = {
                    "startPoint": curve["start_point"],
                    "endPoint": curve["end_point"],
                    "points": original_simplified
                }
            
            json_data["curves"].append(curve_info)
        
        # 保存JSON文件
        json_file = os.path.join(output_dir, json_filename)
        with open(json_file, 'w', encoding='utf-8') as f:
            json.dump(json_data, f, indent=2)
        
        print(f"JSON结果已保存到: {json_file}")
        if self.normalization_params:
            print("  坐标已归一化，适合直接在Three.js中使用")
        else:
            print("  使用原始坐标系")
    
    def export_report(self, segments, fitted_curves, fit_infos, report_file):
        """生成报告文件"""
        print(f"生成报告文件: {report_file}")
        
        with open(report_file, 'w', encoding='utf-8') as f:
            f.write("基于RANSAC的电力线重建报告\n")
            f.write("=" * 80 + "\n\n")
            f.write(f"生成时间: {time.strftime('%Y-%m-%d %H:%M:%S')}\n")
            f.write(f"电力线段数: {len(segments)}\n")
            f.write(f"拟合曲线数: {len(fitted_curves)}\n")
            f.write(f"坐标系统: {'归一化' if self.normalization_params else '原始'}\n\n")
            
            if self.normalization_params:
                f.write("归一化参数:\n")
                f.write(f"  缩放因子: {self.normalization_params['normalization']['scale']:.6f}\n")
                translate = self.normalization_params['normalization']['translate']
                f.write(f"  平移量: ({translate['x']:.2f}, {translate['y']:.2f}, {translate['z']:.2f})\n\n")
            
            f.write("参数设置:\n")
            f.write(f"  分段长度: {self.segment_length}米\n")
            f.write(f"  重叠比例: {self.overlap_ratio}\n")
            f.write(f"  RANSAC阈值: {self.ransac_threshold}米\n")
            f.write(f"  多项式度数: {self.polynomial_degree}\n\n")
            
            for info in fit_infos:
                f.write(f"电力线段 {info['segment_id']} (分类 {info['classification']}):\n")
                f.write(f"  点数: {info['total_points']}\n")
                f.write(f"  拟合段数: {info['fitted_segments']}\n")
                f.write(f"  总长度: {info['total_length']:.2f}米\n\n")
            
            f.write("=" * 80 + "\n")
            f.write("报告结束\n")
    
    def export_xyz_files(self, fitted_curves, xyz_dir):
        """导出XYZ格式的曲线文件，支持归一化坐标"""
        print(f"导出XYZ曲线文件到: {xyz_dir}")
        
        for i, curve_info in enumerate(fitted_curves):
            original_points = curve_info['points']
            segment_id = curve_info['segment_id']
            subsegment_id = curve_info['subsegment_id']
            cls = curve_info['classification']
            
            # 应用归一化（如果有）
            if self.normalization_params:
                curve_points = apply_normalization(original_points, self.normalization_params)
                coord_suffix = "_normalized"
            else:
                curve_points = original_points
                coord_suffix = "_original"
            
            # 保存为XYZ文件
            curve_file = os.path.join(xyz_dir, f"powerline_seg{segment_id}_sub{subsegment_id}_class{cls}{coord_suffix}.xyz")
            with open(curve_file, 'w') as f:
                f.write(f"# RANSAC拟合的电力线曲线 - 段{segment_id} 子段{subsegment_id} 分类{cls}\n")
                f.write(f"# 坐标系统: {'归一化' if self.normalization_params else '原始'}\n")
                f.write("# X Y Z\n")
                for point in curve_points:
                    f.write(f"{point[0]:.6f} {point[1]:.6f} {point[2]:.6f}\n")
    
    def export_html_report(self, fitted_curves, fit_infos, html_file, total_points=0, total_segments=0, 
                          success_rate=0, avg_inlier_ratio=0, avg_rmse=0, total_time=0, fit_time=0):
        """导出HTML格式的报告，类似于ransac_report.html的样式"""
        print(f"生成HTML报告: {html_file}")
        
        # 创建HTML报告
        html_content = f"""<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>RANSAC电力线重建报告</title>
    <style>
        body {{ font-family: Arial, sans-serif; margin: 20px; line-height: 1.6; }}
        .header {{ background: #2c3e50; color: white; padding: 20px; border-radius: 5px; }}
        .summary {{ background: #ecf0f1; padding: 15px; margin: 20px 0; border-radius: 5px; }}
        .curve-item {{ background: #f8f9fa; margin: 10px 0; padding: 15px; border-left: 4px solid #3498db; }}
        .stats {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; }}
        .stat-card {{ background: white; padding: 15px; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }}
        .good {{ color: #27ae60; }} .warning {{ color: #f39c12; }} .error {{ color: #e74c3c; }}
        table {{ width: 100%; border-collapse: collapse; margin: 20px 0; }}
        th, td {{ padding: 10px; text-align: left; border-bottom: 1px solid #ddd; }}
        th {{ background-color: #f2f2f2; }}
    </style>
</head>
<body>
    <div class="header">
        <h1>🚀 RANSAC电力线重建报告</h1>
        <p>生成时间: {time.strftime('%Y-%m-%d %H:%M:%S')}</p>
    </div>
    
    <div class="summary">
        <h2>📊 处理概要</h2>
        <div class="stats">
            <div class="stat-card">
                <h3>📁 输入数据</h3>
                <p>总点数: {total_points:,}</p>
                <p>电力线段数: {total_segments}</p>
            </div>
            <div class="stat-card">
                <h3>📈 输出结果</h3>
                <p>拟合曲线数: {len(fitted_curves)}</p>
                <p>成功率: {success_rate:.1f}%</p>
            </div>
            <div class="stat-card">
                <h3>⏱️ 处理时间</h3>
                <p>总时间: {total_time:.2f}秒</p>
                <p>拟合时间: {fit_time:.2f}秒</p>
            </div>
            <div class="stat-card">
                <h3>🎯 质量指标</h3>
                <p>平均RMSE: {avg_rmse:.3f}米</p>
                <p>平均内点比: {avg_inlier_ratio:.2f}</p>
            </div>
        </div>
    </div>
    
    <h2>📋 拟合曲线详情</h2>
    <table>
        <thead>
            <tr>
                <th>ID</th>
                <th>点数</th>
                <th>长度(m)</th>
                <th>RMSE(m)</th>
                <th>内点比例</th>
                <th>质量评级</th>
            </tr>
        </thead>
        <tbody>
"""
        
        # 添加每条拟合曲线的信息
        curve_id = 1
        for curve in fitted_curves:
            segment_id = curve['segment_id']
            subsegment_id = curve['subsegment_id']
            
            # 计算曲线长度 - 起点和终点之间的欧氏距离
            start = np.array(curve['start_point'])
            end = np.array(curve['end_point'])
            length = np.linalg.norm(end - start)
            
            # 确定质量等级
            if curve['rmse'] < 0.3 and curve['inlier_ratio'] > 0.7:
                quality = '<span class="good">优秀</span>'
            elif curve['rmse'] < 0.5 and curve['inlier_ratio'] > 0.5:
                quality = '<span class="warning">良好</span>'
            else:
                quality = '<span class="error">一般</span>'
            
            # 格式化ID
            curve_id_str = f"powerline_{segment_id:03d}_{subsegment_id}"
            
            # 添加行
            html_content += f"""
            <tr>
                <td>{curve_id_str}</td>
                <td>{curve.get('original_points_count', 0)}</td>
                <td>{length:.2f}</td>
                <td>{curve['rmse']:.3f}</td>
                <td>{curve['inlier_ratio']:.2f}</td>
                <td>{quality}</td>
            </tr>
"""
            curve_id += 1
        
        # 完成表格并添加参数信息
        html_content += f"""
        </tbody>
    </table>
    
    <h2>🔧 算法参数</h2>
    <div class="summary">
        <p><strong>分段长度:</strong> {self.segment_length:.1f}米</p>
        <p><strong>重叠比例:</strong> {self.overlap_ratio * 100:.0f}%</p>
        <p><strong>RANSAC阈值:</strong> {self.ransac_threshold:.1f}米</p>
        <p><strong>最小样本比例:</strong> {self.min_samples_ratio * 100:.0f}%</p>
        <p><strong>拟合类型:</strong> {'线性拟合' if self.polynomial_degree == 1 else f'多项式拟合(度数: {self.polynomial_degree})'}</p>
    </div>
    
    <footer style="margin-top: 40px; text-align: center; color: #7f8c8d;">
        <p>报告由RANSAC电力线重建系统自动生成</p>
    </footer>
</body>
</html>
"""
        
        # 写入HTML文件
        with open(html_file, 'w', encoding='utf-8') as f:
            f.write(html_content)
        
        print(f"HTML报告已保存到: {html_file}")


def main():
    # 解析命令行参数
    parser = argparse.ArgumentParser(description="基于RANSAC的电力线曲线拟合，输出为JSON格式，支持归一化坐标")
    parser.add_argument("--input", "-i", required=True, help="输入LAS文件路径")
    parser.add_argument("--output_dir", "-o", required=True, help="输出目录路径")
    parser.add_argument("--normalization_file", "-nf", help="归一化参数JSON文件路径(可选)")
    parser.add_argument("--segment_length", "-sl", type=float, default=60.0, help="分段长度(米)")
    parser.add_argument("--overlap_ratio", "-or", type=float, default=0.1, help="重叠比例")
    parser.add_argument("--ransac_threshold", "-rt", type=float, default=1.2, help="RANSAC阈值(米)")
    parser.add_argument("--min_samples_ratio", "-msr", type=float, default=0.5, help="最小样本比例")
    parser.add_argument("--visualize", "-v", action="store_true", help="显示可视化结果")
    parser.add_argument("--save_xyz", "-xyz", action="store_true", help="保存XYZ文件")
    parser.add_argument("--no_html", "-nh", action="store_true", help="不生成HTML报告")
    parser.add_argument("--no_auto_increment", "-nai", action="store_true", help="禁用自动文件编号")
    
    args = parser.parse_args()
    
    print("初始化RANSAC电力线重建器...")
    
    # 使用命令行参数创建处理器
    processor = RANSACPowerLineFitting(
        segment_length=args.segment_length,
        overlap_ratio=args.overlap_ratio,
        ransac_threshold=args.ransac_threshold,
        min_samples_ratio=args.min_samples_ratio,
        polynomial_degree=1,  # 固定使用线性拟合
        normalization_file=args.normalization_file
    )
    
    try:
        print("="*80)
        print("开始RANSAC电力线重建")
        print("优势:")
        print("1. 无需复杂的参数调优")
        print("2. 自动处理噪声点")
        print("3. 适应各种电力线形状")
        print("4. 分段处理，避免过拟合")
        print("5. 结果稳定可靠")
        print("6. 支持归一化坐标输出（Three.js友好）")
        print("7. 生成可视化HTML报告")
        print("="*80)
        
        # 处理
        results = processor.process_powerline_cloud(
            args.input, 
            args.output_dir,
            save_json=True,
            visualize=args.visualize,
            save_xyz=args.save_xyz,
            save_html=not args.no_html,
            auto_increment=not args.no_auto_increment
        )
        
        print("\nRANSAC电力线重建完成！")
        
        if args.normalization_file:
            print("提示: 输出的JSON文件包含归一化坐标，可直接用于Three.js渲染")
        
        if not args.no_html:
            print("提示: HTML报告已生成，可在浏览器中查看电力线拟合结果")
        
    except Exception as e:
        print(f"处理错误: {e}")
        import traceback
        traceback.print_exc()
        return 1
    
    return 0


if __name__ == "__main__":
    start_time = time.time()
    exit_code = main()
    end_time = time.time()
    print(f"\n总运行时间: {end_time - start_time:.2f} 秒")
    exit(exit_code)