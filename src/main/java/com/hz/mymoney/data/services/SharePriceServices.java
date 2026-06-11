package com.hz.mymoney.data.services;

import com.hz.mymoney.data.models.internal.InvestmentHistory;
import com.hz.mymoney.data.models.internal.InvestmentHistoryEntry;
import com.hz.mymoney.exceptions.ValidationException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
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
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Order(1)
@Log4j2
@RequiredArgsConstructor
public class SharePriceServices implements ApplicationRunner {
	private static final String PTA_DATE_FORMAT = "yyyy/MM/dd";
	private static final String QUICKEN_DATE_FORMAT = "d/M/yy";

	private String commodityOption;
	private String commodityFileName;

	private final ResourceLoader resourceLoader;

	@Getter
	private InvestmentHistory investmentHistory;

	@Override
	public void run(@NonNull ApplicationArguments args) throws Exception {
		Map<String, List<InvestmentHistoryEntry>> investmentHistoryEntries = new HashMap<>();
		if (args.containsOption("commodities")) {
			commodityOption = "commodities";
			loadCommoditiesFromArgs(Objects.requireNonNull(args.getOptionValues("commodities")).getFirst(), investmentHistoryEntries);
		} else if (args.containsOption("quicken")) {
			commodityOption = "quicken";
			loadCommoditiesFromQuickenFile(Objects.requireNonNull(args.getOptionValues("quicken")).getFirst(), investmentHistoryEntries);
		} else {
			commodityOption = "internal";
			loadCommoditiesFromArgs("classpath:test.commodities", investmentHistoryEntries);
		}
	}

	public void reloadCommodities() throws IOException {
		Map<String, List<InvestmentHistoryEntry>> investmentHistoryEntries = new HashMap<>();
		switch (commodityOption) {
			case "internal" -> loadCommoditiesFromArgs("classpath:test.commodities", investmentHistoryEntries);
			case "quicken" -> loadCommoditiesFromQuickenFile(commodityFileName, investmentHistoryEntries);
			case "commodities" -> loadCommoditiesFromArgs(commodityFileName, investmentHistoryEntries);
			default -> throw new IllegalStateException("Unexpected value: " + commodityOption);
		}
	}

	// Exported Quicken commodities file format
	// Basic CSV with format of Code, Value, date (dd/MM/yy)
	// VDHG, 74, 16/4/26
	private void loadCommoditiesFromQuickenFile(String fileName, Map<String, List<InvestmentHistoryEntry>> entries) throws IOException {
		commodityFileName = fileName;
		Path path = Path.of(fileName);

		if (Files.isReadable(path)) {
			try {
				try (final InputStream ledgerImportStream = Files.newInputStream(path)) {
					BufferedReader reader = new BufferedReader(new InputStreamReader(ledgerImportStream));
					String line = reader.readLine();
					while (line != null) {
						List<String> tokens = Arrays.stream(line.split(", ")).toList();
						addEntry(entries, tokens.get(0), parseDate(tokens.get(2), QUICKEN_DATE_FORMAT), parseBigDecimal(tokens.get(1)));

						line = reader.readLine();
					}
					investmentHistory = new InvestmentHistory(entries);
				}
			} finally {
				log.info("Quicken Commodities loaded successfully from file {}", fileName);
			}
		} else {
			log.error("Unable to load Quicken Commodities from file {}", fileName);
		}
	}

	// Ledger Commodities file format
	private void loadCommoditiesFromArgs(String fileName, Map<String, List<InvestmentHistoryEntry>> entries) throws IOException {
		commodityFileName = fileName;
		try {
			if (fileName.startsWith("classpath:")) {
				loadCommodities(resourceLoader.getResource(fileName).getInputStream(), entries);
			} else {
				Path path = Path.of(fileName);

				if (Files.isReadable(path)) {
					loadCommodities(Files.newInputStream(path), entries);
				} else {
					throw new ValidationException("Unable to load Commodities from file " + fileName);
				}
			}
		} catch (RuntimeException e) {
			log.error(e.getMessage());
		} finally {
			log.info("Commodities loaded successfully from file {}", fileName);
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
					addEntry(entries, tokens.get(2), parseDate(tokens.get(1), PTA_DATE_FORMAT), parseBigDecimal(tokens.get(3)));
				}
				line = reader.readLine();
			}
			investmentHistory = new InvestmentHistory(entries);
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}

	private LocalDate parseDate(String date, String format) {
		return LocalDate.parse(date, DateTimeFormatter.ofPattern(format));
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
