package com.mi.project.config;

import com.mi.project.common.MyJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {
    @Bean
    public JobDetail cleanTmpFilesJobDetail() {
        return JobBuilder.newJob(MyJob.class)
                .withIdentity("cleanTmpFilesJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger cleanTmpFilesTrigger() {
        // 每天凌晨3点
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule("0 0 3 * * ?");
        return TriggerBuilder.newTrigger()
                .forJob(cleanTmpFilesJobDetail())
                .withIdentity("cleanTmpFilesTrigger")
                .withSchedule(scheduleBuilder)
                .build();
    }
}
