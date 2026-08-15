package com.github.haenaryn.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.github.haenaryn")
public class LeeCommerceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeeCommerceApplication.class, args);
    }
}
