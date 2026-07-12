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
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@Order(1)
@Log4j2
@RequiredArgsConstructor
public class SharePriceServices implements ApplicationRunner {
	private static final String PTA_DATE_FORMAT_1 = "yyyy/MM/dd";
	private static final String PTA_DATE_FORMAT_2 = "yyyy-MM-dd";
	private static final String QUICKEN_DATE_FORMAT = "d/M/yy";

	private String commodityOption;
	private String commodityFileName;
	private boolean loadSuccess;

	private final ResourceLoader resourceLoader;

	@Getter
	private InvestmentHistory investmentHistory;

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
		Map<String, List<InvestmentHistoryEntry>> investmentHistoryEntries = new HashMap<>();
		loadSuccess = true;
		switch (commodityOption) {
			case "internal" -> loadCommoditiesFromArgs("classpath:test.commodities", investmentHistoryEntries);
			case "quicken" -> loadCommoditiesFromQuickenFile(commodityFileName, investmentHistoryEntries);
			case "commodities" -> loadCommoditiesFromArgs(commodityFileName, investmentHistoryEntries);
			default -> throw new IllegalStateException("Unexpected value: " + commodityOption);
		}
		if (loadSuccess) {
			log.info("Loaded {} commodities", investmentHistoryEntries.size());
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
						addEntry(entries, tokens.get(0), parseQuickenDate(tokens.get(2)), parseBigDecimal(tokens.get(1)));

						line = reader.readLine();
					}
					investmentHistory = new InvestmentHistory(entries);
				}
			} catch (RuntimeException e) {
				log.error(e.getMessage());
				loadSuccess = false;
			} finally {
				log.info("Quicken Commodities {} from {}", loadSuccess ? "loaded successfully" : "failed to load", fileName);
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
					throw new ValidationException("Unable to load Commodities from " + fileName);
				}
			}
		} catch (RuntimeException e) {
			log.error(e.getMessage());
			loadSuccess = false;
		} finally {
			log.info("Commodities {} from {}", loadSuccess ? "loaded successfully" : "failed to load", fileName);
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
					addEntry(entries, tokens.get(2), parseDate(tokens.get(1)), parseBigDecimal(tokens.get(3)));
				}
				line = reader.readLine();
			}
			investmentHistory = new InvestmentHistory(entries);
		} catch (IOException e) {
			log.error(e.getMessage());
			throw new ValidationException(e.getMessage());
		}
	}

	private LocalDate parseQuickenDate(String dateString) {
		return LocalDate.parse(dateString, DateTimeFormatter.ofPattern(QUICKEN_DATE_FORMAT));
	}

	private LocalDate parseDate(String dateString) {
		try {
			if (dateString.contains("/")) {
				return LocalDate.parse(dateString, DateTimeFormatter.ofPattern(PTA_DATE_FORMAT_1));
			}
			return LocalDate.parse(dateString, DateTimeFormatter.ofPattern(PTA_DATE_FORMAT_2));
		} catch (DateTimeParseException e) {
			throw new RuntimeException("Could not parse date " + dateString + " as either " + PTA_DATE_FORMAT_1 + " or " + PTA_DATE_FORMAT_2, e);
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
