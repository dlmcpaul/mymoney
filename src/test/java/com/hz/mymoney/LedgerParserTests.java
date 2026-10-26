package com.hz.mymoney;

import com.hz.mymoney.data.models.Money;
import com.hz.mymoney.data.models.ledger.IPosting;
import com.hz.mymoney.data.models.ledger.Ledger;
import com.hz.mymoney.data.models.ledger.LedgerEntry;
import com.hz.mymoney.data.utilities.LedgerParser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LedgerParserTests {

	@Test
	public void SharePostParseStandardTests() {
		LedgerParser ledgerParser = new LedgerParser();
		String line = "  a:b:c  100  COL @ $12.00";

		var result = ledgerParser.parseSharePosting(line);
		assertEquals("a:b:c", result.getAccount());
		assertEquals("COL", result.getCode());
		assertEquals(new Money("100.00").getAmount(), result.getAmount().getAmount());
		assertEquals(new Money("12.00").getAmount(), result.getPrice().getAmount());
		assertEquals(new Money("1200.00").getAmount(), result.getValue().getAmount());
		assertEquals("$", result.getPrice().getCurrencySymbol());
		assertNull(result.getNote());

		String output = result.toString();
		assertEquals(line, output);
	}

	@Test
	public void SharePostParseMalformedTests() {
		LedgerParser ledgerParser = new LedgerParser();

		var singleSpaceResult = ledgerParser.parseSharePosting("  a:b:c  100 COL @ $12.00");
		assertEquals("a:b:c", singleSpaceResult.getAccount());
		assertEquals("COL", singleSpaceResult.getCode());
		assertEquals(new Money("100.00").getAmount(), singleSpaceResult.getAmount().getAmount());
		assertEquals(new Money("12.00").getAmount(), singleSpaceResult.getPrice().getAmount());
		assertEquals(new Money("1200.00").getAmount(), singleSpaceResult.getValue().getAmount());
		assertEquals("$", singleSpaceResult.getPrice().getCurrencySymbol());
		assertNull(singleSpaceResult.getNote());

		String output1 = singleSpaceResult.toString();
		assertEquals("  a:b:c  100  COL @ $12.00", output1);

		var usDollarResult = ledgerParser.parseSharePosting("  a:b:c  100 COL @ $12.00 USD");
		assertEquals("USD", usDollarResult.getPrice().getCurrency());
		assertEquals("$", usDollarResult.getPrice().getCurrencySymbol());

		String output2 = usDollarResult.toString();
		assertEquals("  a:b:c  100  COL @ $12.00 USD", output2);

		var nonDollarResult = ledgerParser.parseSharePosting("  a:b:c  100 COL @ 12.00 INR");
		assertEquals("INR", nonDollarResult.getPrice().getCurrency());

		String output3 = nonDollarResult.toString();
		assertEquals("  a:b:c  100  COL @ 12.00 INR", output3);
	}

	@Test
	void LedgerParserBasicTests() {
		Ledger ledger = parseLedgerString("""
2013/03/14 Capital Losses from MEF
  Assets:Fund:Fat Prophets Fund  -$5629.95
  Equity:Investment:Fund:Capital Losses  $5629.95
2005/04/18 Opening Balance for MEF
  Assets:Fund:Fat Prophets Fund  $10000.00
  Assets:Cash:StGeorge Savings  -$10000.00
""");
		assertEquals(2, ledger.getEntries().size());
	}

	@Test
	void LedgerParserCommentInTransactionTest() {
		Ledger ledger = parseLedgerString("""
2017-02-20 Monthly contribution from Simon Michael (Bronze)
  ; id:fe0ead37, group:197d11a4, dc:CREDIT, payment-service:STRIPE, payment-type:CREDITCARD
  income:contributions:Simon Michael           -10.00 USD
  expenses:fees:processor:STRIPE                   0.59 USD
  expenses:fees:host:Open Source Collective        1.00 USD
  assets:opencollective:hledger                    8.41 USD = 16.82 USD
""");
		assertEquals(1, ledger.getEntries().size());
	}

	@Test
	void accountNamesMayContainSingleSpaces() {
		LedgerEntry e = single("""
                    2024/01/01 T
                        Expenses:Food and Drink  $5
                        Assets:Checking Account
                    """);
		assertEquals("Expenses:Food and Drink", e.getFirstPosting().getAccount());
		assertEquals("Assets:Checking Account", e.getLastPosting().getAccount());
	}

	@Test
	void twoSpacesSeparateAccountFromAmount() {
		IPosting p = single("2024/01/01 T\n    Assets:Cash  $10\n    Income:Gift\n").getFirstPosting();
		assertEquals("Assets:Cash", p.getAccount());
		assertAmount("10", "$", p.getAmount());
	}

	@Test
	void accountNamesMayBeUnicode() {
		LedgerEntry e = single("""
                    2024/01/01 T
                        Dépenses:Café  €3
                        Actifs:Espèces
                    """);
		assertEquals("Dépenses:Café", e.getFirstPosting().getAccount());
		assertAmount("3", "€", e.getFirstPosting().getAmount());
	}

	@Test
	void extraWhitespaceAroundAmountIsIgnored() {
		IPosting p = single("2024/01/01 T\n    Assets:Cash          $10   \n    Income:Gift\n").getFirstPosting();
		assertAmount("10", "$", p.getAmount());
	}

	@Test
	void amountsAreNotAlignedToAnyColumn() {
		LedgerEntry e = single("""
                    2024/01/01 T
                        Assets:Cash  $10
                        Income:Gift                                              $-10
                    """);
		assertAmount("-10", "$", e.getLastPosting().getAmount());
	}

	@Test
	void indentationMayBeTwoSpacesFourSpacesOrATab() {
		for (String indent : new String[] {"  ", "    ", "\t"}) {
			LedgerEntry e = single("2024/01/01 T\n" + indent + "A  $1\n" + indent + "B\n");
			assertEquals(2, e.getPostings().size(), () -> "indent: [" + indent + "]");
		}
	}

	@Test
	void keepsPostingOrder() {
		LedgerEntry e = single("""
                    2024/01/01 T
                        Expenses:A  $1
                        Expenses:B  $2
                        Expenses:C  $3
                        Assets:Cash
                    """);
		assertEquals(
				java.util.List.of("Expenses:A", "Expenses:B", "Expenses:C", "Assets:Cash"),
				e.getPostings().stream().map(IPosting::getAccount).toList());
	}

	@Test
	void atMultipliesThePriceByTheQuantity() {
		LedgerEntry e = single("""
                    2024/01/01 Buy stock
                        Assets:Brokerage  10 AAPL @ $150
                        Assets:Cash       $-1500
                    """);
		IPosting p = e.getFirstPosting();
		assertAmount("10", "", p.getAmount());
		assertAmount("150", "$", p.getPrice());
		assertAmount("1500", "$", p.getValue());
	}

	@Test
	void fractionalQuantitiesAreMultipliedExactly() {
		LedgerEntry e = single("""
                    2024/01/01 Buy bitcoin
                        Assets:Crypto  1.5 BTC @ $20,000.50
                        Assets:Cash    $-30000.75
                    """);
		assertAmount("30000.750", "$", e.getFirstPosting().getValue());
	}

	private LedgerEntry single(String text) {
		return parseLedgerString(text).getOnlyLedgerEntries().getFirst();
	}

	private Ledger parseLedgerString(String ledgerString) {
		return new LedgerParser().loadLedger(new ByteArrayInputStream(ledgerString.getBytes(StandardCharsets.UTF_8)));
	}

	/** Compares quantities numerically so that scale (1.0 vs 1.00) does not matter. */
	private void assertQuantity(String expected, BigDecimal actual) {
		assertEquals(0, new BigDecimal(expected).compareTo(actual),
				() -> "expected " + expected + " but was " + actual.toPlainString());
	}

	private void assertAmount(String expectedQuantity, String expectedCurrencySymbol, Money actual) {
		assertQuantity(expectedQuantity, actual.getAmount());
		assertEquals(expectedCurrencySymbol, actual.getCurrencySymbol());
	}
}
