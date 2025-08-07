package com.sage.bif;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class BifApplication {

	public static void main(String[] args) {
		SpringApplication.run(BifApplication.class, args);
	}

}
