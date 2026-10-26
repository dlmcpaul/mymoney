package com.hz.mymoney.data.models.ledger;

public sealed interface Entry permits RawEntry, LedgerEntry {
}
