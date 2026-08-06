package com.tuckshop.pos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PosApplication {
    public static void main(String[] args) {
        SpringApplication.run(PosApplication.class, args);
        System.out.println("=================================================");
        System.out.println(" Tuck Shop POS is running.");
        System.out.println(" On this computer, open: http://localhost:8080");
        System.out.println(" On phone (same WiFi), open: http://<this-pc-ip>:8080");
        System.out.println("=================================================");
    }
}
