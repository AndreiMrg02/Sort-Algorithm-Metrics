package sort;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SelectionSort implements SortAlgorithm {

    private static final int MAX_THREADS = 25;
    private static final AtomicInteger activeThreads = new AtomicInteger(0);
    private static final AtomicInteger spawnedThreads = new AtomicInteger(0);

    @Override
    public void sort(int[] arr) {
        int n = arr.length;
        for (int i = 0; i < n - 1; i++) {
            int minIndex = i;
            for (int j = i + 1; j < n; j++) {
                if (arr[j] < arr[minIndex]) {
                    minIndex = j;
                }
            }
            int temp = arr[minIndex];
            arr[minIndex] = arr[i];
            arr[i] = temp;
        }
    }

    @Override
    public void sortUsingThreading(int[] arr) {
        int n = arr.length;
        List<Thread> threads = new ArrayList<>();

        int chunkSize = n / MAX_THREADS;
        if (chunkSize == 0) chunkSize = 1;

        for (int t = 0; t < MAX_THREADS && t * chunkSize < n; t++) {
            final int start = t * chunkSize;
            final int end = (t == MAX_THREADS - 1) ? n : Math.min(n, (t + 1) * chunkSize);

            activeThreads.incrementAndGet();
            spawnedThreads.incrementAndGet();
            Thread thread = new Thread(() -> {
                selectionSortPartial(arr, start, end);
                activeThreads.decrementAndGet();
            });
            thread.start();
            threads.add(thread);
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        sort(arr);
    }

    private void selectionSortPartial(int[] arr, int start, int end) {
        for (int i = start; i < end - 1; i++) {
            int minIndex = i;
            for (int j = i + 1; j < end; j++) {
                if (arr[j] < arr[minIndex]) {
                    minIndex = j;
                }
            }
            synchronized (arr) {
                int temp = arr[minIndex];
                arr[minIndex] = arr[i];
                arr[i] = temp;
            }
        }
    }

    public void resetSpawnedThreads() {
        spawnedThreads.set(0);
    }

    public int getSpawnedThreads() {
        return spawnedThreads.get();
    }
}
