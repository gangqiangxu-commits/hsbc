package com.hsbc.iwpb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan("com.hsbc.iwpb.mapper")
public class AccountBalanceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AccountBalanceApplication.class, args);
    }
}