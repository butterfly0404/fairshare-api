package com.alishri.fairshare.expense;

/**
 * How an expense's amount is divided among its participants.
 *
 * <p>Persisted as a string, not an ordinal — reordering this enum must never
 * silently reinterpret existing rows.
 */
public enum SplitType {

    /** Divided evenly; any rounding remainder falls on the first participant. */
    EQUAL,

    /** Caller supplies each participant's exact share. */
    EXACT,

    /** Caller supplies percentages, which must total 100. */
    PERCENTAGE
}