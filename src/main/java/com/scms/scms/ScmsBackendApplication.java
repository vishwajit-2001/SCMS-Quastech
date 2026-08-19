package com.scms.scms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ScmsBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(ScmsBackendApplication.class, args);
    }
}
