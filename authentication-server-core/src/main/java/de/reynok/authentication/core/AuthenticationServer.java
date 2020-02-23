package de.reynok.authentication.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@EnableScheduling
public class AuthenticationServer {

    public static void main(String[] args) {
        SpringApplication.run(AuthenticationServer.class, args);
    }
}