package com.hz.mymoney;

import com.hz.mymoney.data.models.Money;
import com.hz.mymoney.data.utilities.LedgerParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class LedgerParserTests {

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
		assertEquals("AUD", result.getPrice().getCurrency());
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
		assertEquals("AUD", singleSpaceResult.getPrice().getCurrency());
		assertNull(singleSpaceResult.getNote());

		String output1 = singleSpaceResult.toString();
		assertEquals("  a:b:c  100  COL @ $12.00", output1);

		var usDollarResult = ledgerParser.parseSharePosting("  a:b:c  100 COL @ $12.00 USD");
		assertEquals("USD", usDollarResult.getPrice().getCurrency());

		String output2 = usDollarResult.toString();
		assertEquals("  a:b:c  100  COL @ $12.00 USD", output2);

		var nonDollarResult = ledgerParser.parseSharePosting("  a:b:c  100 COL @ 12.00 INR");
		assertEquals("INR", nonDollarResult.getPrice().getCurrency());

		String output3 = nonDollarResult.toString();
		assertEquals("  a:b:c  100  COL @ 12.00 INR", output3);
	}
}
