package com.hz.mymoney.data.services;

import com.hz.mymoney.data.models.internal.Schedule;
import com.hz.mymoney.data.models.ledger.IPosting;
import com.hz.mymoney.data.models.ledger.Ledger;
import com.hz.mymoney.data.models.ledger.LedgerEntry;
import com.hz.mymoney.data.models.ledger.Posting;
import com.hz.mymoney.data.utilities.LedgerParser;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.core.io.ResourceLoader.CLASSPATH_URL_PREFIX;

@Service
@Log4j2
@RequiredArgsConstructor
public class SchedulesServices implements ApplicationRunner {
	private final ResourceLoader resourceLoader;
	private String schedulesFileName;

	@Getter
	private List<Schedule> scheduledTransactions;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		if (args.containsOption("schedules")) {
			loadSchedulesFromArgs(Objects.requireNonNull(args.getOptionValues("schedules")).getFirst());
		} else {
			this.loadSchedulesFromClassPath("test-schedules.ledger");
		}
	}

	private void loadSchedulesFromArgs(String fileName) throws IOException {
		LedgerParser ledgerParser = new LedgerParser();

		Path path = Path.of(fileName);

		if (Files.isReadable(path)) {
			schedulesFileName = path.toString();
			log.info("Loading schedules from file {}", path.toString());
			Ledger scheduleLedger = ledgerParser.loadLedger(Files.newInputStream(path));
			convertLedgerToSchedules(scheduleLedger);
		} else {
			log.error("Unable to load schedules from file {}", fileName);
		}
	}

	private void loadSchedulesFromClassPath(String fileName) throws IOException {
		LedgerParser ledgerParser = new LedgerParser();

		schedulesFileName = CLASSPATH_URL_PREFIX + fileName;
		Resource resource = resourceLoader.getResource(schedulesFileName);
		log.info("Loading schedules from classpath:{}", fileName);
		Ledger scheduleLedger = ledgerParser.loadLedger(resource.getInputStream());
		convertLedgerToSchedules(scheduleLedger);
	}

	private void convertLedgerToSchedules(Ledger scheduleLedger) {
		// Convert to List<Schedule>
		this.scheduledTransactions = scheduleLedger.getLedgerEntries()
				.stream()
				.map(ledgerEntry -> createSchedule(ledgerEntry.getNote(), ledgerEntry))
				.sorted()
				.collect(Collectors.toCollection(ArrayList::new));  // Cannot use toList as it produces an immutable collection
	}

	private Schedule createSchedule(String recurrence, LedgerEntry ledgerEntry) {
		return new Schedule(recurrence, ledgerEntry);
	}

	private Schedule createSchedule(String recurrence, String description, LocalDate date, BigDecimal amount, String debit, String credit) {
		Posting debitAccount = new Posting(debit, amount.multiply(BigDecimal.valueOf(-1)));
		Posting creditAccount = new Posting(credit, amount);
		LedgerEntry ledgerEntry = new LedgerEntry(date, description, List.of(debitAccount, creditAccount));
		return new Schedule(recurrence, ledgerEntry);
	}

	private Schedule createSchedule(String recurrence, String description, LocalDate date, BigDecimal amount, String debit, IPosting... credits) {
		List<IPosting> postings = new ArrayList<>();
		postings.add(new Posting(debit, amount.multiply(BigDecimal.valueOf(-1))));
		postings.addAll(Arrays.stream(credits).toList());
		LedgerEntry ledgerEntry = new LedgerEntry(date, description, postings);
		return new Schedule(recurrence, ledgerEntry);
	}

	public Optional<Schedule> findSchedule(String scheduleDescription) {
		return scheduledTransactions.stream().filter(schedule -> schedule.ledgerEntry.getDescription().equals(scheduleDescription)).findFirst();
	}

	public void scheduleRollForward(Schedule schedule) {
		schedule.rollForward();
		// Replace in sorted position
		scheduledTransactions.remove(schedule);
		scheduledTransactions.add(schedule);
	}

	public void saveSchedules() {
		if (schedulesFileName == null || schedulesFileName.startsWith(CLASSPATH_URL_PREFIX)) {
			log.error("Cannot Save Schedules");
		} else {
			Path path = Path.of(schedulesFileName);
			log.info("Saving Schedules to file {}", path);
			try (var out = new BufferedWriter(new FileWriter(path.toFile()))) {
				for (Schedule entry : scheduledTransactions) {
					out.write(entry.ledgerEntry.toString());
				}
			} catch (IOException e) {
				log.error("Failed to Save Schedules", e);
			}
		}
	}

	public void reloadSchedules() throws IOException {
		if (schedulesFileName.contains(CLASSPATH_URL_PREFIX)) {
			this.loadSchedulesFromClassPath(schedulesFileName.replace(CLASSPATH_URL_PREFIX, ""));
		} else {
			this.loadSchedulesFromArgs(schedulesFileName);
		}
	}
}
