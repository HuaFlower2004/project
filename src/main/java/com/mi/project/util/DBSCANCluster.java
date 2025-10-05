package com.mi.project.util;

import java.util.*;

/**
 * 自实现的DBSCAN聚类算法
 * 替代Smile库，避免依赖下载问题
 */
public class DBSCANCluster {

    private final double eps;           // 邻域半径
    private final int minPoints;       // 最小点数
    private final double[][] data;     // 数据点
    private final int[] labels;        // 聚类标签
    private final boolean[] visited;   // 访问标记

    private static final int NOISE = -1;
    private static final int UNCLASSIFIED = 0;

    /**
     * DBSCAN聚类构造函数
     *
     * @param data 数据点数组 [n][d]，n个点，每个点d维
     * @param eps 邻域半径
     * @param minPoints 形成聚类的最小点数
     */
    public DBSCANCluster(double[][] data, double eps, int minPoints) {
        this.data = data;
        this.eps = eps;
        this.minPoints = minPoints;
        this.labels = new int[data.length];
        this.visited = new boolean[data.length];

        // 初始化标签
        Arrays.fill(labels, UNCLASSIFIED);
    }

    /**
     * 执行DBSCAN聚类
     *
     * @return 聚类标签数组，-1表示噪声点，其他非负数表示聚类编号
     */
    public int[] cluster() {
        int clusterId = 0;

        for (int i = 0; i < data.length; i++) {
            if (visited[i]) continue;

            visited[i] = true;
            List<Integer> neighbors = getNeighbors(i);

            if (neighbors.size() < minPoints) {
                labels[i] = NOISE;
            } else {
                clusterId++;
                expandCluster(i, neighbors, clusterId);
            }
        }

        return labels;
    }

    /**
     * 扩展聚类
     */
    private void expandCluster(int pointIdx, List<Integer> neighbors, int clusterId) {
        labels[pointIdx] = clusterId;

        Queue<Integer> queue = new LinkedList<>(neighbors);

        while (!queue.isEmpty()) {
            int currentPoint = queue.poll();

            if (!visited[currentPoint]) {
                visited[currentPoint] = true;
                List<Integer> currentNeighbors = getNeighbors(currentPoint);

                if (currentNeighbors.size() >= minPoints) {
                    queue.addAll(currentNeighbors);
                }
            }

            if (labels[currentPoint] == UNCLASSIFIED || labels[currentPoint] == NOISE) {
                labels[currentPoint] = clusterId;
            }
        }
    }

    /**
     * 获取指定点的邻居
     */
    private List<Integer> getNeighbors(int pointIdx) {
        List<Integer> neighbors = new ArrayList<>();

        for (int i = 0; i < data.length; i++) {
            if (distance(data[pointIdx], data[i]) <= eps) {
                neighbors.add(i);
            }
        }

        return neighbors;
    }

    /**
     * 计算两点间的欧几里得距离
     */
    private double distance(double[] point1, double[] point2) {
        double sum = 0.0;
        for (int i = 0; i < point1.length; i++) {
            double diff = point1[i] - point2[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    /**
     * 获取聚类数量（不包括噪声）
     */
    public int getClusterCount() {
        return Arrays.stream(labels).max().orElse(0);
    }

    /**
     * 获取噪声点数量
     */
    public int getNoiseCount() {
        return (int) Arrays.stream(labels).filter(label -> label == NOISE).count();
    }

    /**
     * 静态工厂方法，方便使用
     */
    public static ClusterResult fit(double[][] data, double eps, int minPoints) {
        DBSCANCluster dbscan = new DBSCANCluster(data, eps, minPoints);
        int[] labels = dbscan.cluster();
        return new ClusterResult(labels, dbscan.getClusterCount(), dbscan.getNoiseCount());
    }

    /**
     * 聚类结果封装类
     */
    public static class ClusterResult {
        public final int[] labels;
        public final int clusterCount;
        public final int noiseCount;

        public ClusterResult(int[] labels, int clusterCount, int noiseCount) {
            this.labels = labels;
            this.clusterCount = clusterCount;
            this.noiseCount = noiseCount;
        }

        /**
         * 获取指定聚类的所有点的索引
         */
        public List<Integer> getClusterPoints(int clusterId) {
            List<Integer> points = new ArrayList<>();
            for (int i = 0; i < labels.length; i++) {
                if (labels[i] == clusterId) {
                    points.add(i);
                }
            }
            return points;
        }

        /**
         * 获取噪声点的索引
         */
        public List<Integer> getNoisePoints() {
            return getClusterPoints(NOISE);
        }

        /**
         * 获取所有聚类的统计信息
         */
        public Map<Integer, Integer> getClusterStatistics() {
            Map<Integer, Integer> stats = new HashMap<>();
            for (int label : labels) {
                stats.merge(label, 1, Integer::sum);
            }
            return stats;
        }
    }
}