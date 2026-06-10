package com.hz.mymoney.ui.models;

import java.math.BigDecimal;

public record Journal(String description, String destinationAccount, BigDecimal amount) {
}
