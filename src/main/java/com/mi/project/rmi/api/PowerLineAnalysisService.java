package com.mi.project.rmi.api;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

/**
 * 电力线分析远程服务接口
 * 提供电力线点云数据处理和分析功能
 */
public interface PowerLineAnalysisService extends Remote {

    /**
     * 处理LAS文件并提取电力线
     * @param filePath LAS文件路径
     * @param outputDir 输出目录
     * @return 处理结果
     * @throws RemoteException
     */
    Map<String, Object> processLasFile(String filePath, String outputDir) throws RemoteException;

    /**
     * 执行电力线提取算法
     * @param inputPath 输入文件路径
     * @param parameters 算法参数
     * @return 提取结果
     * @throws RemoteException
     */
    List<Map<String, Object>> extractPowerLines(String inputPath, Map<String, Object> parameters) throws RemoteException;

    /**
     * 执行RANSAC拟合
     * @param powerLineData 电力线数据
     * @param fitParameters 拟合参数
     * @return 拟合结果
     * @throws RemoteException
     */
    Map<String, Object> performRansacFitting(List<Map<String, Object>> powerLineData, Map<String, Object> fitParameters) throws RemoteException;

    /**
     * 生成分析报告
     * @param analysisData 分析数据
     * @param reportType 报告类型 (html, json, txt)
     * @return 报告内容
     * @throws RemoteException
     */
    String generateAnalysisReport(Map<String, Object> analysisData, String reportType) throws RemoteException;

    /**
     * 获取处理状态
     * @param taskId 任务ID
     * @return 状态信息
     * @throws RemoteException
     */
    Map<String, Object> getProcessingStatus(String taskId) throws RemoteException;

    /**
     * 取消处理任务
     * @param taskId 任务ID
     * @return 取消结果
     * @throws RemoteException
     */
    boolean cancelProcessing(String taskId) throws RemoteException;

    /**
     * 获取服务健康状态
     * @return 健康状态
     * @throws RemoteException
     */
    Map<String, Object> getHealthStatus() throws RemoteException;
}

