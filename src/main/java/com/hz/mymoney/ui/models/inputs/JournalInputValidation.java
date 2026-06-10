package com.hz.mymoney.ui.models.inputs;

import java.util.List;
import java.util.Objects;

public interface JournalInputValidation {
	default boolean noNulls(List<?> list) {
		return list.stream().noneMatch(Objects::isNull);
	}

	boolean isValid();
}