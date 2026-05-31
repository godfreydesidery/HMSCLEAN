package com.otapp.hmis.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@Modulithic(systemName = "hmis-engine")
public class HmisEngineApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(HmisEngineApiApplication.class, args);
    }
}
