package com.invitely;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableKafka
@EnableAsync   // for async video generation jobs
public class InvitelyApplication {

    public static void main(String[] args) {
        SpringApplication.run(InvitelyApplication.class, args);
    }
}
