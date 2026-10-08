package dev.barboza.pulso;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PulsoApplication {

    public static void main(String[] args) {
        SpringApplication.run(PulsoApplication.class, args);
    }
}
