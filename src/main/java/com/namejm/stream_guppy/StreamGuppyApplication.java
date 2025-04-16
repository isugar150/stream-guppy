package com.namejm.stream_guppy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StreamGuppyApplication {

	public static void main(String[] args) {
		SpringApplication.run(StreamGuppyApplication.class, args);
	}

}
