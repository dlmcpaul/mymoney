package com.hz.mymoney;

import com.hz.mymoney.configuration.FileConfigurationProperties;
import com.hz.mymoney.configuration.ReleaseProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@RequiredArgsConstructor
@EnableConfigurationProperties({ReleaseProperties.class, FileConfigurationProperties.class})
public class MyMoneyApplication {

	public static void main(String[] args) {
		SpringApplication.run(MyMoneyApplication.class, args);
	}
}
