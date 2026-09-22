package com.vai.cygnus_assesment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CygnusAssesmentApplication {

	public static void main(String[] args) {
		SpringApplication.run(CygnusAssesmentApplication.class, args);
	}

}
