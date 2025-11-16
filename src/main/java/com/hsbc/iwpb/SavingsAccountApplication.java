package com.hsbc.iwpb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan("com.hsbc.iwpb.mapper")
public class SavingsAccountApplication {
    public static void main(String[] args) {
        SpringApplication.run(SavingsAccountApplication.class, args);
    }
}