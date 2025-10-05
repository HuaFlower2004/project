package com.mi.project.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * 优化版 LAS文件点云分析工具类
 * 支持大数据量处理：分页、采样、压缩等策略
 * 既可以保存本地文件，也可以返回JSON字符串传递给前端
 *
 * @author MI Project Team
 * @since 2.0.0
 */
@Component
public class LASToJsonUtil {

    // LAS文件头部结构
    static class LASHeader {
        String fileSignature;
        int pointDataRecordFormat;
        int pointDataRecordLength;
        long numberOfPointRecords;
        double xScaleFactor, yScaleFactor, zScaleFactor;
        double xOffset, yOffset, zOffset;
        double maxX, minX, maxY, minY, maxZ, minZ;
        long offsetToPointData;

        @Override
        public String toString() {
            return String.format("LAS Header: %d points, format %d, scale(%.6f, %.6f, %.6f)",
                    numberOfPointRecords, pointDataRecordFormat, xScaleFactor, yScaleFactor, zScaleFactor);
        }
    }

    // 点云数据结构
    static class LASPoint {
        double x, y, z;
        int intensity;
        int classification;

        public LASPoint(double x, double y, double z, int intensity, int classification) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.intensity = intensity;
            this.classification = classification;
        }
    }

    // 边界框结构
    static class BoundingBox {
        double minX, minY, minZ;
        double maxX, maxY, maxZ;
        double centerX, centerY, centerZ;

        public BoundingBox(List<LASPoint> points) {
            if (points.isEmpty()) return;

            minX = maxX = points.get(0).x;
            minY = maxY = points.get(0).y;
            minZ = maxZ = points.get(0).z;

            for (LASPoint point : points) {
                minX = Math.min(minX, point.x);
                maxX = Math.max(maxX, point.x);
                minY = Math.min(minY, point.y);
                maxY = Math.max(maxY, point.y);
                minZ = Math.min(minZ, point.z);
                maxZ = Math.max(maxZ, point.z);
            }

            centerX = (minX + maxX) / 2;
            centerY = (minY + maxY) / 2;
            centerZ = (minZ + maxZ) / 2;
        }

        @Override
        public String toString() {
            return String.format("Bounds: X[%.2f, %.2f], Y[%.2f, %.2f], Z[%.2f, %.2f]",
                    minX, maxX, minY, maxY, minZ, maxZ);
        }
    }

    /**
     * 采样策略枚举
     */
    public enum SamplingStrategy {
        UNIFORM,     // 均匀采样
        RANDOM,      // 随机采样
        GRID_BASED,  // 基于网格的采样
        INTENSITY    // 基于强度的采样
    }

    /**
     * 点云数据处理选项
     */
    public static class ProcessingOptions {
        private int maxPointsForJSON = 50000;        // JSON最大点数限制
        private boolean enableSampling = true;       // 是否启用采样
        private boolean enablePagination = true;     // 是否启用分页
        private int pageSize = 10000;               // 分页大小
        private boolean enableCompression = false;   // 是否启用压缩
        private SamplingStrategy samplingStrategy = SamplingStrategy.UNIFORM; // 采样策略

        // Getters and Setters
        public int getMaxPointsForJSON() { return maxPointsForJSON; }
        public void setMaxPointsForJSON(int maxPointsForJSON) { this.maxPointsForJSON = maxPointsForJSON; }

        public boolean isEnableSampling() { return enableSampling; }
        public void setEnableSampling(boolean enableSampling) { this.enableSampling = enableSampling; }

        public boolean isEnablePagination() { return enablePagination; }
        public void setEnablePagination(boolean enablePagination) { this.enablePagination = enablePagination; }

        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }

        public boolean isEnableCompression() { return enableCompression; }
        public void setEnableCompression(boolean enableCompression) { this.enableCompression = enableCompression; }

        public SamplingStrategy getSamplingStrategy() { return samplingStrategy; }
        public void setSamplingStrategy(SamplingStrategy samplingStrategy) { this.samplingStrategy = samplingStrategy; }

        public static ProcessingOptions getDefault() {
            return new ProcessingOptions();
        }

        public static ProcessingOptions forLargeDataset() {
            ProcessingOptions options = new ProcessingOptions();
            options.maxPointsForJSON = 20000;
            options.enableSampling = true;
            options.samplingStrategy = SamplingStrategy.GRID_BASED;
            return options;
        }

        public static ProcessingOptions forWebDisplay() {
            ProcessingOptions options = new ProcessingOptions();
            options.maxPointsForJSON = 10000;
            options.enableSampling = true;
            options.enablePagination = true;
            options.pageSize = 5000;
            return options;
        }
    }

    /**
     * 增强版分析结果
     */
    public static class LASAnalysisResult {
        private boolean success;
        private String outputPath;
        private String errorMessage;
        private long processingTime;
        private int totalPoints;
        private int filteredPoints;
        private int jsonPoints;  // JSON中实际包含的点数
        private Map<Integer, Integer> classificationStats;
        private String jsonData;
        private boolean isSampled;   // 是否进行了采样
        private boolean isPaginated; // 是否分页
        private int totalPages;      // 总页数
        private List<String> pageFiles; // 分页文件列表

        public LASAnalysisResult(boolean success, String outputPath, String errorMessage,
                                 long processingTime, int totalPoints, int filteredPoints, int jsonPoints,
                                 Map<Integer, Integer> classificationStats, String jsonData,
                                 boolean isSampled, boolean isPaginated, int totalPages, List<String> pageFiles) {
            this.success = success;
            this.outputPath = outputPath;
            this.errorMessage = errorMessage;
            this.processingTime = processingTime;
            this.totalPoints = totalPoints;
            this.filteredPoints = filteredPoints;
            this.jsonPoints = jsonPoints;
            this.classificationStats = classificationStats;
            this.jsonData = jsonData;
            this.isSampled = isSampled;
            this.isPaginated = isPaginated;
            this.totalPages = totalPages;
            this.pageFiles = pageFiles;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getOutputPath() { return outputPath; }
        public String getErrorMessage() { return errorMessage; }
        public long getProcessingTime() { return processingTime; }
        public int getTotalPoints() { return totalPoints; }
        public int getFilteredPoints() { return filteredPoints; }
        public int getJsonPoints() { return jsonPoints; }
        public Map<Integer, Integer> getClassificationStats() { return classificationStats; }
        public String getJsonData() { return jsonData; }
        public boolean isSampled() { return isSampled; }
        public boolean isPaginated() { return isPaginated; }
        public int getTotalPages() { return totalPages; }
        public List<String> getPageFiles() { return pageFiles; }

        @Override
        public String toString() {
            if (success) {
                String samplingInfo = isSampled ? String.format(" (采样后%d点)", jsonPoints) : "";
                String pageInfo = isPaginated ? String.format(" (分%d页)", totalPages) : "";
                return String.format("LAS分析成功: 总点数%d, 过滤%d, JSON%d%s%s, 耗时%dms",
                        totalPoints, filteredPoints, jsonPoints, samplingInfo, pageInfo, processingTime);
            } else {
                return String.format("LAS分析失败: %s", errorMessage);
            }
        }
    }

    // ================================ 公共方法 ================================

    /**
     * 主要分析方法 - 支持大数据优化
     */
    public LASAnalysisResult lasAnalysisOptimized(String filePath, String outputPath,
                                                  int[] classificationValues, boolean normalizeCoords,
                                                  ProcessingOptions options) {
        long startTime = System.currentTimeMillis();

        try {
            // 参数验证
            if (filePath == null || filePath.trim().isEmpty()) {
                return createErrorResult("LAS文件路径不能为空", startTime);
            }

            if (!Files.exists(Paths.get(filePath))) {
                return createErrorResult("输入文件不存在: " + filePath, startTime);
            }

            if (options == null) {
                options = ProcessingOptions.getDefault();
            }

            // 设置默认分类值
            if (classificationValues == null || classificationValues.length == 0) {
                classificationValues = new int[]{16, 0};
            }

            System.out.println("开始LAS分析 (优化模式): " + filePath);
            System.out.println("目标分类值: " + Arrays.toString(classificationValues));
            System.out.println("处理选项: 最大JSON点数=" + options.maxPointsForJSON +
                    ", 采样=" + options.enableSampling +
                    ", 分页=" + options.enablePagination);

            // 读取LAS文件
            List<LASPoint> allPoints = readLASPoints(filePath);

            // 过滤点云
            List<LASPoint> filteredPoints = filterPointsByClassification(allPoints, classificationValues);

            if (filteredPoints.isEmpty()) {
                return createErrorResult("未找到指定分类的点", startTime);
            }

            // 统计分类信息
            Map<Integer, Integer> classStats = new HashMap<>();
            for (LASPoint point : filteredPoints) {
                classStats.merge(point.classification, 1, Integer::sum);
            }

            String originalFileName = Paths.get(filePath).getFileName().toString();

            // 决定处理策略
            boolean needsSampling = filteredPoints.size() > options.maxPointsForJSON && options.enableSampling;
            boolean needsPagination = filteredPoints.size() > options.maxPointsForJSON && options.enablePagination && !needsSampling;

            List<LASPoint> jsonPoints = filteredPoints;
            boolean isSampled = false;
            boolean isPaginated = false;
            int totalPages = 1;
            List<String> pageFiles = new ArrayList<>();
            String jsonData = null;

            if (needsSampling) {
                // 采样处理
                System.out.println("数据量过大，启用采样处理...");
                jsonPoints = samplePoints(filteredPoints, options.maxPointsForJSON, options.samplingStrategy);
                isSampled = true;
                jsonData = generateJSONString(jsonPoints, normalizeCoords, originalFileName);

                // 保存完整数据到文件，采样数据返回JSON
                if (outputPath != null) {
                    String fullDataPath = determineOutputPath(outputPath, originalFileName);
                    saveJSONToFile(generateJSONString(filteredPoints, normalizeCoords, originalFileName), fullDataPath);
                    pageFiles.add(fullDataPath);
                }

            } else if (needsPagination) {
                // 分页处理
                System.out.println("数据量过大，启用分页处理...");
                totalPages = (int) Math.ceil((double) filteredPoints.size() / options.pageSize);
                isPaginated = true;

                // 生成分页文件
                if (outputPath != null) {
                    pageFiles = generatePagedFiles(filteredPoints, outputPath, originalFileName,
                            options.pageSize, normalizeCoords);
                }

                // 返回第一页数据作为JSON
                int firstPageSize = Math.min(options.pageSize, filteredPoints.size());
                jsonPoints = filteredPoints.subList(0, firstPageSize);
                jsonData = generateJSONString(jsonPoints, normalizeCoords, originalFileName);

            } else {
                // 正常处理
                jsonData = generateJSONString(jsonPoints, normalizeCoords, originalFileName);

                if (outputPath != null) {
                    String finalOutputPath = determineOutputPath(outputPath, originalFileName);
                    saveJSONToFile(jsonData, finalOutputPath);
                    pageFiles.add(finalOutputPath);
                }
            }

            long processingTime = System.currentTimeMillis() - startTime;
            System.out.printf("LAS分析完成! 总耗时: %.2f 秒\n", processingTime / 1000.0);

            return new LASAnalysisResult(true, pageFiles.isEmpty() ? null : pageFiles.get(0), null,
                    processingTime, allPoints.size(), filteredPoints.size(), jsonPoints.size(),
                    classStats, jsonData, isSampled, isPaginated, totalPages, pageFiles);

        } catch (Exception e) {
            String errorMsg = "LAS分析过程中出错: " + e.getMessage();
            System.err.println(errorMsg);
            e.printStackTrace();
            return createErrorResult(errorMsg, startTime);
        }
    }

    /**
     * 便捷方法 - Web显示优化（自动采样到10000个点）
     */
    public LASAnalysisResult lasAnalysisForWeb(String filePath, String outputPath, int[] classificationValues) {
        return lasAnalysisOptimized(filePath, outputPath, classificationValues, true,
                ProcessingOptions.forWebDisplay());
    }

    /**
     * 便捷方法 - 大数据集优化（采样到20000个点）
     */
    public LASAnalysisResult lasAnalysisForLargeDataset(String filePath, String outputPath, int[] classificationValues) {
        return lasAnalysisOptimized(filePath, outputPath, classificationValues, true,
                ProcessingOptions.forLargeDataset());
    }

    /**
     * 便捷方法 - 只返回采样后的JSON，不保存文件
     */
    public LASAnalysisResult lasAnalysisJSONSampled(String filePath, int maxPoints) {
        ProcessingOptions options = new ProcessingOptions();
        options.setMaxPointsForJSON(maxPoints);
        options.setEnableSampling(true);

        return lasAnalysisOptimized(filePath, null, null, true, options);
    }

    /**
     * 生成JSON字符串（不保存文件）
     */
    public LASAnalysisResult lasAnalysisToJSON(String filePath, int[] classificationValues, boolean normalizeCoords) {
        return lasAnalysisOptimized(filePath, null, classificationValues, normalizeCoords,
                ProcessingOptions.getDefault());
    }

    /**
     * 保存文件并返回JSON字符串
     */
    public LASAnalysisResult lasAnalysisWithJSON(String filePath, String outputPath, int[] classificationValues) {
        return lasAnalysisOptimized(filePath, outputPath, classificationValues, true,
                ProcessingOptions.getDefault());
    }

    /**
     * 简化版LAS分析方法 - 兼容原有接口
     */
    public LASAnalysisResult lasAnalysis(String filePath, String outputPath) {
        return lasAnalysisOptimized(filePath, outputPath, null, true, ProcessingOptions.getDefault());
    }

    /**
     * 最简版LAS分析方法 - 兼容原有接口
     */
    public LASAnalysisResult lasAnalysis(String filePath) {
        return lasAnalysisOptimized(filePath, null, null, true, ProcessingOptions.getDefault());
    }

    /**
     * 自定义分类值的LAS分析方法 - 兼容原有接口
     */
    public LASAnalysisResult lasAnalysisWithClasses(String filePath, String outputPath, int[] classificationValues) {
        return lasAnalysisOptimized(filePath, outputPath, classificationValues, true, ProcessingOptions.getDefault());
    }

    /**
     * 流式分批分析LAS文件并分批生成JSON片段（适合WebSocket分批推送）
     * @param filePath LAS文件路径
     * @param batchSize 每批点数
     * @param normalizeCoords 是否归一化坐标
     * @param classificationValues 需要的分类（可为null，默认全部）
     * @param jsonBatchConsumer 每批JSON片段的消费函数（如WebSocket推送）
     */
    public void lasAnalysisStream(String filePath, int batchSize, boolean normalizeCoords, int[] classificationValues, java.util.function.Consumer<String> jsonBatchConsumer) throws IOException {
        // 读取LAS文件全部字节
        Path path = Paths.get(filePath);
        byte[] fileBytes = Files.readAllBytes(path);
        ByteBuffer buffer = ByteBuffer.wrap(fileBytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // 读取头部
        LASHeader header = readLASHeader(buffer);
        long totalPoints = header.numberOfPointRecords;
        buffer.position((int) header.offsetToPointData);

        // 过滤分类
        Set<Integer> classSet = null;
        if (classificationValues != null && classificationValues.length > 0) {
            classSet = new HashSet<>();
            for (int c : classificationValues) classSet.add(c);
        }

        List<LASPoint> batch = new ArrayList<>(batchSize);
        int batchIndex = 0;
        int sentPoints = 0;
        BoundingBox bounds = null;
        List<LASPoint> allForBounds = new ArrayList<>();

        // 先遍历一遍获取边界（可选：如需精确归一化）
        if (normalizeCoords) {
            int pos = buffer.position();
            for (long i = 0; i < totalPoints; i++) {
                int rawX = buffer.getInt();
                int rawY = buffer.getInt();
                int rawZ = buffer.getInt();
                double x = rawX * header.xScaleFactor + header.xOffset;
                double y = rawY * header.yScaleFactor + header.yOffset;
                double z = rawZ * header.zScaleFactor + header.zOffset;
                int intensity = buffer.getShort() & 0xFFFF;
                buffer.get(); // flags
                int classification = buffer.get() & 0xFF;
                int skipBytes = header.pointDataRecordLength - 20;
                if (skipBytes > 0) buffer.position(buffer.position() + skipBytes);
                if (classSet == null || classSet.contains(classification)) {
                    allForBounds.add(new LASPoint(x, y, z, intensity, classification));
                }
            }
            bounds = new BoundingBox(allForBounds);
            buffer.position(pos); // 重置回点数据起始
        }

        for (long i = 0; i < totalPoints; i++) {
            int rawX = buffer.getInt();
            int rawY = buffer.getInt();
            int rawZ = buffer.getInt();
            double x = rawX * header.xScaleFactor + header.xOffset;
            double y = rawY * header.yScaleFactor + header.yOffset;
            double z = rawZ * header.zScaleFactor + header.zOffset;
            int intensity = buffer.getShort() & 0xFFFF;
            buffer.get(); // flags
            int classification = buffer.get() & 0xFF;
            int skipBytes = header.pointDataRecordLength - 20;
            if (skipBytes > 0) buffer.position(buffer.position() + skipBytes);
            if (classSet != null && !classSet.contains(classification)) continue;
            LASPoint point = new LASPoint(x, y, z, intensity, classification);
            batch.add(point);
            sentPoints++;
            if (batch.size() == batchSize) {
                String json = generateBatchJSON(batch, normalizeCoords, bounds);
                jsonBatchConsumer.accept(json);
                batch.clear();
                batchIndex++;
            }
        }
        // 处理最后一批
        if (!batch.isEmpty()) {
            String json = generateBatchJSON(batch, normalizeCoords, bounds);
            jsonBatchConsumer.accept(json);
        }
    }

    /**
     * 分批读取LAS文件并分批写入JSON文件，返回所有批次文件路径
     * @param filePath LAS文件路径
     * @param outputDir 输出目录
     * @param batchSize 每批点数
     * @param normalizeCoords 是否归一化坐标
     * @param classificationValues 需要的分类（可为null，默认全部）
     * @return 所有批次文件路径
     * @throws IOException
     */
    public List<String> lasAnalysisStreamToFiles(String filePath, String outputDir, int batchSize, boolean normalizeCoords, int[] classificationValues) throws IOException {
        List<String> batchFiles = new ArrayList<>();
        java.io.File outDir = new java.io.File(outputDir);
        if (!outDir.exists()) outDir.mkdirs();
        final int[] batchIndex = {0};
        lasAnalysisStream(filePath, batchSize, normalizeCoords, classificationValues, jsonBatch -> {
            String batchFileName = String.format("%s/batch_%d.json", outputDir, batchIndex[0]++);
            try (FileWriter fw = new FileWriter(batchFileName)) {
                fw.write(jsonBatch);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            batchFiles.add(batchFileName);
        });
        return batchFiles;
    }

    /**
     * 全局中心点归一化：先统计全局min/max/center，再分批写文件，每批都用全局center归一化
     * @param filePath LAS文件路径
     * @param outputDir 输出目录
     * @param batchSize 每批点数
     * @param classificationValues 需要的分类（可为null，默认全部）
     * @return 所有批次文件路径
     * @throws IOException
     */
    public List<String> lasAnalysisStreamToFilesGlobalNormalized(String filePath, String outputDir, int batchSize, int[] classificationValues) throws IOException {
        // 1. 先统计全局min/max/center
        BoundingBox globalBounds = calcGlobalBounds(filePath, classificationValues);
        List<String> batchFiles = new ArrayList<>();
        java.io.File outDir = new java.io.File(outputDir);
        if (!outDir.exists()) outDir.mkdirs();
        final int[] batchIndex = {0};
        lasAnalysisStreamWithGlobalCenter(filePath, batchSize, globalBounds, classificationValues, jsonBatch -> {
            String batchFileName = String.format("%s/batch_%d.json", outputDir, batchIndex[0]++);
            try (java.io.FileWriter fw = new java.io.FileWriter(batchFileName)) {
                fw.write(jsonBatch);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            batchFiles.add(batchFileName);
        });
        return batchFiles;
    }

    /**
     * 生成单批次JSON（只包含points数组，不含metadata）
     */
    private static String generateBatchJSON(List<LASPoint> points, boolean normalizeCoords, BoundingBox bounds) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode pointsArray = mapper.createArrayNode();
        for (LASPoint point : points) {
            ObjectNode pointNode = mapper.createObjectNode();
            ArrayNode position = mapper.createArrayNode();
            if (normalizeCoords && bounds != null) {
                position.add(point.x - bounds.centerX);
                position.add(point.y - bounds.centerY);
                position.add(point.z - bounds.centerZ);
            } else {
                position.add(point.x);
                position.add(point.y);
                position.add(point.z);
            }
            pointNode.set("position", position);
            pointNode.put("classification", point.classification);
            pointNode.put("intensity", point.intensity);
            pointsArray.add(pointNode);
        }
        root.set("points", pointsArray);
        return mapper.writeValueAsString(root);
    }

    /**
     * 生成LAS文件的metadata JSON（不包含points数组，仅元信息）
     * @param filePath LAS文件路径
     * @param classificationValues 需要的分类（可为null，默认全部）
     * @param normalizeCoords 是否归一化坐标
     * @return metadata部分的JSON字符串
     */
    public String lasMetadataJson(String filePath, int[] classificationValues, boolean normalizeCoords) throws IOException {
        Path path = Paths.get(filePath);
        byte[] fileBytes = Files.readAllBytes(path);
        ByteBuffer buffer = ByteBuffer.wrap(fileBytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        LASHeader header = readLASHeader(buffer);
        long totalPoints = header.numberOfPointRecords;
        buffer.position((int) header.offsetToPointData);
        Set<Integer> classSet = null;
        if (classificationValues != null && classificationValues.length > 0) {
            classSet = new HashSet<>();
            for (int c : classificationValues) classSet.add(c);
        }
        List<LASPoint> allForBounds = new ArrayList<>();
        for (long i = 0; i < totalPoints; i++) {
            int rawX = buffer.getInt();
            int rawY = buffer.getInt();
            int rawZ = buffer.getInt();
            double x = rawX * header.xScaleFactor + header.xOffset;
            double y = rawY * header.yScaleFactor + header.yOffset;
            double z = rawZ * header.zScaleFactor + header.zOffset;
            int intensity = buffer.getShort() & 0xFFFF;
            buffer.get(); // flags
            int classification = buffer.get() & 0xFF;
            int skipBytes = header.pointDataRecordLength - 20;
            if (skipBytes > 0) buffer.position(buffer.position() + skipBytes);
            if (classSet == null || classSet.contains(classification)) {
                allForBounds.add(new LASPoint(x, y, z, intensity, classification));
            }
        }
        BoundingBox bounds = new BoundingBox(allForBounds);
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode metadata = mapper.createObjectNode();
        metadata.put("version", 1.0);
        metadata.put("type", "pointcloud");
        metadata.put("generator", "MI Project LAS Extractor v2.0");
        metadata.put("count", allForBounds.size());
        metadata.put("original_file", Paths.get(filePath).getFileName().toString());
        metadata.put("timestamp", System.currentTimeMillis());
        ObjectNode boundsNode = mapper.createObjectNode();
        ArrayNode minArray = mapper.createArrayNode();
        minArray.add(bounds.minX).add(bounds.minY).add(bounds.minZ);
        ArrayNode maxArray = mapper.createArrayNode();
        maxArray.add(bounds.maxX).add(bounds.maxY).add(bounds.maxZ);
        ArrayNode centerArray = mapper.createArrayNode();
        centerArray.add(bounds.centerX).add(bounds.centerY).add(bounds.centerZ);
        boundsNode.set("min", minArray);
        boundsNode.set("max", maxArray);
        boundsNode.set("center", centerArray);
        metadata.set("bounds", boundsNode);
        ObjectNode root = mapper.createObjectNode();
        root.set("metadata", metadata);
        return mapper.writeValueAsString(root);
    }

    /**
     * 先遍历一遍LAS文件，统计全局min/max/center
     */
    private BoundingBox calcGlobalBounds(String filePath, int[] classificationValues) throws IOException {
        Path path = Paths.get(filePath);
        byte[] fileBytes = Files.readAllBytes(path);
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(fileBytes);
        buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
        LASHeader header = readLASHeader(buffer);
        buffer.position((int) header.offsetToPointData);
        List<LASPoint> allPoints = new ArrayList<>();
        java.util.Set<Integer> classSet = null;
        if (classificationValues != null && classificationValues.length > 0) {
            classSet = new java.util.HashSet<>();
            for (int c : classificationValues) classSet.add(c);
        }
        for (long i = 0; i < header.numberOfPointRecords; i++) {
            int rawX = buffer.getInt();
            int rawY = buffer.getInt();
            int rawZ = buffer.getInt();
            double x = rawX * header.xScaleFactor + header.xOffset;
            double y = rawY * header.yScaleFactor + header.yOffset;
            double z = rawZ * header.zScaleFactor + header.zOffset;
            int intensity = buffer.getShort() & 0xFFFF;
            buffer.get(); // flags
            int classification = buffer.get() & 0xFF;
            int skipBytes = header.pointDataRecordLength - 20;
            if (skipBytes > 0) buffer.position(buffer.position() + skipBytes);
            if (classSet == null || classSet.contains(classification)) {
                allPoints.add(new LASPoint(x, y, z, intensity, classification));
            }
        }
        return new BoundingBox(allPoints);
    }

    /**
     * 分批读取LAS文件，每批都用全局center归一化，生成JSON字符串
     */
    private void lasAnalysisStreamWithGlobalCenter(String filePath, int batchSize, BoundingBox globalBounds, int[] classificationValues, java.util.function.Consumer<String> jsonBatchConsumer) throws IOException {
        Path path = Paths.get(filePath);
        byte[] fileBytes = Files.readAllBytes(path);
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(fileBytes);
        buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
        LASHeader header = readLASHeader(buffer);
        buffer.position((int) header.offsetToPointData);
        java.util.Set<Integer> classSet = null;
        if (classificationValues != null && classificationValues.length > 0) {
            classSet = new java.util.HashSet<>();
            for (int c : classificationValues) classSet.add(c);
        }
        List<LASPoint> batch = new ArrayList<>(batchSize);
        for (long i = 0; i < header.numberOfPointRecords; i++) {
            int rawX = buffer.getInt();
            int rawY = buffer.getInt();
            int rawZ = buffer.getInt();
            double x = rawX * header.xScaleFactor + header.xOffset;
            double y = rawY * header.yScaleFactor + header.yOffset;
            double z = rawZ * header.zScaleFactor + header.zOffset;
            int intensity = buffer.getShort() & 0xFFFF;
            buffer.get(); // flags
            int classification = buffer.get() & 0xFF;
            int skipBytes = header.pointDataRecordLength - 20;
            if (skipBytes > 0) buffer.position(buffer.position() + skipBytes);
            if (classSet != null && !classSet.contains(classification)) continue;
            batch.add(new LASPoint(x, y, z, intensity, classification));
            if (batch.size() == batchSize) {
                String json = generateBatchJSONWithGlobalCenter(batch, globalBounds);
                jsonBatchConsumer.accept(json);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            String json = generateBatchJSONWithGlobalCenter(batch, globalBounds);
            jsonBatchConsumer.accept(json);
        }
    }

    /**
     * 生成单批次JSON，所有点用全局center归一化
     */
    private static String generateBatchJSONWithGlobalCenter(List<LASPoint> points, BoundingBox globalBounds) throws IOException {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode root = mapper.createObjectNode();
        com.fasterxml.jackson.databind.node.ArrayNode pointsArray = mapper.createArrayNode();
        for (LASPoint point : points) {
            com.fasterxml.jackson.databind.node.ObjectNode pointNode = mapper.createObjectNode();
            com.fasterxml.jackson.databind.node.ArrayNode position = mapper.createArrayNode();
            position.add(point.x - globalBounds.centerX);
            position.add(point.y - globalBounds.centerY);
            position.add(point.z - globalBounds.centerZ);
            pointNode.set("position", position);
            pointNode.put("classification", point.classification);
            pointNode.put("intensity", point.intensity);
            pointsArray.add(pointNode);
        }
        root.set("points", pointsArray);
        // 只保留points数组，metadata可按需扩展
        return mapper.writeValueAsString(root);
    }

    // ================================ 私有方法 ================================

    /**
     * 读取LAS文件头部信息
     */
    private static LASHeader readLASHeader(ByteBuffer buffer) throws IOException {
        LASHeader header = new LASHeader();

        // 读取文件签名 (4 bytes)
        byte[] signature = new byte[4];
        buffer.get(signature);
        header.fileSignature = new String(signature);

        if (!"LASF".equals(header.fileSignature)) {
            throw new IOException("Invalid LAS file signature: " + header.fileSignature);
        }

        // 跳过一些字段到关键信息
        buffer.position(96); // 跳到 offset to point data
        header.offsetToPointData = Integer.toUnsignedLong(buffer.getInt());

        buffer.position(104); // 跳到 point data record format
        header.pointDataRecordFormat = buffer.get() & 0xFF;
        header.pointDataRecordLength = buffer.getShort() & 0xFFFF;

        buffer.position(107); // 跳到 number of point records
        header.numberOfPointRecords = Integer.toUnsignedLong(buffer.getInt());

        // 读取缩放因子和偏移量
        buffer.position(131);
        header.xScaleFactor = buffer.getDouble();
        header.yScaleFactor = buffer.getDouble();
        header.zScaleFactor = buffer.getDouble();
        header.xOffset = buffer.getDouble();
        header.yOffset = buffer.getDouble();
        header.zOffset = buffer.getDouble();

        // 读取边界信息
        header.maxX = buffer.getDouble();
        header.minX = buffer.getDouble();
        header.maxY = buffer.getDouble();
        header.minY = buffer.getDouble();
        header.maxZ = buffer.getDouble();
        header.minZ = buffer.getDouble();

        return header;
    }

    /**
     * 读取LAS文件中的点云数据
     */
    private static List<LASPoint> readLASPoints(String filePath) throws IOException {
        System.out.println("开始读取LAS文件: " + filePath);
        long startTime = System.currentTimeMillis();

        Path path = Paths.get(filePath);
        byte[] fileBytes = Files.readAllBytes(path);
        ByteBuffer buffer = ByteBuffer.wrap(fileBytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // 读取头部
        LASHeader header = readLASHeader(buffer);
        System.out.println("LAS文件信息: " + header);

        // 移动到点数据开始位置
        buffer.position((int) header.offsetToPointData);

        List<LASPoint> points = new ArrayList<>();

        // 根据点数据格式读取点
        for (long i = 0; i < header.numberOfPointRecords; i++) {
            // 读取XYZ坐标 (每个4字节整数)
            int rawX = buffer.getInt();
            int rawY = buffer.getInt();
            int rawZ = buffer.getInt();

            // 转换为实际坐标
            double x = rawX * header.xScaleFactor + header.xOffset;
            double y = rawY * header.yScaleFactor + header.yOffset;
            double z = rawZ * header.zScaleFactor + header.zOffset;

            // 读取强度 (2字节)
            int intensity = buffer.getShort() & 0xFFFF;

            // 读取分类等信息 (1字节)
            byte flags = buffer.get();

            // 读取分类 (1字节)
            int classification = buffer.get() & 0xFF;

            // 跳过其他字段，根据格式不同跳过不同字节数
            int skipBytes = header.pointDataRecordLength - 20; // 已读取20字节
            if (skipBytes > 0) {
                buffer.position(buffer.position() + skipBytes);
            }

            points.add(new LASPoint(x, y, z, intensity, classification));

            // 进度显示
            if (i % 100000 == 0 && i > 0) {
                System.out.printf("已读取 %d / %d 点 (%.1f%%)\n",
                        i, header.numberOfPointRecords, (double) i / header.numberOfPointRecords * 100);
            }
        }

        long endTime = System.currentTimeMillis();
        System.out.printf("读取完成! 共 %d 个点，耗时: %.2f 秒\n",
                points.size(), (endTime - startTime) / 1000.0);

        return points;
    }

    /**
     * 根据分类值过滤点云数据
     */
    private static List<LASPoint> filterPointsByClassification(List<LASPoint> points, int[] classificationValues) {
        Set<Integer> targetClasses = new HashSet<>();
        for (int cls : classificationValues) {
            targetClasses.add(cls);
        }

        List<LASPoint> filteredPoints = new ArrayList<>();
        Map<Integer, Integer> classCounts = new HashMap<>();

        for (LASPoint point : points) {
            if (targetClasses.contains(point.classification)) {
                filteredPoints.add(point);
                classCounts.merge(point.classification, 1, Integer::sum);
            }
        }

        System.out.printf("过滤结果: 从 %d 个点中找到 %d 个目标点 (%.2f%%)\n",
                points.size(), filteredPoints.size(),
                (double) filteredPoints.size() / points.size() * 100);

        classCounts.forEach((cls, count) ->
                System.out.printf("  分类 %d: %d 个点\n", cls, count));

        return filteredPoints;
    }

    /**
     * 点云采样
     */
    private List<LASPoint> samplePoints(List<LASPoint> points, int maxPoints, SamplingStrategy strategy) {
        if (points.size() <= maxPoints) {
            return points;
        }

        System.out.printf("采样: %d -> %d 点 (策略: %s)\n", points.size(), maxPoints, strategy);

        switch (strategy) {
            case UNIFORM:
                return uniformSampling(points, maxPoints);
            case RANDOM:
                return randomSampling(points, maxPoints);
            case GRID_BASED:
                return gridBasedSampling(points, maxPoints);
            case INTENSITY:
                return intensityBasedSampling(points, maxPoints);
            default:
                return uniformSampling(points, maxPoints);
        }
    }

    /**
     * 均匀采样
     */
    private List<LASPoint> uniformSampling(List<LASPoint> points, int maxPoints) {
        List<LASPoint> sampled = new ArrayList<>();
        double step = (double) points.size() / maxPoints;

        for (int i = 0; i < maxPoints; i++) {
            int index = (int) (i * step);
            if (index < points.size()) {
                sampled.add(points.get(index));
            }
        }

        return sampled;
    }

    /**
     * 随机采样
     */
    private List<LASPoint> randomSampling(List<LASPoint> points, int maxPoints) {
        List<LASPoint> sampled = new ArrayList<>(points);
        Collections.shuffle(sampled);
        return sampled.subList(0, Math.min(maxPoints, sampled.size()));
    }

    /**
     * 网格采样 - 空间均匀分布
     */
    private List<LASPoint> gridBasedSampling(List<LASPoint> points, int maxPoints) {
        BoundingBox bounds = new BoundingBox(points);

        // 计算网格大小
        int gridSize = (int) Math.sqrt(maxPoints);
        double cellWidth = (bounds.maxX - bounds.minX) / gridSize;
        double cellHeight = (bounds.maxY - bounds.minY) / gridSize;

        Map<String, LASPoint> gridMap = new HashMap<>();

        for (LASPoint point : points) {
            int gridX = (int) ((point.x - bounds.minX) / cellWidth);
            int gridY = (int) ((point.y - bounds.minY) / cellHeight);
            String key = gridX + "," + gridY;

            // 每个网格只保留一个点（或者选择强度最高的）
            if (!gridMap.containsKey(key) || point.intensity > gridMap.get(key).intensity) {
                gridMap.put(key, point);
            }
        }

        return new ArrayList<>(gridMap.values());
    }

    /**
     * 基于强度的采样
     */
    private List<LASPoint> intensityBasedSampling(List<LASPoint> points, int maxPoints) {
        List<LASPoint> sorted = new ArrayList<>(points);
        sorted.sort((a, b) -> Integer.compare(b.intensity, a.intensity)); // 按强度降序
        return sorted.subList(0, Math.min(maxPoints, sorted.size()));
    }

    /**
     * 生成分页文件
     */
    private List<String> generatePagedFiles(List<LASPoint> points, String outputPath, String originalFileName,
                                            int pageSize, boolean normalizeCoords) throws IOException {
        List<String> pageFiles = new ArrayList<>();
        int totalPages = (int) Math.ceil((double) points.size() / pageSize);

        String basePath = determineOutputPath(outputPath, originalFileName);
        String baseFileName = basePath.substring(0, basePath.lastIndexOf('.'));

        for (int page = 0; page < totalPages; page++) {
            int startIndex = page * pageSize;
            int endIndex = Math.min(startIndex + pageSize, points.size());
            List<LASPoint> pagePoints = points.subList(startIndex, endIndex);

            String pageFileName = String.format("%s_page_%d_of_%d.json", baseFileName, page + 1, totalPages);
            String pageJson = generateJSONString(pagePoints, normalizeCoords, originalFileName);
            saveJSONToFile(pageJson, pageFileName);
            pageFiles.add(pageFileName);

            System.out.printf("生成分页文件 %d/%d: %s (%d 点)\n", page + 1, totalPages,
                    Paths.get(pageFileName).getFileName(), pagePoints.size());
        }

        return pageFiles;
    }

    /**
     * 生成JSON字符串
     */
    private static String generateJSONString(List<LASPoint> points, boolean normalizeCoords, String originalFile) throws IOException {
        System.out.println("开始构建JSON数据...");

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();

        // 计算边界框
        BoundingBox bounds = new BoundingBox(points);
        System.out.println("点云范围: " + bounds);

        // 创建metadata
        ObjectNode metadata = mapper.createObjectNode();
        metadata.put("version", 1.0);
        metadata.put("type", "pointcloud");
        metadata.put("generator", "MI Project LAS Extractor v2.0");
        metadata.put("count", points.size());
        metadata.put("original_file", originalFile);
        metadata.put("timestamp", System.currentTimeMillis());

        // 添加边界信息
        ObjectNode boundsNode = mapper.createObjectNode();
        ArrayNode minArray = mapper.createArrayNode();
        minArray.add(bounds.minX).add(bounds.minY).add(bounds.minZ);
        ArrayNode maxArray = mapper.createArrayNode();
        maxArray.add(bounds.maxX).add(bounds.maxY).add(bounds.maxZ);
        ArrayNode centerArray = mapper.createArrayNode();
        centerArray.add(bounds.centerX).add(bounds.centerY).add(bounds.centerZ);

        boundsNode.set("min", minArray);
        boundsNode.set("max", maxArray);
        boundsNode.set("center", centerArray);
        metadata.set("bounds", boundsNode);

        root.set("metadata", metadata);

        // 创建points数组
        ArrayNode pointsArray = mapper.createArrayNode();

        for (LASPoint point : points) {
            ObjectNode pointNode = mapper.createObjectNode();

            ArrayNode position = mapper.createArrayNode();
            if (normalizeCoords) {
                // 归一化坐标
                position.add(point.x - bounds.centerX);
                position.add(point.y - bounds.centerY);
                position.add(point.z - bounds.centerZ);
            } else {
                position.add(point.x);
                position.add(point.y);
                position.add(point.z);
            }

            pointNode.set("position", position);
            pointNode.put("classification", point.classification);
            pointNode.put("intensity", point.intensity);

            pointsArray.add(pointNode);
        }

        root.set("points", pointsArray);

        // 转换为JSON字符串
        return mapper.writeValueAsString(root);
    }

    /**
     * 确定输出文件路径
     */
    private static String determineOutputPath(String outputPath, String originalFile) {
        File outputFile = new File(outputPath);

        if (outputFile.exists() && outputFile.isDirectory()) {
            // 如果是存在的目录，在目录下生成文件名
            String baseName = originalFile.substring(0, originalFile.lastIndexOf('.'));
            String fileName = baseName + "_analysis.json";
            String finalPath = Paths.get(outputPath, fileName).toString();
            System.out.println("检测到目录路径，生成文件名: " + fileName);
            return finalPath;
        } else if (outputPath.endsWith("\\") || outputPath.endsWith("/")) {
            // 如果路径以分隔符结尾，说明是目录路径
            outputFile.mkdirs();
            String baseName = originalFile.substring(0, originalFile.lastIndexOf('.'));
            String fileName = baseName + "_analysis.json";
            String finalPath = Paths.get(outputPath, fileName).toString();
            System.out.println("创建目录并生成文件名: " + fileName);
            return finalPath;
        } else if (!outputPath.toLowerCase().endsWith(".json")) {
            // 判断是目录还是文件
            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                // 父目录不存在，可能是想创建目录结构
                parentDir.mkdirs();
                String baseName = originalFile.substring(0, originalFile.lastIndexOf('.'));
                String fileName = baseName + "_analysis.json";
                String finalPath = Paths.get(outputPath, fileName).toString();
                System.out.println("创建目录结构并生成文件名: " + fileName);
                return finalPath;
            } else {
                // 添加.json扩展名
                String finalPath = outputPath + ".json";
                System.out.println("添加.json扩展名: " + finalPath);
                return finalPath;
            }
        } else {
            // 已经是完整的文件路径
            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
                System.out.println("创建父目录: " + parentDir.getAbsolutePath());
            }
            return outputPath;
        }
    }

    /**
     * 保存JSON字符串到文件
     */
    private static void saveJSONToFile(String jsonData, String outputPath) throws IOException {
        System.out.println("正在保存JSON文件到: " + outputPath);

        // 创建父目录（如果不存在）
        File outputFile = new File(outputPath);
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // 使用OutputStreamWriter显式指定UTF-8编码
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputPath),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write(jsonData);
            writer.flush();
        }

        // 显示文件大小
        double fileSizeMB = outputFile.length() / (1024.0 * 1024.0);
        System.out.printf("JSON文件大小: %.2f MB (UTF-8编码)\n", fileSizeMB);
        System.out.printf("文件保存成功: %s\n", outputFile.getAbsolutePath());
    }

    /**
     * 创建错误结果
     */
    private LASAnalysisResult createErrorResult(String errorMessage, long startTime) {
        return new LASAnalysisResult(false, null, errorMessage,
                System.currentTimeMillis() - startTime, 0, 0, 0, null, null,
                false, false, 0, new ArrayList<>());
    }
}