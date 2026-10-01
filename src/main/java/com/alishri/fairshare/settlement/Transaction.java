package com.alishri.fairshare.settlement;

import java.math.BigDecimal;

/**
 * A single payment in a settlement plan: {@code from} pays {@code to} the given amount.
 */
public record Transaction(String from, String to, BigDecimal amount) {
}