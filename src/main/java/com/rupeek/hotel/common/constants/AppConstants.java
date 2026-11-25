package com.rupeek.hotel.common.constants;

public final class AppConstants {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    /** Longest stay that can be searched or booked in one go. */
    public static final int MAX_STAY_NIGHTS = 30;

    private AppConstants() {
    }
}
