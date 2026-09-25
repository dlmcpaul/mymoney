package com.hz.mymoney.data.utilities;

import com.hz.mymoney.data.models.ledger.IPosting;

import java.util.Comparator;
import java.util.List;

public final class LedgerEntrySupport {
	private LedgerEntrySupport() {}

	public static List<IPosting> sortByAmount(List<IPosting> postings) {
		return postings.stream().sorted(Comparator.comparing(IPosting::getAmount)).toList();
	}

	public static List<IPosting> splitAndReturnFirst(List<IPosting> postings) {
		return postings.subList(0, postings.size() / 2);
	}

	public static List<IPosting> splitAndReturnLast(List<IPosting> postings) {
		return postings.subList(postings.size() / 2, postings.size());
	}

}
