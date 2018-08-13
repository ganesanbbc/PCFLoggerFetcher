package com.pcf.readlog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BootReadingPcfLogsFinalApplication {

	public static void main(String[] args) {
		SpringApplication.run(BootReadingPcfLogsFinalApplication.class, args);
	}
	
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(BootReadingPcfLogsFinalApplication.class);
	}
}
