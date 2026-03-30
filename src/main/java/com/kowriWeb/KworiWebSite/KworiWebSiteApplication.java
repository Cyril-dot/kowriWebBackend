package com.kowriWeb.KworiWebSite;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KworiWebSiteApplication {

	public static void main(String[] args) {

		Dotenv dotenv = Dotenv.configure()
				.directory("./")       // looks for .env in the project root (next to pom.xml)
				.ignoreIfMissing()     // safe in CI/CD environments without .env
				.load();

		dotenv.entries().forEach(entry ->
				System.setProperty(entry.getKey(), entry.getValue())
		);

		SpringApplication.run(KworiWebSiteApplication.class, args);
	}

}
