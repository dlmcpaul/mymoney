package com.hz.mymoney.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("mymoney.files")
@Data
public class FileConfigurationProperties {
	private final String ledger;
	private final String priceHistoryType;
	private final String priceHistory;
	private final String schedules;
}
