package com.hz.mymoney.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;

@PropertySource(value = "classpath:/release.properties", ignoreResourceNotFound = true)
@ConfigurationProperties("release")
@Data
public class ReleaseProperties {
	private final String version;

	public ReleaseProperties(String version) {
		this.version = version;
	}
}
