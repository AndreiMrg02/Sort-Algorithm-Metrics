package com.ucv.model;

public class SystemMetrics {
    private double systemCpuLoad;
    private int availableProcessors;
    private long systemRamTotalMb;
    private long systemRamUsedMb;
    private long systemSwapTotalMb;
    private long systemSwapFreeMb;
    private long jvmUptimeMs;

    public SystemMetrics(double systemCpuLoad, int availableProcessors,
                         long systemRamTotalMb, long systemRamUsedMb,
                         long systemSwapTotalMb, long systemSwapFreeMb, long jvmUptimeMs) {
        this.systemCpuLoad = systemCpuLoad;
        this.availableProcessors = availableProcessors;
        this.systemRamTotalMb = systemRamTotalMb;
        this.systemRamUsedMb = systemRamUsedMb;
        this.systemSwapTotalMb = systemSwapTotalMb;
        this.systemSwapFreeMb = systemSwapFreeMb;
        this.jvmUptimeMs = jvmUptimeMs;
    }

    public double getSystemCpuLoad() {
        return systemCpuLoad;
    }

    public void setSystemCpuLoad(double systemCpuLoad) {
        this.systemCpuLoad = systemCpuLoad;
    }

    public int getAvailableProcessors() {
        return availableProcessors;
    }

    public void setAvailableProcessors(int availableProcessors) {
        this.availableProcessors = availableProcessors;
    }

    public long getSystemRamTotalMb() {
        return systemRamTotalMb;
    }

    public void setSystemRamTotalMb(long systemRamTotalMb) {
        this.systemRamTotalMb = systemRamTotalMb;
    }

    public long getSystemRamUsedMb() {
        return systemRamUsedMb;
    }

    public void setSystemRamUsedMb(long systemRamUsedMb) {
        this.systemRamUsedMb = systemRamUsedMb;
    }

    public long getSystemSwapTotalMb() {
        return systemSwapTotalMb;
    }

    public void setSystemSwapTotalMb(long systemSwapTotalMb) {
        this.systemSwapTotalMb = systemSwapTotalMb;
    }

    public long getSystemSwapFreeMb() {
        return systemSwapFreeMb;
    }

    public void setSystemSwapFreeMb(long systemSwapFreeMb) {
        this.systemSwapFreeMb = systemSwapFreeMb;
    }

    public long getJvmUptimeMs() {
        return jvmUptimeMs;
    }

    public void setJvmUptimeMs(long jvmUptimeMs) {
        this.jvmUptimeMs = jvmUptimeMs;
    }

    // Getters & setters
}
