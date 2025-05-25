// Updated MergeSort.java
package com.ucv.sort;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class MergeSort implements SortAlgorithm {

    private static final int MAX_THREADS = 10;
    private static final AtomicInteger activeThreads = new AtomicInteger(0);
    private static final AtomicInteger spawnedThreads = new AtomicInteger(0);

    @Override
    public void sort(int[] arr) {
        if (arr.length > 1) {
            int mid = arr.length / 2;

            int[] left = Arrays.copyOfRange(arr, 0, mid);
            int[] right = Arrays.copyOfRange(arr, mid, arr.length);

            sort(left);
            sort(right);

            merge(arr, left, right);
        }
    }

    @Override
    public void sortUsingThreading(int[] arr) {
        if (arr.length > 1) {
            int mid = arr.length / 2;

            int[] left = Arrays.copyOfRange(arr, 0, mid);
            int[] right = Arrays.copyOfRange(arr, mid, arr.length);

            Thread leftSorter = null;
            Thread rightSorter = null;

            if (activeThreads.get() < MAX_THREADS) {
                activeThreads.incrementAndGet();
                spawnedThreads.incrementAndGet();
                leftSorter = new Thread(() -> {
                    sortUsingThreading(left);
                    activeThreads.decrementAndGet();
                });
                leftSorter.start();
            } else {
                sortUsingThreading(left);
            }

            if (activeThreads.get() < MAX_THREADS) {
                activeThreads.incrementAndGet();
                spawnedThreads.incrementAndGet();
                rightSorter = new Thread(() -> {
                    sortUsingThreading(right);
                    activeThreads.decrementAndGet();
                });
                rightSorter.start();
            } else {
                sortUsingThreading(right);
            }

            try {
                if (leftSorter != null) leftSorter.join();
                if (rightSorter != null) rightSorter.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            merge(arr, left, right);
        }
    }

    private void merge(int[] arr, int[] left, int[] right) {
        int i = 0, j = 0, k = 0;

        while (i < left.length && j < right.length) {
            if (left[i] <= right[j]) {
                arr[k++] = left[i++];
            } else {
                arr[k++] = right[j++];
            }
        }

        while (i < left.length) {
            arr[k++] = left[i++];
        }

        while (j < right.length) {
            arr[k++] = right[j++];
        }
    }

    public void resetSpawnedThreads() {
        spawnedThreads.set(0);
    }

    public int getSpawnedThreads() {
        return spawnedThreads.get();
    }
}
