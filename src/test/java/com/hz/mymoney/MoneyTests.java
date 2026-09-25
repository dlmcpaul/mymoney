package com.hz.mymoney;

import com.hz.mymoney.data.utilities.MoneyParser;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MoneyTests {
	@Test
	void testSymbol() {
		assertEquals("$", MoneyParser.MONEY_SYMBOL);
	}

	@Test
	void positiveMoneyTest() {
		BigDecimal expected = new BigDecimal("201.00");
		assertEquals(expected, MoneyParser.parseMoney("$201.00", 2).getAmount());
		assertEquals(expected, MoneyParser.parseMoney("201.00", 2).getAmount());
		assertEquals(expected, MoneyParser.parseMoney(" $201.00", 2).getAmount());
		assertEquals(expected, MoneyParser.parseMoney("$ 201.00", 2).getAmount());
		assertEquals(expected, MoneyParser.parseMoney("$201", 2).getAmount());
	}

	@Test
	void negativeMoneyTest() {
		BigDecimal expected = new BigDecimal("-201.00");
		assertEquals(expected, MoneyParser.parseMoney("-$201.00", 2).getAmount());
		assertEquals(expected, MoneyParser.parseMoney("-201.00", 2).getAmount());
		assertEquals(expected, MoneyParser.parseMoney(" $-201.00", 2).getAmount());
		assertEquals(expected, MoneyParser.parseMoney("$ -201.00", 2).getAmount());
		assertEquals(expected, MoneyParser.parseMoney("-$201", 2).getAmount());
	}

	@Test
	void currencyTest() {
		assertEquals("AUD", MoneyParser.parseMoney("100.00 AUD", 2).getCurrency());
		assertEquals("AUD", MoneyParser.parseMoney("100.00", 2).getCurrency());
		assertEquals("USD", MoneyParser.parseMoney("100.00 USD", 2).getCurrency());
		assertEquals("INR", MoneyParser.parseMoney("100.00 INR", 2).getCurrency());
	}

	@Test
	void priceTest() {
		String expected = "$100.1234";
		assertEquals(expected, MoneyParser.parseMoney("$100.1234", 6).toString());
	}
}
