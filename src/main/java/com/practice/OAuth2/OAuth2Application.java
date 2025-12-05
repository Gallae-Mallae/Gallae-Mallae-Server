package com.practice.OAuth2;

import com.practice.OAuth2.global.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@EnableConfigurationProperties(AppProperties.class)
public class OAuth2Application {

	public static void main(String[] args) {
		SpringApplication.run(OAuth2Application.class, args);
	}

}
