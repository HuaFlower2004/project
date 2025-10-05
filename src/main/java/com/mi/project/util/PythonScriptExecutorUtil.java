package com.mi.project.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.io.File;

@Slf4j
@Component
public class PythonScriptExecutorUtil {
    private final String pythonScriptPath = "C:\\Users\\31591\\Desktop\\project\\src\\main\\resources\\python\\check.py";

    private final String pythonExecutable = "python";

    private final Long timeoutSeconds = 300L;

    public String executeFileAnalysis(String postParams, String absolutePath, String outPath) {
        try {
            // 构建Python命令
            List<String> command = new ArrayList<>();
            command.add(pythonExecutable);
            command.add(pythonScriptPath);// Python脚本路径
            command.add(absolutePath);
            command.add(outPath);// 文件路径参数
            // 增加 --classes 参数支持

            log.info("执行Python命令: {}", String.join(" ", command));

            // 创建进程
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true); // 合并错误流和输出流

            Process process = processBuilder.start();

            LocalDateTime localDateTime = LocalDateTime.now();
            String json = "{\n" +
                    "  \"type\": \"start\",\n" +
                    "  \"message\": \"python程序开始进行\",\n" +
                    "  \"data\": {\n" +
                    "    \"timestamp\": \"" + localDateTime.toString() + "\"\n" +
                    "  }\n" +
                    "}";
            WebSocketSenderUtil.sendJsonToAll(json);

            // 读取Python脚本的输出（指定UTF-8编码，防止中文乱码）
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    log.debug("Python输出: {}", line);
                }
            }

            // 等待进程完成
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Python脚本执行超时");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new RuntimeException("Python脚本执行失败，退出码: " + exitCode + ", 输出: " + output.toString());
            }

            String result = output.toString().trim();
            log.info("Python脚本执行成功，结果长度: {}", result.length());

            return result;

        } catch (Exception e) {
            log.error("执行Python脚本失败: {}", e.getMessage(), e);
            throw new RuntimeException("Python脚本执行失败: " + e.getMessage());
        }
    }

    public boolean checkPythonEnvironment() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(pythonExecutable, "--version");
            Process process = processBuilder.start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                return false;
            }

            return process.exitValue() == 0;

        } catch (Exception e) {
            log.error("检查Python环境失败: {}", e.getMessage());
            return false;
        }
    }

    public List<String> runLastile(String inputLas, String outputDir, String outputPrefix) {
        List<String> lasPaths = new ArrayList<>();
        try {
            // 构建Python命令
            List<String> command = new ArrayList<>();
            command.add("python");
            command.add("C:\\Users\\31591\\Desktop\\project\\src\\main\\resources\\python\\1-lastile_wrapper.py");
            command.add("--input");
            command.add(inputLas);
            command.add("--output_dir");
            command.add(outputDir);
            command.add("--prefix");
            command.add(outputPrefix);
            command.add("--generate_threejs_params");

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 读取输出
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[lastile_wrapper.py] {}", line);
                }
            }

            boolean finished = process.waitFor(300, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("lastile_wrapper.py 执行超时");
            }
            if (process.exitValue() != 0) {
                throw new RuntimeException("lastile_wrapper.py 执行失败，退出码: " + process.exitValue());
            }

            // 收集分段文件路径
            java.io.File outDir = new java.io.File(outputDir);
            java.io.File[] lasFiles = outDir.listFiles((dir1, name) -> name.endsWith(".las"));
            if (lasFiles != null) {
                for (java.io.File f : lasFiles) {
                    lasPaths.add(f.getAbsolutePath());
                }
            }
            return lasPaths;
        } catch (Exception e) {
            log.error("lastile_wrapper.py 执行失败: {}", e.getMessage(), e);
            throw new RuntimeException("las文件分段失败: " + e.getMessage());
        }
    }

    /**
     * 批量处理分段 las 文件，依次调用 2-process_tiles.py 和 3-powerline_extractor.py，将结果输出到指定目录
     * 
     * @param tileLasFiles 分段 las 文件的绝对路径列表
     * @param processDir   处理后 las 文件的输出目录
     * @return 处理后 las 文件的绝对路径列表
     */
    public List<String> processTilesAndExtractPowerlines(List<String> tileLasFiles, String processDir) {
        // 多线程处理，线程数不超过10
        int threadCount = Math.min(5, Math.max(2, Runtime.getRuntime().availableProcessors()));
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
        List<java.util.concurrent.Future<List<String>>> futures = new ArrayList<>();
        String processTilesPy = "C:\\Users\\31591\\Desktop\\project\\src\\main\\resources\\python\\2-process_tiles.py";
        String powerlinePy = "C:\\Users\\31591\\Desktop\\project\\src\\main\\resources\\python\\3-powerline_extractor.py";

        for (String tileLas : tileLasFiles) {
            futures.add(executor.submit(() -> {
                List<String> processedLasFiles = new ArrayList<>();
                try {
                    String tileName = new File(tileLas).getName().replace(".las", "");
                    String tileOutputDir = processDir + File.separator + tileName;
                    new File(tileOutputDir).mkdirs();

                    List<String> command = new ArrayList<>();
                    command.add("python");
                    command.add(processTilesPy);
                    command.add("--input");
                    command.add(tileLas);
                    command.add("--output");
                    command.add(tileOutputDir);
                    command.add("--script");
                    command.add(powerlinePy);

                    ProcessBuilder pb = new ProcessBuilder(command);
                    pb.redirectErrorStream(true);
                    Process process = pb.start();

                    // 读取输出日志
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            log.info("[process_tiles] {}", line);
                        }
                    }
                    int exitCode = process.waitFor();
                    if (exitCode != 0) {
                        log.error("处理分段文件失败: {}，退出码: {}", tileLas, exitCode);
                        return processedLasFiles;
                    }
                    // 收集本次处理后生成的 las 文件
                    java.io.File outDir = new java.io.File(tileOutputDir);
                    java.io.File[] lasFiles = outDir.listFiles((dir, name) -> name.endsWith(".las"));
                    if (lasFiles != null) {
                        for (java.io.File f : lasFiles) {
                            processedLasFiles.add(f.getAbsolutePath());
                        }
                    }
                } catch (Exception e) {
                    log.error("处理分段文件异常: {}", tileLas, e);
                }
                return processedLasFiles;
            }));
        }

        List<String> allProcessedLasFiles = new ArrayList<>();
        for (java.util.concurrent.Future<List<String>> future : futures) {
            try {
                allProcessedLasFiles.addAll(future.get());
            } catch (Exception e) {
                log.error("多线程处理分段文件异常", e);
            }
        }
        executor.shutdown();
        return allProcessedLasFiles;
    }

    public void runRansacFitToJsonOnProcessFolders(String processDir, String normJsonPath, String ransacJsonDir) {
        String ransacPy = "C:\\Users\\31591\\Desktop\\project\\src\\main\\resources\\python\\5-ransac_fit_to_json.py";
        File processRoot = new File(processDir);
        File[] subDirs = processRoot.listFiles(File::isDirectory);
        if (subDirs == null)
            return;

        for (File subDir : subDirs) {
            File allPowerLines = new File(subDir, "all_power_lines.las");
            if (allPowerLines.exists()) {
                try {
                    List<String> command = new ArrayList<>();
                    command.add("python");
                    command.add(ransacPy);
                    command.add("-i");
                    command.add(allPowerLines.getAbsolutePath());
                    command.add("-o");
                    command.add(ransacJsonDir);
                    command.add("-nf");
                    command.add(normJsonPath);
                    // 可选：生成html报告
                    // command.add("--visualize");

                    ProcessBuilder pb = new ProcessBuilder(command);
                    pb.redirectErrorStream(true);
                    Process process = pb.start();

                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            log.info("[ransac_fit_to_json] {}", line);
                        }
                    }
                    int exitCode = process.waitFor();
                    if (exitCode != 0) {
                        log.error("ransac_fit_to_json 处理失败: {}，退出码: {}", allPowerLines.getAbsolutePath(), exitCode);
                    }
                } catch (Exception e) {
                    log.error("ransac_fit_to_json 处理异常: {}", allPowerLines.getAbsolutePath(), e);
                }
            }
        }
    }

    public void runPointToJsonOnProcessFolders(String processDir, String normJsonPath, String pointJsonDir) {
        String pointPy = "C:\\Users\\31591\\Desktop\\project\\src\\main\\resources\\python\\6-point_to_json.py";
        File processRoot = new File(processDir);
        File[] subDirs = processRoot.listFiles(File::isDirectory);
        if (subDirs == null)
            return;

        for (File subDir : subDirs) {
            File allPowerLines = new File(subDir, "all_power_lines.las");
            if (allPowerLines.exists()) {
                try {
                    List<String> command = new ArrayList<>();
                    command.add("python");
                    command.add(pointPy);
                    command.add("--input");
                    command.add(allPowerLines.getAbsolutePath());
                    command.add("--output");
                    command.add(pointJsonDir);
                    command.add("--normalization_file");
                    command.add(normJsonPath);

                    ProcessBuilder pb = new ProcessBuilder(command);
                    pb.redirectErrorStream(true);
                    Process process = pb.start();

                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            log.info("[point_to_json] {}", line);
                        }
                    }
                    int exitCode = process.waitFor();
                    if (exitCode != 0) {
                        log.error("point_to_json 处理失败: {}，退出码: {}", allPowerLines.getAbsolutePath(), exitCode);
                    }
                } catch (Exception e) {
                    log.error("point_to_json 处理异常: {}", allPowerLines.getAbsolutePath(), e);
                }
            }
        }
    }
}
