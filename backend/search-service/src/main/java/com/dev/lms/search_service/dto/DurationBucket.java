package com.dev.lms.search_service.dto;

public enum DurationBucket {
    SHORT(0, 120),
    MEDIUM(120, 600),
    LONG(600, Integer.MAX_VALUE);

    private final int minMinutes;
    private final int maxMinutes;

    DurationBucket(int minMinutes, int maxMinutes) {
        this.minMinutes = minMinutes;
        this.maxMinutes = maxMinutes;
    }

    public int getMinMinutes() {
        return minMinutes;
    }

    public int getMaxMinutes() {
        return maxMinutes;
    }
}
