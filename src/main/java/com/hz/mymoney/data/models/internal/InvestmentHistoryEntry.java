package com.hz.mymoney.data.models.internal;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvestmentHistoryEntry(LocalDate asAt, BigDecimal value) {
}
