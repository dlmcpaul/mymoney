package com.hz.mymoney;

import com.hz.mymoney.data.models.coa.Schedule;
import com.hz.mymoney.data.models.ledger.IPosting;
import com.hz.mymoney.data.models.ledger.Ledger;
import com.hz.mymoney.data.models.ledger.LedgerEntry;
import com.hz.mymoney.data.models.ledger.Posting;
import com.hz.mymoney.data.services.LedgerServices;
import com.hz.mymoney.data.services.SchedulesServices;
import com.hz.mymoney.data.services.SharePriceServices;
import com.hz.mymoney.ui.services.FileUpdateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regression tests for review item 1.1: posting a schedule must add an independent
 * copy of the schedule's postings to the ledger.
 */
class PostScheduleTests {
	private static final String EXPENSE = "Expenses:Utilities:Electricity";
	private static final String BANK = "Assets:Bank:Everyday";

	private Ledger ledger;
	private FileUpdateService fileUpdateService;

	@BeforeEach
	void setUp() {
		LedgerServices ledgerServices = mock(LedgerServices.class);
		SchedulesServices schedulesServices = mock(SchedulesServices.class);
		SharePriceServices sharePriceServices = mock(SharePriceServices.class);

		ledger = new Ledger();
		when(ledgerServices.getLedger()).thenReturn(ledger);

		fileUpdateService = new FileUpdateService(ledgerServices, schedulesServices, sharePriceServices);
	}

	private static Schedule electricitySchedule() {
		List<IPosting> postings = new ArrayList<>();
		postings.add(new Posting(EXPENSE, new BigDecimal("100.00")));
		postings.add(new Posting(BANK, new BigDecimal("-100.00")));
		return new Schedule("1m", new LedgerEntry(LocalDate.of(2026, 1, 15), "Electricity bill", postings));
	}

	private static List<BigDecimal> amounts(String debit, String credit) {
		return List.of(new BigDecimal(debit), new BigDecimal(credit));
	}

	private static BigDecimal amountFor(LedgerEntry entry, String account) {
		return entry.getPostings().stream()
				.filter(p -> p.isSameAccount(account))
				.findFirst()
				.orElseThrow()
				.getAmount()
				.getAmount();
	}

	@Test
	void postedEntryDoesNotShareThePostingListWithTheSchedule() {
		Schedule schedule = electricitySchedule();

		fileUpdateService.postSchedule(schedule, amounts("120.00", "-120.00"));

		LedgerEntry posted = ledger.getOnlyLedgerEntries().getFirst();
		assertNotSame(schedule.ledgerEntry.getPostings(), posted.getPostings(),
				"Ledger entry must not reuse the schedule's posting list");
	}

	@Test
	void postedEntryDoesNotShareThePostingObjectsWithTheSchedule() {
		Schedule schedule = electricitySchedule();

		fileUpdateService.postSchedule(schedule, amounts("120.00", "-120.00"));

		LedgerEntry posted = ledger.getOnlyLedgerEntries().getFirst();
		for (IPosting postedPosting : posted.getPostings()) {
			for (IPosting templatePosting : schedule.ledgerEntry.getPostings()) {
				assertNotSame(templatePosting, postedPosting,
						"Ledger posting for " + postedPosting.getAccount() + " is the schedule's own Posting instance");
			}
		}
	}

	@Test
	void postingTheScheduleAgainDoesNotRewriteEarlierLedgerEntries() {
		Schedule schedule = electricitySchedule();

		fileUpdateService.postSchedule(schedule, amounts("120.00", "-120.00"));
		fileUpdateService.postSchedule(schedule, amounts("250.00", "-250.00"));

		List<LedgerEntry> entries = ledger.getOnlyLedgerEntries();
		assertEquals(2, entries.size());

		LedgerEntry first = entries.get(0);
		assertEquals(0, new BigDecimal("120.00").compareTo(amountFor(first, EXPENSE)),
				"First posted entry was changed by the second post");
		assertEquals(0, new BigDecimal("-120.00").compareTo(amountFor(first, BANK)),
				"First posted entry was changed by the second post");

		LedgerEntry second = entries.get(1);
		assertEquals(0, new BigDecimal("250.00").compareTo(amountFor(second, EXPENSE)));
		assertEquals(0, new BigDecimal("-250.00").compareTo(amountFor(second, BANK)));
	}

	@Test
	void postedEntryUsesTheOverrideAmounts() {
		Schedule schedule = electricitySchedule();

		fileUpdateService.postSchedule(schedule, amounts("87.65", "-87.65"));

		LedgerEntry posted = ledger.getOnlyLedgerEntries().getFirst();
		assertEquals(0, new BigDecimal("87.65").compareTo(amountFor(posted, EXPENSE)));
		assertEquals(0, new BigDecimal("-87.65").compareTo(amountFor(posted, BANK)));
		assertTrue(posted.isBalanced());
	}

	@Test
	void overridesDoNotChangeTheScheduleTemplateAmounts() {
		Schedule schedule = electricitySchedule();

		fileUpdateService.postSchedule(schedule, amounts("120.00", "-120.00"));

		assertEquals(0, new BigDecimal("100.00").compareTo(amountFor(schedule.ledgerEntry, EXPENSE)),
				"Schedule template amount was overwritten by the one-off override");
		assertEquals(0, new BigDecimal("-100.00").compareTo(amountFor(schedule.ledgerEntry, BANK)),
				"Schedule template amount was overwritten by the one-off override");
	}
}
