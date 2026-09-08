package com.hz.mymoney;

import com.hz.mymoney.data.utilities.Dates;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DateTests {

	@Test
	void QuickenDateTest() {
		LocalDate expected = LocalDate.of(2021, 6, 20);
		assertEquals(expected, Dates.parseQuickenDate("20/6/21"));
		assertEquals(expected, Dates.parseQuickenDate("20/06/21"));
	}

	@Test
	void LedgerDateTest() {
		LocalDate expected = LocalDate.of(2021, 6, 20);
		assertEquals(expected, Dates.parseDate("2021/06/20"));
		assertEquals(expected, Dates.parseDate("2021-06-20"));
		assertEquals(expected, Dates.parseDate("2021-6-20"));
		assertEquals(LocalDate.of(2021, 6, 7), Dates.parseDate("2021-6-7"));
	}
}
