package io.github.jelenajjovanoski.releasetracker;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(
        info = @Info(
                title = "Release Tracker API",
                version = "v1",
                description = "REST API for release tracking and management",
                contact = @Contact(name = "Jelena Jovanoski", email = "jelenajjovanoski@gmail.com")
        )
)
public class ReleaseTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReleaseTrackerApplication.class, args);
	}

}
