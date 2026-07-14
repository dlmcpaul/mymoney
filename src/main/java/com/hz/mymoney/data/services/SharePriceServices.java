package com.hz.mymoney.data.services;

import com.hz.mymoney.data.models.internal.InvestmentHistory;
import com.hz.mymoney.data.models.internal.InvestmentHistoryEntry;
import com.hz.mymoney.data.utilities.Dates;
import com.hz.mymoney.exceptions.ValidationException;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

@Service
@Order(1)
@Log4j2
public class SharePriceServices implements ApplicationRunner {

	private String commodityOption;
	private String commodityFileName;
	private boolean loadSuccess;

	private final ResourceLoader resourceLoader;

	@Getter
	private InvestmentHistory investmentHistory;

	public SharePriceServices(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void run(@NonNull ApplicationArguments args) throws Exception {
		if (args.containsOption("commodities")) {
			commodityOption = "commodities";
			commodityFileName = Objects.requireNonNull(args.getOptionValues("commodities")).getFirst();
		} else if (args.containsOption("quicken")) {
			commodityOption = "quicken";
			commodityFileName = Objects.requireNonNull(args.getOptionValues("quicken")).getFirst();
		} else {
			commodityOption = "internal";
			commodityFileName = "classpath:test.commodities";
		}

		if (commodityOption.isEmpty()) {
			log.error("No commodities option provided");
		} else {
			reloadCommodities();
		}
	}

	public void reloadCommodities() throws IOException {

		loadSuccess = true;

		switch (commodityOption) {
			case "internal", "commodities" -> loadCommoditiesFromArgs();
			case "quicken" -> loadCommoditiesFromQuickenFile();
			default -> throw new IllegalStateException("Unexpected value: " + commodityOption);
		}
		if (loadSuccess) {
			switch (commodityOption) {
				case "internal" -> log.info("Successfully loaded {} Commodities from TEST file {}", investmentHistory.commodityMap().size(), commodityFileName);
				case "quicken" -> log.info("Successfully loaded {} Commodities from Quicken file {}", investmentHistory.commodityMap().size(), commodityFileName);
				case "commodities" -> log.info("Successfully loaded {} Commodities from PTA file {}", investmentHistory.commodityMap().size(), commodityFileName);
			}
		}
	}

	// Exported Quicken commodities file format
	// Basic CSV with format of Code, Value, date (dd/MM/yy)
	// VDHG, 74, 16/4/26
	private void loadCommoditiesFromQuickenFile() throws IOException {
		Map<String, List<InvestmentHistoryEntry>> entries = new HashMap<>();
		investmentHistory = new InvestmentHistory(entries);

		Path path = Path.of(commodityFileName);

		if (Files.isReadable(path)) {
			try {
				try (final InputStream ledgerImportStream = Files.newInputStream(path)) {
					BufferedReader reader = new BufferedReader(new InputStreamReader(ledgerImportStream));
					String line = reader.readLine();
					while (line != null) {
						List<String> tokens = Arrays.stream(line.split(", ")).toList();
						addEntry(entries, tokens.get(0), Dates.parseQuickenDate(tokens.get(2)), parseBigDecimal(tokens.get(1)));

						line = reader.readLine();
					}
				}
			} catch (RuntimeException e) {
				log.error(e.getMessage());
				loadSuccess = false;
			}
		} else {
			log.error("Unable to load Quicken Commodities from file {}", commodityFileName);
			loadSuccess = false;
		}
	}

	// Ledger Commodities file format
	private void loadCommoditiesFromArgs() throws IOException {
		Map<String, List<InvestmentHistoryEntry>> entries = new HashMap<>();
		investmentHistory = new InvestmentHistory(entries);
		try {
			if (commodityFileName.startsWith("classpath:")) {
				loadCommodities(resourceLoader.getResource(commodityFileName).getInputStream(), entries);
			} else {
				Path path = Path.of(commodityFileName);

				if (Files.isReadable(path)) {
					loadCommodities(Files.newInputStream(path), entries);
				} else {
					throw new ValidationException("Unable to load Commodities from " + commodityFileName);
				}
			}
		} catch (RuntimeException e) {
			log.error(e.getMessage());
			loadSuccess = false;
		}
	}

	private void loadCommodities(InputStream inputStream, Map<String, List<InvestmentHistoryEntry>> entries) {
		try (final InputStream ledgerImportStream = inputStream) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(ledgerImportStream));
			String line = reader.readLine();
			while (line != null) {
				// Parse as P 2025-01-01 NSC 150,25 USD
				// P date commodity value currency
				if (line.startsWith("P")) {
					List<String> tokens = Arrays.stream(line.split(" ")).toList();
					addEntry(entries, tokens.get(2), Dates.parseDate(tokens.get(1)), parseBigDecimal(tokens.get(3)));
				}
				line = reader.readLine();
			}
		} catch (IOException e) {
			log.error(e.getMessage());
			throw new ValidationException(e.getMessage());
		}
	}

	private BigDecimal parseBigDecimal(String value) {
		return BigDecimal.valueOf(Double.parseDouble(value));
	}

	private void addEntry(Map<String, List<InvestmentHistoryEntry>> entries, String commodityCode, LocalDate asAt, BigDecimal value) {
		InvestmentHistoryEntry entry = new InvestmentHistoryEntry(asAt, value);
		entries.computeIfAbsent(commodityCode, k -> new ArrayList<>());
		entries.get(commodityCode).add(entry);
	}

}
