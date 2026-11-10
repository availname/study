package com.example.mqmall.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 开启定时任务：发送补偿、订单扫表兜底、幂等记录清理 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
