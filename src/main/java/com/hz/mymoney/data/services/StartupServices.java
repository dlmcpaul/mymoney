package com.hz.mymoney.data.services;

import com.hz.mymoney.configuration.FileConfigurationProperties;
import com.hz.mymoney.data.models.ledger.LedgerEntry;
import com.hz.mymoney.data.models.ledger.PriceEntry;
import com.hz.mymoney.data.models.ledger.SharePosting;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Objects;

import static org.springframework.core.io.ResourceLoader.CLASSPATH_URL_PREFIX;

@Service
@RequiredArgsConstructor
@Log4j2
public class StartupServices implements ApplicationRunner {
	private final LedgerServices ledgerServices;
	private final SchedulesServices schedulesServices;
	private final PriceHistoryServices shareValueService;
	private final FileConfigurationProperties fileConfigurationProperties;

	@Override
	public void run(@NonNull ApplicationArguments args) throws Exception {
		String ledgerFileName;
		if (args.containsOption("ledger")) {
			ledgerFileName = Objects.requireNonNull(args.getOptionValues("ledger")).getFirst();
		} else {
			ledgerFileName = Objects.requireNonNullElse(fileConfigurationProperties.getLedger(), CLASSPATH_URL_PREFIX + "test.ledger");
		}

		String schedulesFileName;
		if (args.containsOption("schedules")) {
			schedulesFileName = Objects.requireNonNull(args.getOptionValues("schedules")).getFirst();
		} else {
			schedulesFileName = Objects.requireNonNullElse(fileConfigurationProperties.getSchedules(), CLASSPATH_URL_PREFIX + "test-schedules.ledger");
		}

		String commodityOption;
		String commodityFileName;

		if (args.containsOption("commodities") || args.containsOption("quicken")) {
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
		} else {
			if (fileConfigurationProperties.getPriceHistoryType() == null || fileConfigurationProperties.getPriceHistory() == null) {
				commodityOption = "internal";
				commodityFileName = "classpath:test.commodities";
			} else {
				commodityOption = fileConfigurationProperties.getPriceHistoryType();
				commodityFileName = fileConfigurationProperties.getPriceHistory();
			}
		}

		shareValueService.startUp(commodityOption, commodityFileName);
		schedulesServices.startUp(schedulesFileName);
		ledgerServices.startUp(ledgerFileName);
		loadFiles();
	}

	public void loadFiles() throws IOException {
		log.info("Loading files");
		schedulesServices.reloadSchedules();
		ledgerServices.reloadLedger();
		this.loadCommodities();
	}

	public void loadCommodities() throws IOException {
		shareValueService.reloadCommodities();

		long total = ledgerServices.getLedger()
				.getOnlyLedgerEntries()
				.stream()
				.filter(this::updateShareValues)
				.count();

		total += ledgerServices.getLedger()
				.getOnlyPriceEntries()
				.stream()
				.filter(this::updateShareValues)
				.count();

		log.info("Updated {} Share Prices from Ledger", total);
	}

	private boolean updateShareValues(PriceEntry priceEntry) {
		shareValueService.updateFromJournal(priceEntry.line());
		return true;
	}

	private boolean updateShareValues(LedgerEntry ledgerEntry) {
		SharePosting posting = ledgerEntry.getSharePosting();
		if (posting != null) {
			shareValueService.updateFromJournal(ledgerEntry.getDate(), posting.getCode(), posting.getPrice());
			return true;
		}
		return false;
	}

}
