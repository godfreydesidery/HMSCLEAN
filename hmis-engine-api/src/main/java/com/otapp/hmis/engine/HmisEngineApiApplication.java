package com.otapp.hmis.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

@SpringBootApplication
@Modulithic(systemName = "hmis-engine")
public class HmisEngineApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(HmisEngineApiApplication.class, args);
    }
}
