package com.example.uvmate;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.uvmate.mapper")
public class UvmateApplication {

    public static void main(String[] args) {
        SpringApplication.run(UvmateApplication.class, args);
    }
}