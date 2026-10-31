package com.hz.mymoney;

import com.hz.mymoney.data.models.Money;
import com.hz.mymoney.data.models.ledger.IPosting;
import com.hz.mymoney.data.models.ledger.LedgerEntry;
import com.hz.mymoney.data.models.ledger.Posting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for review item 1.2: Money should be an immutable value type
 * whose equality follows equals(Object)/hashCode and ignores BigDecimal scale.
 */
class MoneyValueSemanticsTests {

	// --- Immutability ---------------------------------------------------------

	@Test
	void setScaleDoesNotMutateTheReceiver() {
		Money original = new Money("1.005");

		Money scaled = original.setScale(2, RoundingMode.HALF_UP);

		assertEquals(new BigDecimal("1.005"), original.amount(), "setScale changed the original Money");
		assertEquals(new BigDecimal("1.01"), scaled.amount());
	}

	@Test
	void setScaleReturnsANewInstance() {
		Money original = new Money("10.5");

		assertNotSame(original, original.setScale(2, RoundingMode.HALF_UP));
	}

	@Test
	void postingConstructorDoesNotMutateTheCallersMoney() {
		Money callerAmount = new Money("12.345");

		new Posting("Expenses:Food", callerAmount);

		assertEquals(new BigDecimal("12.345"), callerAmount.amount(), "Posting(String, Money) rescaled the argument in place");
	}

	@Test
	void postingNoteConstructorDoesNotMutateTheCallersMoney() {
		Money callerAmount = new Money("12.345");

		new Posting("Expenses:Food", callerAmount, "note");

		assertEquals(new BigDecimal("12.345"), callerAmount.amount());
	}

	@Test
	void postingSetAmountDoesNotMutateTheCallersMoney() {
		Money callerAmount = new Money("7.777");
		Posting posting = new Posting("Expenses:Food");

		posting.setAmount(callerAmount);

		assertEquals(new BigDecimal("7.777"), callerAmount.amount());
	}

	@Test
	void moneyHasNoPublicAmountSetter() {
		assertThrows(NoSuchMethodException.class, () -> Money.class.getMethod("setAmount", BigDecimal.class),
				"Money exposes setAmount, so shared constants like Money.ZERO can be mutated");
	}

	// --- equals(Object) / hashCode contract ----------------------------------

	@Test
	void equalsOverridesObjectEquals() throws NoSuchMethodException {
		assertEquals(Money.class, Money.class.getMethod("equals", Object.class).getDeclaringClass(),
				"Money only overloads equals(Money); equals(Object) is still Object's identity check");
	}

	@Test
	void equalValuesAreEqualWhenTypedAsObject() {
		Object a = new Money("10.00");
		Object b = new Money("10.00");

		assertEquals(a, b);
		assertTrue(Objects.equals(a, b));
	}

	@Test
	void equalValuesHaveEqualHashCodes() {
		Money a = new Money("10.00");
		Money b = new Money("10.00");

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	void listContainsFindsAnEqualValue() {
		List<Money> amounts = List.of(new Money("5.00"), new Money("10.00"));

		assertTrue(amounts.contains(new Money("10.00")));
	}

	@Test
	void hashSetDeduplicatesEqualValues() {
		Set<Money> amounts = new HashSet<>();
		amounts.add(new Money("10.00"));
		amounts.add(new Money("10.00"));

		assertEquals(1, amounts.size());
	}

	@Test
	void equalValuesWorkAsMapKeys() {
		Map<Money, String> byAmount = new HashMap<>();
		byAmount.put(new Money("42.00"), "answer");

		assertEquals("answer", byAmount.get(new Money("42.00")));
	}

	@Test
	void notEqualToNullOrOtherTypes() {
		Money money = new Money("1.00");

		assertNotEquals(null, money);
		assertNotEquals(new BigDecimal("1.00"), money);
	}

	@Test
	void differentCurrenciesAreNotEqual() {
		assertNotEquals(new Money(new BigDecimal("10.00"), Money.AUSTRALIAN), new Money(new BigDecimal("10.00"), Money.USA));
	}

	// --- Scale-insensitive equality -------------------------------------------

	@ParameterizedTest
	@CsvSource({
			"1, 1.00",
			"1.0, 1.00",
			"100, 100.000",
			"-25.5, -25.50",
			"0, 0.00"
	})
	void equalityIgnoresScale(String left, String right) {
		Money a = new Money(left);
		Money b = new Money(right);

		assertEquals(a, b, left + " and " + right + " should be the same amount");
		assertEquals(a.hashCode(), b.hashCode(), "hashCode must agree with scale-insensitive equals");
	}

	@Test
	void equalityIsConsistentWithCompareTo() {
		Money a = new Money("3.10");
		Money b = new Money("3.1");

		assertEquals(0, a.compareTo(b));
		assertEquals(a, b);
	}

	@Test
	void postingLookupByAmountIgnoresTypedPrecision() {
		// Posting(String, BigDecimal) keeps the scale the user typed, as the parser does.
		List<IPosting> postings = List.of(
				new Posting("Expenses:Rent", new BigDecimal("500.0")),
				new Posting("Assets:Bank", new BigDecimal("-500")));
		LedgerEntry entry = new LedgerEntry(LocalDate.of(2026, 1, 1), "Rent", postings);

		IPosting found = entry.getPostingWithAmount(new Money("500.00"));

		assertNotNull(found, "Lookup failed only because 500.0 and 500.00 have different scales");
		assertEquals("Expenses:Rent", found.getAccount());
	}

	@Test
	void negatedAmountMatchesRegardlessOfScale() {
		// Mirrors LedgerServices.findMatching: p.getAmount().equals(other.getAmount().negate())
		Money debit = new Money("75.5");
		Money credit = new Money("-75.50");

		assertEquals(debit, credit.negate());
	}
}
