package com.hz.mymoney.ui.models;

import java.math.BigDecimal;

public record SummedTransaction(String description, BigDecimal amount) {
}
