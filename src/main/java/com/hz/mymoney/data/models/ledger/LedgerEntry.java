package com.hz.mymoney.data.models.ledger;

import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalUnit;
import java.util.List;

@Data
public class LedgerEntry {
	private LocalDate date;
	private String status;
	private String description;
	private String note;
	private List<IPosting> postings;

	public LedgerEntry(LocalDate date, String status, String description, String note, List<IPosting> postings) {
		init(date, status, description, note, postings);
	}

	public LedgerEntry(LocalDate date, String description, List<IPosting> postings) {
		if (description == null) {
			init(date, null, "", null, postings);
		} else if (description.contains(";")) {
			init(date, null, description.substring(0, description.indexOf(";")).trim(), description.substring(description.indexOf(";") + 1).trim(), postings);
		} else {
			init(date, null, description, null, postings);
		}
	}

	public void init(LocalDate date, String status, String description, String note, List<IPosting> postings) {
		this.date = date;
		this.status = status;
		this.description = description;
		this.note = note;
		this.postings = postings;
	}

	public void changeDateBy(long amount, TemporalUnit unit) {
		this.date = this.date.plus(amount, unit);
	}

	// Basic check for balanced transaction (debits == credits)
	public boolean isBalanced() {
		BigDecimal total = postings.stream()
				.map(IPosting::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		// Financial calculations should always balance
		return total.compareTo(BigDecimal.ZERO) == 0;
	}

	public BigDecimal getRemainingBalance() {
		BigDecimal total = BigDecimal.ZERO;
		for (IPosting p : postings) {
			total = total.add(p.getValue());
		}
		return total.multiply(BigDecimal.valueOf(-1)).setScale(2, RoundingMode.HALF_UP);
	}

	public boolean hasCommissionPosting() {
		return postings.stream().anyMatch(IPosting::isCommissionPosting);
	}

	public BigDecimal getCommission() {
		if (hasCommissionPosting()) {
			return postings.stream().filter(IPosting::isCommissionPosting)
					.findFirst().orElseThrow().getValue();
		}
		return BigDecimal.ZERO;
	}

	public boolean isEquivalent(LedgerEntry transaction) {
		return this.date.isEqual(transaction.getDate())
				&& this.getFirstPosting().isSameAccount(transaction.getLastPosting().getAccount())
				//&& this.getLastPosting().isSameAccount(transaction.getFirstPosting().getAccount())
				//&& this.description.equals(transaction.getDescription())
				&& this.getFirstAmount().abs().compareTo(transaction.getFirstAmount().abs()) == 0;
	}

	public IPosting getNonSourcePosting(String account) {
		for (IPosting p : postings) {
			if (p.getAccount().equalsIgnoreCase(account) == false) {
				return p;
			}
		}

		return getFirstPosting();
	}

	public IPosting getFirstNegativePosting() {
		for (IPosting p : postings) {
			if (p.getAmount().compareTo(BigDecimal.ZERO) < 0) {
				return p;
			}
		}
		return null;
	}

	public IPosting getFirstPositivePosting() {
		for (IPosting p : postings) {
			if (p.getAmount().compareTo(BigDecimal.ZERO) > 0) {
				return p;
			}
		}
		return null;
	}

	public IPosting getFirstPosting() {
		return this.postings.getFirst();
	}

	public IPosting getLastPosting() {
		return this.postings.get(postings.size() - 1);
	}

	public BigDecimal getFirstAmount() {
		return postings.stream()
				.findFirst()
				.orElseGet(() -> new Posting("Empty")).getAmount();
	}

	public IPosting getPostingWithAmount(BigDecimal amount) {
		return postings.stream()
				.filter(t -> t.getAmount().equals(amount))
				.findFirst().orElse(null);
	}

	private String noteLine() {
		if (this.note != null) {
			return " ; " + this.note;
		}
		return "";
	}

	private String statusLine() {
		return this.status == null ? "" : " " + this.status;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(date.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")))
				.append(statusLine())
				.append(" ")
				.append(description)
				.append(this.noteLine())
				.append("\n");
		for (IPosting p : postings) {
			sb.append(p).append("\n");
		}
		return sb.toString();
	}
}
