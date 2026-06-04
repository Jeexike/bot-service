package com.example.orderproxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@SpringBootApplication
public class OrderProxyApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderProxyApplication.class, args);
	}

}
