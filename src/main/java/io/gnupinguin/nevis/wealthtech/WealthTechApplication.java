package io.gnupinguin.nevis.wealthtech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WealthTechApplication {

    static void main(String[] args) {
        SpringApplication.run(WealthTechApplication.class, args);
    }

}
