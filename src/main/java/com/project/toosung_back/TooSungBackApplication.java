package com.project.toosung_back;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TooSungBackApplication {

    public static void main(String[] args) {
        SpringApplication.run(TooSungBackApplication.class, args);
    }

}
