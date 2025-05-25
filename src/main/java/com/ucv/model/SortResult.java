package com.ucv.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "sort_results")
public class SortResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String algorithm;
    private String mode;
    private int inputSize;
    private long executionTimeMs;
    private int threadsSpawned;

    private long heapMemoryUsedBytes;
    private long cpuTimeMs;
    private double cpuLoadAvg;

    private double systemCpuLoad;
    private int availableProcessors;
    private long systemRamTotalMb;
    private long systemRamUsedMb;
    private long systemSwapTotalMb;
    private long systemSwapFreeMb;
    private long jvmUptimeMs;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public SortResult() {}

    public SortResult(
            SortMetadata meta,
            JvmMetrics jvm,
            SystemMetrics sys
    ) {
        this.algorithm = meta.getAlgorithm();
        this.mode = meta.getMode();
        this.inputSize = meta.getInputSize();
        this.executionTimeMs = meta.getExecutionTimeMs();
        this.threadsSpawned = meta.getThreadsSpawned();

        this.heapMemoryUsedBytes = jvm.getHeapMemoryUsedBytes();
        this.cpuTimeMs = jvm.getCpuTimeMs();
        this.cpuLoadAvg = jvm.getCpuLoadAvg();

        this.systemCpuLoad = sys.getSystemCpuLoad();
        this.availableProcessors = sys.getAvailableProcessors();
        this.systemRamTotalMb = sys.getSystemRamTotalMb();
        this.systemRamUsedMb = sys.getSystemRamUsedMb();
        this.systemSwapTotalMb = sys.getSystemSwapTotalMb();
        this.systemSwapFreeMb = sys.getSystemSwapFreeMb();
        this.jvmUptimeMs = sys.getJvmUptimeMs();
        // createdAt se setează automat
    }

    // Getters & Setters pentru toate câmpurile, inclusiv createdAt
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
