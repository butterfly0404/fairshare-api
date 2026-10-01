package com.alishri.fairshare.expense;

import java.math.BigDecimal;

/**
 * Projection for aggregate queries: a per-user total.
 *
 * <p>Spring Data maps query result aliases onto these getters, so the database
 * returns two columns instead of whole entity graphs.
 */
public interface UserAmount {

    Long getUserId();

    BigDecimal getTotal();
}