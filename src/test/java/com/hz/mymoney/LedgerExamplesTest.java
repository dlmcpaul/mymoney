package com.hz.mymoney;

import com.hz.mymoney.data.models.Money;
import com.hz.mymoney.data.models.ledger.Ledger;
import com.hz.mymoney.data.models.ledger.LedgerEntry;
import com.hz.mymoney.data.models.ledger.SharePosting;
import com.hz.mymoney.data.utilities.LedgerParser;
import com.hz.mymoney.data.utilities.MoneyParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Ledger Format Tests")
public class LedgerExamplesTest {

	@Test
	void basicExample() {
		String basic = """
				; a comment
				
				2016/1/1 set opening balance
				   assets:checking       $500.00
				   equity:opening balances
				
				2016/1/5 farmer's market
				   expenses:groceries     $50.00
				   Assets:checking
				""";
		Ledger ledger1 = parseLedgerString(basic);
		assertEquals(5, ledger1.getEntries().size());
		assertEquals(2, ledger1.getOnlyLedgerEntries().size());
		assertEquals(new Money("-500.00").amount(), ledger1.getOnlyLedgerEntries().getFirst().getLastPosting().getValue().amount());
		assertEquals(new Money(new BigDecimal("-50.00"), "", "$"), ledger1.getOnlyLedgerEntries().getLast().getLastPosting().getValue());
	}

	@Test
	void oneThirdExample() {
		Money totalCost = MoneyParser.parseMoney("$1000.00");
		BigDecimal totalShares = new BigDecimal("3000");

		SharePosting sharePosting = new SharePosting("a:b:c", totalShares, "CODE", totalCost, "");
		assertEquals(totalCost, sharePosting.getValue());
	}

	@ParameterizedTest(name = "[{index}] a line starting with ''{0}'' is a comment")
	@ValueSource(strings = {";", "#", "%", "|", "*"})
	void commentCharactersAtColumnZeroAreIgnored(String marker) {
		Ledger ledger = parseLedgerString(marker + " this is not a transaction 2024/01/01\n");
		assertTrue(ledger.getOnlyLedgerEntries().isEmpty());
	}

	@Test
	void commentsBetweenTransactionsAreIgnored() {
		Ledger ledger = parseLedgerString("""
                    ; Header comment
                    2024/01/01 One
                        A  $1
                        B
                    # separator
                    2024/01/02 Two
                        A  $1
                        B
                    """);
		// 4 entries, 2 comments and 2 ledger entries
		assertEquals(4, ledger.getEntries().size());
		assertEquals(2, ledger.getOnlyLedgerEntries().size());
	}

	@Test
	void headerCommentBecomesTheTransactionNote() {
		assertEquals("weekly shop", single("""
                    2024/01/01 Store ; weekly shop
                        A  $1
                        B
                    """).getNote());
	}

	@Test
	void noteIsNullWhenThereIsNoComment() {
		assertNull(single("2024/01/01 Store\n    A  $1\n    B\n").getNote());
	}

	@Test
	void commentAfterAPostingAmountIsThePostingNote() {
		LedgerEntry le = single("""
                    2024/01/01 Store
                        Expenses:Food  $10  ; burger
                        Assets:Cash
                    """);
		assertEquals("burger", le.getFirstPosting().getNote());
		assertNull(le.getLastPosting().getNote());
		assertNull(le.getNote());
	}

	@Test
	void commentAfterAnAccountWithoutAmountIsThePostingNote() {
		LedgerEntry le = single("""
                    2024/01/01 Store
                        Expenses:Food  $10
                        Assets:Cash  ; paid by card
                    """);
		assertEquals("paid by card", le.getLastPosting().getNote());
	}

	@Test
	void commentTextDoesNotAffectAmountParsing() {
		LedgerEntry le = single("""
                    2024/01/01 Store
                        Expenses:Food  $10 ; costs $999 @ nothing = $0
                        Assets:Cash
                    """);
		assertEquals("$10.00", le.getFirstPosting().getAmount().toString());
		assertEquals(new Money("10", "", "$"), le.getFirstPosting().getValue());
	}

// Not working
/*	@Test
	void missingAmountAtStartExample() {
		String basic = """
				; a comment
				
				2016/1/1 set opening balance
				   assets:checking
				   equity:opening balances $500.00
				
				2016/1/5 farmer's market
				   expenses:groceries
				   Assets:checking     $50.00
				""";
		Ledger ledger1 = parseLedgerString(basic);
		assertEquals(5, ledger1.getEntries().size());
		assertEquals(2, ledger1.getOnlyLedgerEntries().size());
		assertEquals(new Money("-500.00").amount(), ledger1.getOnlyLedgerEntries().getFirst().getFirstPosting().getValue().amount());
		assertEquals(new Money(new BigDecimal("-50.00"), "", "$"), ledger1.getOnlyLedgerEntries().getLast().getFirstPosting().getValue());
	}*/

	@Test
	void commentsNonIndentedExample() {
		String comment = """
				; comment
				# comment
				% comment
				| comment
				* comment and org-mode outline node
				""";
		Ledger ledger1 = parseLedgerString(comment);
		assertEquals(5, ledger1.getEntries().size());
	}

	@Test
	void basicLedgerExamples() {
		String full = """
		1990/10/19 description
		   a:b:c    100
		   b:c:d    -100
		""";
		Ledger ledger1 = parseLedgerString(full);
		assertEquals(1, ledger1.getEntries().size());
		assertEquals(new Money("-100", ""), ledger1.getOnlyLedgerEntries().getFirst().getLastPosting().getAmount());
		assertEquals(new Money("100", ""), ledger1.getOnlyLedgerEntries().getFirst().getFirstPosting().getAmount());

		String optional = """
		1990/10/19 description
		   a:b:c    100
		   b:c:d
		""";
		Ledger ledger2 = parseLedgerString(optional);
		assertEquals(1, ledger2.getEntries().size());
		assertEquals(new Money("-100", ""), ledger2.getOnlyLedgerEntries().getFirst().getLastPosting().getAmount());
		assertEquals(new Money("100", ""), ledger2.getOnlyLedgerEntries().getFirst().getFirstPosting().getAmount());
	}

	@Test
	void commodityExamples() {
		String commodity = """
		1990/10/19 description
		    a:b:c  10
			a:b:d  $10.00
			a:b:e  1,000.0001 USD
			e:f:g  -10
			e:f:g  -$10.00
			e:f:h
		""";
//			a:b:f  10 "prepaid classes" ; Don't yet support quoted or lower case Strings as commodity type

		Ledger ledger = parseLedgerString(commodity);
		assertEquals(1, ledger.getEntries().size());
	}

	private LedgerEntry single(String text) {
		return parseLedgerString(text).getOnlyLedgerEntries().getFirst();
	}

	private Ledger parseLedgerString(String ledgerString) {
		return new LedgerParser().loadLedger(new ByteArrayInputStream(ledgerString.getBytes(StandardCharsets.UTF_8)));
	}

}
