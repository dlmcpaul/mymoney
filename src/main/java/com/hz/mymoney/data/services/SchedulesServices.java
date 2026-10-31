package com.hz.mymoney.data.services;

import com.hz.mymoney.data.models.coa.Schedule;
import com.hz.mymoney.data.models.ledger.Ledger;
import com.hz.mymoney.data.models.ledger.LedgerEntry;
import com.hz.mymoney.data.utilities.FileSupport;
import com.hz.mymoney.data.utilities.LedgerParser;
import com.hz.mymoney.exceptions.FileSaveFailedException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.core.io.ResourceLoader.CLASSPATH_URL_PREFIX;

@Service
@RequiredArgsConstructor
@Log4j2
public class SchedulesServices {
	private final ResourceLoader resourceLoader;
	private String schedulesFileName;

	@Getter
	private List<Schedule> scheduledTransactions;

	private Ledger scheduleLedger;

	public void startUp(String schedulesFileName) {
		this.schedulesFileName = schedulesFileName;
	}

	private void loadSchedulesFromArgs(String fileName) {
		LedgerParser ledgerParser = new LedgerParser();

		Path path = Path.of(fileName);

		if (Files.isReadable(path)) {
			try {
				scheduleLedger = ledgerParser.loadLedger(path);
				convertLedgerToSchedules(scheduleLedger);
			} finally {
				assert scheduleLedger != null : "scheduleLedger is null";
				log.info("Successfully loaded {} Schedules from file {} with {} errors", scheduleLedger.getEntries().size(), path.toString(), scheduleLedger.getLoadErrorCount());
			}
		} else {
			log.error("Unable to load schedules from file {}", fileName);
		}
	}

	private void loadSchedulesFromClassPath(String fileName) throws IOException {
		LedgerParser ledgerParser = new LedgerParser();

		try {
			Resource resource = resourceLoader.getResource(schedulesFileName);
			scheduleLedger = ledgerParser.loadLedger(resource.getInputStream());
			convertLedgerToSchedules(scheduleLedger);
		} finally {
			log.info("Demo schedules loaded successfully from classpath:{}", fileName);
		}
	}

	private void convertLedgerToSchedules(Ledger scheduleLedger) {
		// Convert to List<Schedule>
		this.scheduledTransactions = scheduleLedger.getOnlyLedgerEntries()
				.stream()
				.map(ledgerEntry -> createSchedule(ledgerEntry.getNote(), ledgerEntry))
				.sorted()
				.collect(Collectors.toCollection(ArrayList::new));  // Cannot use toList as it produces an immutable collection
	}

	private Schedule createSchedule(String recurrence, LedgerEntry ledgerEntry) {
		return new Schedule(recurrence, ledgerEntry);
	}

	public Optional<Schedule> findSchedule(String scheduleDescription) {
		return scheduledTransactions.stream().filter(schedule -> schedule.ledgerEntry.getDescription().equals(scheduleDescription)).findFirst();
	}

	public void scheduleRollForward(Schedule schedule) {
		schedule.rollForward();
		scheduledTransactions.sort(Comparator.comparing(o -> o.ledgerEntry.getDate()));
	}

	public void saveSchedules() {
		if (schedulesFileName == null) {
			log.error("Cannot Save Schedules as filename is missing");
			throw new FileSaveFailedException("Failed to Save Schedules due to missing filename");
		}

		if (schedulesFileName.startsWith(CLASSPATH_URL_PREFIX)) {
			log.error("Cannot Save Schedules as filename is internal");
			throw new FileSaveFailedException("Failed to Save Schedules file " + schedulesFileName);
		}

		if (scheduleLedger.isReadOnly()) {
			log.error("Cannot Save a Read Only Schedules");
			throw new FileSaveFailedException("Failed to Save Schedules as it is read-only");
		}

		FileSupport.writeFile(schedulesFileName, "Schedules", scheduledTransactions);
	}

	public void reloadSchedules() throws IOException {
		if (schedulesFileName.contains(CLASSPATH_URL_PREFIX)) {
			this.loadSchedulesFromClassPath(schedulesFileName);
		} else {
			this.loadSchedulesFromArgs(schedulesFileName);
		}
	}
}
