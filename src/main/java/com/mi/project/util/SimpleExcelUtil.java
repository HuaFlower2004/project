package com.mi.project.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 简化版Excel工具类
 * 专门用于电力线分析系统的Excel导入导出
 */
@Slf4j
@Component
public class SimpleExcelUtil {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /**
     * 导出用户数据到Excel
     */
    public void exportUsersToExcel(List<Map<String, Object>> userData, String fileName, HttpServletResponse response) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("用户数据");
            // 创建标题行
            String[] headers = {"ID", "用户名", "邮箱", "手机号", "状态", "创建时间"};
            Row headerRow = sheet.createRow(0);
            // 设置标题样式
            CellStyle headerStyle = createHeaderStyle(workbook);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            // 填充数据
            for (int i = 0; i < userData.size(); i++) {
                Row row = sheet.createRow(i + 1);
                Map<String, Object> user = userData.get(i);
                
                row.createCell(0).setCellValue(user.get("id") != null ? user.get("id").toString() : "");
                row.createCell(1).setCellValue(user.get("userName") != null ? user.get("userName").toString() : "");
                row.createCell(2).setCellValue(user.get("email") != null ? user.get("email").toString() : "");
                row.createCell(3).setCellValue(user.get("phoneNumber") != null ? user.get("phoneNumber").toString() : "");
                row.createCell(4).setCellValue(user.get("isActive") != null ? (Boolean) user.get("isActive") ? "激活" : "禁用" : "");
                row.createCell(5).setCellValue(user.get("createdTime") != null ? user.get("createdTime").toString() : "");
            }
            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + 
                    java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8));
            // 写入响应
            workbook.write(response.getOutputStream());
            response.getOutputStream().flush();
            log.info("用户数据Excel导出成功: fileName={}, rows={}", fileName, userData.size());
        } catch (IOException e) {
            log.error("用户数据Excel导出失败: fileName={}", fileName, e);
            throw new RuntimeException("Excel导出失败", e);
        }
    }

    /**
     * 导出文件处理记录到Excel
     */
    public void exportFileRecordsToExcel(List<Map<String, Object>> fileData, String fileName, HttpServletResponse response) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("文件处理记录");
            
            // 创建标题行
            String[] headers = {"文件ID", "文件名", "文件类型", "文件大小", "处理状态", "上传时间", "处理时间", "用户名"};
            Row headerRow = sheet.createRow(0);
            
            // 设置标题样式
            CellStyle headerStyle = createHeaderStyle(workbook);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 填充数据
            for (int i = 0; i < fileData.size(); i++) {
                Row row = sheet.createRow(i + 1);
                Map<String, Object> file = fileData.get(i);
                
                row.createCell(0).setCellValue(file.get("id") != null ? file.get("id").toString() : "");
                row.createCell(1).setCellValue(file.get("fileName") != null ? file.get("fileName").toString() : "");
                row.createCell(2).setCellValue(file.get("fileType") != null ? file.get("fileType").toString() : "");
                row.createCell(3).setCellValue(file.get("fileSize") != null ? file.get("fileSize").toString() : "");
                row.createCell(4).setCellValue(file.get("fileStatus") != null ? file.get("fileStatus").toString() : "");
                row.createCell(5).setCellValue(file.get("uploadTime") != null ? file.get("uploadTime").toString() : "");
                row.createCell(6).setCellValue(file.get("processTime") != null ? file.get("processTime").toString() : "");
                row.createCell(7).setCellValue(file.get("userName") != null ? file.get("userName").toString() : "");
            }

            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + 
                    java.net.URLEncoder.encode(fileName, "UTF-8"));
            
            // 写入响应
            workbook.write(response.getOutputStream());
            response.getOutputStream().flush();
            log.info("文件记录Excel导出成功: fileName={}, rows={}", fileName, fileData.size());

        } catch (IOException e) {
            log.error("文件记录Excel导出失败: fileName={}", fileName, e);
            throw new RuntimeException("Excel导出失败", e);
        }
    }

    /**
     * 导出电力线分析结果到Excel
     */
    public void exportPowerlineAnalysisToExcel(List<Map<String, Object>> analysisData, String fileName, HttpServletResponse response) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("电力线分析结果");
            
            // 创建标题行
            String[] headers = {"分析ID", "文件ID", "电力线数量", "检测准确率", "处理时间", "电压等级", "线路长度", "分析状态"};
            Row headerRow = sheet.createRow(0);
            
            // 设置标题样式
            CellStyle headerStyle = createHeaderStyle(workbook);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 填充数据
            for (int i = 0; i < analysisData.size(); i++) {
                Row row = sheet.createRow(i + 1);
                Map<String, Object> analysis = analysisData.get(i);
                
                row.createCell(0).setCellValue(analysis.get("id") != null ? analysis.get("id").toString() : "");
                row.createCell(1).setCellValue(analysis.get("fileId") != null ? analysis.get("fileId").toString() : "");
                row.createCell(2).setCellValue(analysis.get("powerlineCount") != null ? analysis.get("powerlineCount").toString() : "");
                row.createCell(3).setCellValue(analysis.get("accuracy") != null ? analysis.get("accuracy").toString() : "");
                row.createCell(4).setCellValue(analysis.get("processTime") != null ? analysis.get("processTime").toString() : "");
                row.createCell(5).setCellValue(analysis.get("voltageLevel") != null ? analysis.get("voltageLevel").toString() : "");
                row.createCell(6).setCellValue(analysis.get("lineLength") != null ? analysis.get("lineLength").toString() : "");
                row.createCell(7).setCellValue(analysis.get("status") != null ? analysis.get("status").toString() : "");
            }

            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + 
                    java.net.URLEncoder.encode(fileName, "UTF-8"));
            
            // 写入响应
            workbook.write(response.getOutputStream());
            response.getOutputStream().flush();
            log.info("电力线分析结果Excel导出成功: fileName={}, rows={}", fileName, analysisData.size());

        } catch (IOException e) {
            log.error("电力线分析结果Excel导出失败: fileName={}", fileName, e);
            throw new RuntimeException("Excel导出失败", e);
        }
    }

    /**
     * 从Excel导入用户数据
     */
    public List<Map<String, Object>> importUsersFromExcel(MultipartFile file) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        if (file == null || file.isEmpty()) {
            log.warn("上传文件为空");
            return result;
        }
        
        String fileName = file.getOriginalFilename();
        if (fileName == null || (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
            log.warn("文件格式不支持: {}", fileName);
            throw new RuntimeException("只支持Excel文件格式(.xlsx, .xls)");
        }
        
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                log.warn("Excel文件没有工作表");
                return result;
            }

            // 跳过标题行，从第二行开始读取数据
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    Map<String, Object> user = new HashMap<>();
                    user.put("userName", getCellValueAsString(row.getCell(0)));
                    user.put("email", getCellValueAsString(row.getCell(1)));
                    user.put("phoneNumber", getCellValueAsString(row.getCell(2)));
                    user.put("password", "123456"); // 默认密码
                    user.put("isActive", true);
                    user.put("createdTime", LocalDateTime.now());
                    
                    result.add(user);
                } catch (Exception e) {
                    log.warn("解析第{}行用户数据失败: {}", i + 1, e.getMessage());
                }
            }

            log.info("用户数据Excel导入成功: fileName={}, rows={}", fileName, result.size());
            return result;

        } catch (IOException e) {
            log.error("用户数据Excel导入失败: fileName={}", fileName, e);
            throw new RuntimeException("Excel导入失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建标题样式
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }

    /**
     * 获取单元格值作为字符串
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}
