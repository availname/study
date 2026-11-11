package com.bigcenter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 开发者成长平台（端口 8085）：学习体系总装厂，各功能包按 java-core 教学篇目对号入座 */
@SpringBootApplication
@MapperScan("com.bigcenter.**.mapper")
public class BigCenterApplication {

    public static void main(String[] args) {
        SpringApplication.run(BigCenterApplication.class, args);
    }
}
