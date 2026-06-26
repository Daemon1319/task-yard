package com.allan.task_yard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TaskYardApplication {

	public static void main(String[] args) {
		SpringApplication.run(TaskYardApplication.class, args);
	}

}
