package com.example.spring_boot_event_generator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
@EnableScheduling
public class SpringBootEventGeneratorApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringBootEventGeneratorApplication.class, args);
	}

}
