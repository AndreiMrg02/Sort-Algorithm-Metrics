package com.ucv.model;

public class JvmMetrics {
    private long heapMemoryUsedBytes;
    private long cpuTimeMs;
    private double cpuLoadAvg;

    public JvmMetrics(long heapMemoryUsedBytes, long cpuTimeMs, double cpuLoadAvg) {
        this.heapMemoryUsedBytes = heapMemoryUsedBytes;
        this.cpuTimeMs = cpuTimeMs;
        this.cpuLoadAvg = cpuLoadAvg;
    }

    public long getHeapMemoryUsedBytes() {
        return heapMemoryUsedBytes;
    }

    public void setHeapMemoryUsedBytes(long heapMemoryUsedBytes) {
        this.heapMemoryUsedBytes = heapMemoryUsedBytes;
    }

    public long getCpuTimeMs() {
        return cpuTimeMs;
    }

    public void setCpuTimeMs(long cpuTimeMs) {
        this.cpuTimeMs = cpuTimeMs;
    }

    public double getCpuLoadAvg() {
        return cpuLoadAvg;
    }

    public void setCpuLoadAvg(double cpuLoadAvg) {
        this.cpuLoadAvg = cpuLoadAvg;
    }

    // Getters & setters
}
