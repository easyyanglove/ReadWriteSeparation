package com.easyyang.dbtest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

/**
 * Created by jiangpy on 9:54.
 */
@SpringBootApplication
@ImportResource("classpath*:applicationContext.xml")
public class DbStartService {
    public static void main(String[] args) {
        SpringApplication.run(DbStartService.class, args);
    }
}
