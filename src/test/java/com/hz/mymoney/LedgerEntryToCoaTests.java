package com.hz.mymoney;

import com.hz.mymoney.data.models.coa.ChartOfAccounts;
import com.hz.mymoney.data.models.ledger.IPosting;
import com.hz.mymoney.data.models.ledger.LedgerEntry;
import com.hz.mymoney.data.models.ledger.Posting;
import com.hz.mymoney.data.services.LedgerServices;
import com.hz.mymoney.data.utilities.LedgerEntrySupport;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LedgerEntryToCoaTests {

	private static final BigDecimal AMOUNT = new BigDecimal("100.00");
	private static final BigDecimal HALF_AMOUNT = new BigDecimal("50.00");

	// Basic 2 entry ledger
	@Test
	void testSimpleLedgerEntryToCoa() {
		ChartOfAccounts coa = new ChartOfAccounts(LocalDate.now());
		LedgerServices ledgerServices = new LedgerServices(null, null);

		Posting fromAccount = new Posting("fromAccount", AMOUNT);
		Posting sourceAccount = new Posting("toAccount", AMOUNT.negate());

		LedgerEntry simpleLedgerEntry = new LedgerEntry(LocalDate.now(), "Empty", List.of(fromAccount, sourceAccount));

		ledgerServices.mapLedgerEntryToPostings(coa, simpleLedgerEntry);

		assertEquals(2, coa.totalAccounts());

		assertTrue(coa.findAccount("toAccount").isPresent());
		assertEquals(1, coa.findAccount("toAccount").get().getMovements().size());
		assertEquals("fromAccount", coa.findAccount("toAccount").get().getMovements().element().sourceAccount());

		assertTrue(coa.findAccount("fromAccount").isPresent());
		assertEquals(1, coa.findAccount("fromAccount").get().getMovements().size());
		assertEquals("toAccount", coa.findAccount("fromAccount").get().getMovements().element().sourceAccount());
	}

	// Ledger Entry with only one negative amount but more than 1 positive amount
	@Test
	void testOneNegativeLedgerEntryToCoa() {
		ChartOfAccounts coa = new ChartOfAccounts(LocalDate.now());
		LedgerServices ledgerServices = new LedgerServices(null, null);

		Posting account1 = new Posting("fromAccount1", HALF_AMOUNT);
		Posting account2 = new Posting("fromAccount2", HALF_AMOUNT);
		Posting sourceAccount = new Posting("sourceAccount", AMOUNT.negate());

		LedgerEntry simpleNegativeLedgerEntry = new LedgerEntry(LocalDate.now(), "Empty", List.of(account1, account2, sourceAccount));

		ledgerServices.mapLedgerEntryToPostings(coa, simpleNegativeLedgerEntry);

		assertEquals(3, coa.totalAccounts());

		assertTrue(coa.findAccount("sourceAccount").isPresent());
		assertEquals(1, coa.findAccount("sourceAccount").get().getMovements().size());

		assertTrue(coa.findAccount("fromAccount1").isPresent());
		assertEquals(1, coa.findAccount("fromAccount1").get().getMovements().size());
		assertEquals("sourceAccount", coa.findAccount("fromAccount1").get().getMovements().element().sourceAccount());

		assertTrue(coa.findAccount("fromAccount2").isPresent());
		assertEquals(1, coa.findAccount("fromAccount2").get().getMovements().size());
		assertEquals("sourceAccount", coa.findAccount("fromAccount2").get().getMovements().element().sourceAccount());
	}

	// Ledger Entry with only one positive amount but more than 1 negative amount (Unmatched sources)
	// Currently not handled
	@Test
	void testOnePositiveLedgerEntryToCoa() {
		ChartOfAccounts coa = new ChartOfAccounts(LocalDate.now());
		LedgerServices ledgerServices = new LedgerServices(null, null);

		Posting account1 = new Posting("fromAccount1", HALF_AMOUNT.negate());
		Posting account2 = new Posting("fromAccount2", HALF_AMOUNT.negate());
		Posting sourceAccount = new Posting("sourceAccount", AMOUNT);

		LedgerEntry simplePositiveLedgerEntry = new LedgerEntry(LocalDate.now(), "Empty", List.of(sourceAccount, account1, account2));

		ledgerServices.mapLedgerEntryToPostings(coa, simplePositiveLedgerEntry);

		assertEquals(3, coa.totalAccounts());

		assertTrue(coa.findAccount("sourceAccount").isPresent());
		assertEquals(1, coa.findAccount("sourceAccount").get().getMovements().size());

		assertTrue(coa.findAccount("fromAccount1").isPresent());
		assertEquals(1, coa.findAccount("fromAccount1").get().getMovements().size());
		assertEquals("sourceAccount", coa.findAccount("fromAccount1").get().getMovements().element().sourceAccount());

		assertTrue(coa.findAccount("fromAccount2").isPresent());
		assertEquals(1, coa.findAccount("fromAccount2").get().getMovements().size());
		assertEquals("sourceAccount", coa.findAccount("fromAccount2").get().getMovements().element().sourceAccount());
	}

	// Ledger Entry with multiple negative and positive amounts but the amounts can be matched
	@Test
	void testComplexLedgerEntryToCoa() {
		ChartOfAccounts coa = new ChartOfAccounts(LocalDate.now());
		LedgerServices ledgerServices = new LedgerServices(null, null);

		Posting account1 = new Posting("fromAccount1", HALF_AMOUNT);
		Posting account2 = new Posting("fromAccount2", HALF_AMOUNT);
		Posting sourceAccount1 = new Posting("sourceAccount1", HALF_AMOUNT.negate());
		Posting sourceAccount2 = new Posting("sourceAccount2", HALF_AMOUNT.negate());

		LedgerEntry complexBalancedLedgerEntry = new LedgerEntry(LocalDate.now(), "Empty", List.of(sourceAccount1, sourceAccount2, account2, account1));

		assertTrue(complexBalancedLedgerEntry.canMatchPostings());

		List<IPosting> sortedPostings = LedgerEntrySupport.sortByAmount(complexBalancedLedgerEntry.getPostings());
		List<IPosting> negativeAmounts = LedgerEntrySupport.splitAndReturnFirst(sortedPostings);
		List<IPosting> positiveAmounts = LedgerEntrySupport.splitAndReturnLast(sortedPostings);

		assertEquals(2, negativeAmounts.size());
		assertEquals(HALF_AMOUNT.negate(), negativeAmounts.getFirst().getAmount().getAmount());
		assertEquals(HALF_AMOUNT.negate(), negativeAmounts.get(1).getAmount().getAmount());

		assertEquals(2, positiveAmounts.size());
		assertEquals(HALF_AMOUNT, positiveAmounts.getFirst().getAmount().getAmount());
		assertEquals(HALF_AMOUNT, positiveAmounts.get(1).getAmount().getAmount());

		ledgerServices.mapLedgerEntryToPostings(coa, complexBalancedLedgerEntry);

		assertEquals(4, coa.totalAccounts());

		assertTrue(coa.findAccount("sourceAccount1").isPresent());
		assertEquals(1, coa.findAccount("sourceAccount1").get().getMovements().size());

		assertTrue(coa.findAccount("sourceAccount2").isPresent());
		assertEquals(1, coa.findAccount("sourceAccount2").get().getMovements().size());

		assertTrue(coa.findAccount("fromAccount1").isPresent());
		assertEquals(1, coa.findAccount("fromAccount1").get().getMovements().size());
		assertEquals("sourceAccount1", coa.findAccount("fromAccount1").get().getMovements().element().sourceAccount());

		assertTrue(coa.findAccount("fromAccount2").isPresent());
		assertEquals(1, coa.findAccount("fromAccount2").get().getMovements().size());
		assertEquals("sourceAccount2", coa.findAccount("fromAccount2").get().getMovements().element().sourceAccount());
	}

	@Test
	void testUnbalancedLedgerEntryToCoa() {
		/*
		Assets:Cash:NAB Savings  $682.50
        Assets:Tax Credit:Imputation Credit  $264.64  ;Imputation Credit from TLS
        Income:Investment:Dividends:Franked  -$617.50
        Income:Investment::Imputation:Tax Credit  -$264.64  ;Imputation Credit from TLS
        Income:Investment:Dividends:Unfranked  -$65.00
        */

		ChartOfAccounts coa = new ChartOfAccounts(LocalDate.now());
		LedgerServices ledgerServices = new LedgerServices(null, null);

		Posting account1 = new Posting("fromAccount1", new BigDecimal("682.50"));
		Posting account2 = new Posting("fromAccount2", new BigDecimal("264.64"));
		Posting sourceAccount1 = new Posting("sourceAccount1", new BigDecimal("-617.50"));
		Posting sourceAccount2 = new Posting("sourceAccount2", new BigDecimal("-264.64"));
		Posting sourceAccount3 = new Posting("sourceAccount3", new BigDecimal("-65.00"));

		LedgerEntry unbalancedLedgerEntry = new LedgerEntry(LocalDate.now(), "Empty", List.of(account1, account2, sourceAccount1, sourceAccount2, sourceAccount3));

		ledgerServices.mapLedgerEntryToPostings(coa, unbalancedLedgerEntry);
		assertEquals(5, coa.totalAccounts());
	}

}
