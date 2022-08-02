package com.company.gamestorecatalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class GamestoreCatalogApplication {

	public static void main(String[] args) {
		SpringApplication.run(GamestoreCatalogApplication.class, args);
	}

}
