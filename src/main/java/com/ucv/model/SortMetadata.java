package com.ucv.model;

public class SortMetadata {
    private String algorithm;
    private String mode;
    private int inputSize;
    private long executionTimeMs;
    private int threadsSpawned;

    public SortMetadata(String algorithm, String mode, int inputSize, long executionTimeMs, int threadsSpawned) {
        this.algorithm = algorithm;
        this.mode = mode;
        this.inputSize = inputSize;
        this.executionTimeMs = executionTimeMs;
        this.threadsSpawned = threadsSpawned;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public int getInputSize() {
        return inputSize;
    }

    public void setInputSize(int inputSize) {
        this.inputSize = inputSize;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public int getThreadsSpawned() {
        return threadsSpawned;
    }

    public void setThreadsSpawned(int threadsSpawned) {
        this.threadsSpawned = threadsSpawned;
    }

    // Getters & setters
}
