package com.firebirdberlin.tinytimetracker.models;

public class LogDailySummary {
    public long timestamp;
    public long duration;
    public long saldo;

    public LogDailySummary() {
        this.timestamp = -1L;
        this.duration = 0L;
        this.saldo = -1L;
    }

    public LogDailySummary(long timestamp, long duration) {
        this.timestamp = timestamp;
        this.duration = duration;
        this.saldo = -1L;
    }

    public long calculateSaldo(long targetSeconds) {
         this.saldo = duration - ( 1000L * targetSeconds );
         return this.saldo;
    }
}
