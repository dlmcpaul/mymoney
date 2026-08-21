package com.hz.mymoney.data.services;

import com.hz.mymoney.data.models.internal.InvestmentHistory;
import com.hz.mymoney.data.models.internal.InvestmentHistoryEntry;
import com.hz.mymoney.data.utilities.Dates;
import com.hz.mymoney.data.utilities.Money;
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
import java.nio.file.attribute.FileTime;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;

@Service
@Order(1)
@Log4j2
public class SharePriceServices implements ApplicationRunner {

	private static final Pattern whitespace = Pattern.compile("\\s+");
	private static final Pattern comma_delimited = Pattern.compile(", ");

	private String commodityOption;
	private String commodityFileName;
	private boolean loadSuccess;
	private FileTime lastModified;

	private final ResourceLoader resourceLoader;

	@Getter
	private InvestmentHistory investmentHistory;

	public SharePriceServices(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	public BigDecimal getInvestmentValue(String code, LocalDate asAt) {
		return investmentHistory.getInvestmentValue(code, asAt);
	}

	public BigDecimal getPreviousInvestmentValue(String code, LocalDate asAt) {
		return investmentHistory.getPreviousInvestmentValue(code, asAt);
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
			lastModified = null;
			reloadCommodities();
		}
	}

	// If internal commodity map then allow updates from journal load
	public void updateFromJournal(LocalDate effective, String code, BigDecimal price) {
		if (commodityOption.equals("internal")) {
			log.info("Adding investment history entry {} {} {}", effective, code, price);
			addEntry(investmentHistory.commodityMap(), code, effective, price);
		}
	}

	public void reloadCommodities() throws IOException {
		loadSuccess = false;

		switch (commodityOption) {
			case "internal" -> {
				// Don't reload test data
				if (investmentHistory == null || investmentHistory.commodityMap().isEmpty()) { loadCommoditiesFromArgs(); };
			}
			case "commodities" -> loadCommoditiesFromArgs();
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
		Path path = Path.of(commodityFileName);

		if (Files.isReadable(path)) {
			if (lastModified == null || lastModified.compareTo(Files.getLastModifiedTime(path)) != 0) {
				lastModified = Files.getLastModifiedTime(path);
				Map<String, SortedSet<InvestmentHistoryEntry>> entries = new HashMap<>();
				investmentHistory = new InvestmentHistory(entries);
				try {
					try (final InputStream ledgerImportStream = Files.newInputStream(path)) {
						BufferedReader reader = new BufferedReader(new InputStreamReader(ledgerImportStream));
						String line = reader.readLine();
						while (line != null) {
							List<String> tokens = Arrays.stream(comma_delimited.split(line)).toList();
							addEntry(entries, tokens.get(0), Dates.parseQuickenDate(tokens.get(2)), parseBigDecimal(tokens.get(1)));

							line = reader.readLine();
						}
					}
					loadSuccess = true;
				} catch (RuntimeException e) {
					log.error(e.getMessage(), e);
				}
			}
		} else {
			log.error("Unable to load Quicken Commodities from file {}", commodityFileName);
		}
	}

	// Ledger Commodities file format
	private void loadCommoditiesFromArgs() throws IOException {
		Map<String, SortedSet<InvestmentHistoryEntry>> entries = new HashMap<>();
		try {
			if (commodityFileName.startsWith("classpath:")) {
				investmentHistory = new InvestmentHistory(entries);
				loadCommodities(resourceLoader.getResource(commodityFileName).getInputStream(), entries);
			} else {
				Path path = Path.of(commodityFileName);

				if (Files.isReadable(path)) {
					if (lastModified == null || lastModified.compareTo(Files.getLastModifiedTime(path)) != 0) {
						lastModified = Files.getLastModifiedTime(path);

						investmentHistory = new InvestmentHistory(entries);
						loadCommodities(Files.newInputStream(path), entries);
					}
				} else {
					throw new ValidationException("Unable to load Commodities from " + commodityFileName);
				}
			}
		} catch (ValidationException e) {
			log.error(e.getMessage(), e);
		}
	}

	private void loadCommodities(InputStream inputStream, Map<String, SortedSet<InvestmentHistoryEntry>> entries) {
		String line = "";
		try (final InputStream ledgerImportStream = inputStream) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(ledgerImportStream));
			line = reader.readLine();
			while (line != null) {
				// Parse as P 2025-01-01 NSC 150,25 USD
				// P date commodity value currency
				if (line.startsWith("P")) {
					String[] tokens = whitespace.split(line);
					addEntry(entries, tokens[2], Dates.parseDate(tokens[1]), parseBigDecimal(tokens[3]));
				}
				line = reader.readLine();
			}
		} catch (RuntimeException | IOException e) {
			log.error(e.getMessage(), e);
			throw new ValidationException("Failed to parse line '" + line + "'");
		}
		loadSuccess = true;
	}

	private BigDecimal parseBigDecimal(String value) {
		if (value.startsWith(Money.MONEY_SYMBOL)) {
			return Money.parseMoney(value, 2);
		}
		return new BigDecimal(value);
	}

	private void addEntry(Map<String, SortedSet<InvestmentHistoryEntry>> entries, String commodityCode, LocalDate asAt, BigDecimal value) {
		entries.computeIfAbsent(commodityCode, k -> new TreeSet<>());
		entries.get(commodityCode).add(new InvestmentHistoryEntry(asAt, value));
	}

}
