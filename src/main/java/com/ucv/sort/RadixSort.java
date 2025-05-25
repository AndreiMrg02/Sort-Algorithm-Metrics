package com.ucv.sort;

import java.util.concurrent.atomic.AtomicInteger;

public class RadixSort implements SortAlgorithm {
    private static final int THREAD_COUNT = 4;
    private static final AtomicInteger spawnedThreads = new AtomicInteger(0);

    @Override
    public void sort(int[] arr) {
        int max = getMax(arr);
        for (int exp = 1; max / exp > 0; exp *= 10) {
            countSort(arr, exp);
        }
    }

    @Override
    public void sortUsingThreading(int[] arr) {
        int max = getMax(arr);
        for (int exp = 1; max / exp > 0; exp *= 10) {
            parallelCountSort(arr, exp);
        }
    }

    private int getMax(int[] arr) {
        int max = arr[0];
        for (int num : arr) {
            if (num > max) {
                max = num;
            }
        }
        return max;
    }

    private void countSort(int[] arr, int exp) {
        int n = arr.length;
        int[] output = new int[n];
        int[] count = new int[10];

        for (int i = 0; i < n; i++) {
            count[(arr[i] / exp) % 10]++;
        }

        for (int i = 1; i < 10; i++) {
            count[i] += count[i - 1];
        }

        for (int i = n - 1; i >= 0; i--) {
            output[count[(arr[i] / exp) % 10] - 1] = arr[i];
            count[(arr[i] / exp) % 10]--;
        }

        System.arraycopy(output, 0, arr, 0, n);
    }

    private void parallelCountSort(int[] arr, int exp) {
        int n = arr.length;
        int[] output = new int[n];
        int[][] threadCounts = new int[THREAD_COUNT][10];
        Thread[] threads = new Thread[THREAD_COUNT];
        int chunkSize = (n + THREAD_COUNT - 1) / THREAD_COUNT;

        for (int t = 0; t < THREAD_COUNT; t++) {
            final int threadIndex = t;
            threads[t] = new Thread(() -> {
                int start = threadIndex * chunkSize;
                int end = Math.min(start + chunkSize, n);
                for (int i = start; i < end; i++) {
                    int digit = (arr[i] / exp) % 10;
                    threadCounts[threadIndex][digit]++;
                }
            });
            spawnedThreads.incrementAndGet();
            threads[t].start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        int[] count = new int[10];
        for (int d = 0; d < 10; d++) {
            for (int t = 0; t < THREAD_COUNT; t++) {
                count[d] += threadCounts[t][d];
            }
        }

        for (int i = 1; i < 10; i++) {
            count[i] += count[i - 1];
        }

        for (int i = n - 1; i >= 0; i--) {
            int digit = (arr[i] / exp) % 10;
            output[--count[digit]] = arr[i];
        }

        System.arraycopy(output, 0, arr, 0, n);
    }

    public void resetSpawnedThreads() {
        spawnedThreads.set(0);
    }

    public int getSpawnedThreads() {
        return spawnedThreads.get();
    }
}