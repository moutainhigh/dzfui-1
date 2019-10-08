package com.dzf.zxkj.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @Auther: dandelion
 * @Date: 2019-09-05
 * @Description:
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@ComponentScan
public class ZxkjPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(ZxkjPlatformApplication.class, args);
    }
}
