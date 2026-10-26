package com.hz.mymoney;

import com.hz.mymoney.data.models.Money;
import com.hz.mymoney.data.utilities.MoneyParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoneyTests {
	@Test
	void testSymbol() {
		assertEquals("$", Money.DOLLAR_SYMBOL);
	}

	@Test
	void positiveMoneyTest() {
		BigDecimal expected = new BigDecimal("201.00");
		assertEquals(expected, MoneyParser.parseMoney("$201.00").getAmount());
		assertEquals(expected, MoneyParser.parseMoney("201.00").getAmount());
		assertEquals(expected, MoneyParser.parseMoney(" $201.00").getAmount());
		assertEquals(expected, MoneyParser.parseMoney("$ 201.00").getAmount());
		assertEquals(expected, MoneyParser.parseMoney("$201").getAmount());
	}

	@Test
	void negativeMoneyTest() {
		BigDecimal expected = new BigDecimal("-201.00");
		assertEquals(expected, MoneyParser.parseMoney("-$201.00").getAmount());
		assertEquals(expected, MoneyParser.parseMoney("-201.00").getAmount());
		assertEquals(expected, MoneyParser.parseMoney(" $-201.00").getAmount());
		assertEquals(expected, MoneyParser.parseMoney("$ -201.00").getAmount());
		assertEquals(expected, MoneyParser.parseMoney("-$201").getAmount());
	}

	@Test
	void currencyTest() {
		assertEquals("AUD", MoneyParser.parseMoney("100.00 AUD").getCurrency());
		assertEquals("", MoneyParser.parseMoney("100.00").getCurrency());
		assertEquals("USD", MoneyParser.parseMoney("100.00 USD").getCurrency());
		assertEquals("INR", MoneyParser.parseMoney("100.00 INR").getCurrency());
	}

	@Test
	void priceTest() {
		assertEquals("$100.1234", MoneyParser.parseMoney("$100.1234").toString());
	}

	@ParameterizedTest(name = "[{index}] \"{0}\" -> {1} {2}")
	@CsvSource(delimiter = '|', value = {
			"$10            | 10        | $     | true",
			"$1,000.50      | 1000.50   | $     | true",
			"-$5            | -5        | $     | true",
			"$-5            | -5        | $     | true",
			"+$3            | 3         | $     | true",
			"€ 5            | 5         | €     | true",
			"10 EUR         | 10        | EUR   | false",
			"-10 EUR        | -10       | EUR   | false",
			"10 AAPL        | 10        | AAPL  | false",
			"0.001 BTC      | 0.001     | BTC   | false",
			".5 EUR         | 0.5       | EUR   | false",
			"42             | 42        |       | false",
			"-42.25         | -42.25    |       | false",
			"1,000,000      | 1000000   |       | false",
	})
	void parsesQuantityAndCurrency(String text, String quantity, String currency, boolean before) {
		Money amount = MoneyParser.parseMoney(text);
		assertQuantity(quantity, amount.getAmount());
		if (before) {
			assertEquals(currency.charAt(0), amount.abs().toString().charAt(0));
		} else {
			assertEquals(currency == null ? "" : currency, amount.getCurrency());
		}
		//assertEquals(before, amount.commodityBeforeQuantity());
	}

	/** Compares quantities numerically so that scale (1.0 vs 1.00) does not matter. */
	static void assertQuantity(String expected, BigDecimal actual) {
		assertEquals(0, new BigDecimal(expected).compareTo(actual),
				() -> "expected " + expected + " but was " + actual.toPlainString());
	}
}
