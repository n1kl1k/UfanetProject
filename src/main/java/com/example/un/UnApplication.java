package com.example.un;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UnApplication {

	public static void main(String[] args) {
		SpringApplication.run(UnApplication.class, args);
	}
}