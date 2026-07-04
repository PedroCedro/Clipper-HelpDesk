package br.com.infocedro.clipper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ClipperApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClipperApplication.class, args);
	}

}
