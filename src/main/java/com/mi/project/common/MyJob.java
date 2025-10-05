package com.mi.project.common;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.io.File;
import java.util.Objects;

@Slf4j
public class MyJob extends QuartzJobBean {
    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        log.info("Job Started: 清理tmp目录内容");
        File tmpDir = new File("C:\\Users\\31591\\Desktop\\project\\src\\main\\resources\\las");
        clearDirectory(tmpDir);
        tmpDir = new File("C:\\Users\\31591\\Desktop\\project\\src\\main\\resources\\json");
        clearDirectory(tmpDir);
    }

    // 只清空目录内容，不删除目录本身
    private void clearDirectory(File dir) {
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectoryRecursively(file);
                    } else {
                        boolean deleted = file.delete();
                        log.info("删除文件: {}，结果: {}", file.getAbsolutePath(), deleted);
                    }
                }
            }
        }
    }

    // 递归删除目录及其内容（包括目录本身）
    private void deleteDirectoryRecursively(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectoryRecursively(file);
                } else {
                    boolean deleted = file.delete();
                    log.info("删除文件: {}，结果: {}", file.getAbsolutePath(), deleted);
                }
            }
        }
        boolean deleted = dir.delete();
        log.info("删除目录: {}，结果: {}", dir.getAbsolutePath(), deleted);
    }
}
