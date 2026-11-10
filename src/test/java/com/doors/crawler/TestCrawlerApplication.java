package com.doors.crawler;

import org.springframework.boot.SpringApplication;

public class TestCrawlerApplication {

	public static void main(String[] args) {
		SpringApplication.from(CrawlerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
