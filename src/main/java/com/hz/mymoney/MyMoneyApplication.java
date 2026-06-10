package com.hz.mymoney;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@RequiredArgsConstructor
public class MyMoneyApplication {

	public static void main(String[] args) {
		SpringApplication.run(MyMoneyApplication.class, args);
	}
}
