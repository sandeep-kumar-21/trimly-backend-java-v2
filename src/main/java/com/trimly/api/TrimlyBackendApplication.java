package com.trimly.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.TimeZone;

@SpringBootApplication
@EnableAsync
@org.springframework.data.jpa.repository.config.EnableJpaAuditing
public class TrimlyBackendApplication {

	static {
		System.setProperty("user.timezone", "Asia/Kolkata");
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
	}

	public static void main(String[] args) {
		System.setProperty("user.timezone", "Asia/Kolkata");
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
		SpringApplication.run(TrimlyBackendApplication.class, args);
	}

}
