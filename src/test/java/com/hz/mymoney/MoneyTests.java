package com.hz.mymoney;

import com.hz.mymoney.data.utilities.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MoneyTests {
	@Test
	void testSymbol() {
		assertEquals("$", Money.MONEY_SYMBOL);
	}

	@Test
	void positiveMoneyTest() {
		BigDecimal expected = new BigDecimal("201.00");
		assertEquals(expected, Money.parseMoney("$201.00", 2));
		assertEquals(expected, Money.parseMoney("201.00", 2));
		assertEquals(expected, Money.parseMoney(" $201.00", 2));
		assertEquals(expected, Money.parseMoney("$ 201.00", 2));
		assertEquals(expected, Money.parseMoney("$201", 2));
	}

	@Test
	void negativeMoneyTest() {
		BigDecimal expected = new BigDecimal("-201.00");
		assertEquals(expected, Money.parseMoney("-$201.00", 2));
		assertEquals(expected, Money.parseMoney("-201.00", 2));
		assertEquals(expected, Money.parseMoney(" $-201.00", 2));
		assertEquals(expected, Money.parseMoney("$ -201.00", 2));
		assertEquals(expected, Money.parseMoney("-$201", 2));
	}
}
