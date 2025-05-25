package com.ucv.sort;

public interface SortAlgorithm {
    void sort(int[] arr);
    void sortUsingThreading(int[] arr);
    public void resetSpawnedThreads();
    public int getSpawnedThreads();

}