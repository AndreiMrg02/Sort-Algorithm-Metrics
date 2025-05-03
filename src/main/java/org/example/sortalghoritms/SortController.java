/*
package org.example.sortalghoritms;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import sort.MergeSort;
import sort.RadixSort;
import sort.SelectionSort;
import sort.SortAlgorithm;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SortController {

    @FXML
    public ProgressBar progressBar;

    @FXML
    public Label progressLabel;

    @FXML
    public GridPane resultContainer;

    @FXML
    public Button buttonStart;

    private final List<Integer> inputSizes = List.of(10, 100, 1000, 10000, 100000);
    private final Map<String, Map<String, List<JSONObject>>> resultsJsonMap = new HashMap<>();

    private final Map<String, Integer> algorithmColumn = Map.of(
            "MergeSort", 0,
            "RadixSort", 1,
            "SelectionSort", 2
    );

    @FXML
    protected void onHelloButtonClick() {
        buttonStart.setDisable(true);

        SortAlgorithm[] algorithms = { new MergeSort(), new RadixSort(), new SelectionSort() };
        String[] names = { "MergeSort", "RadixSort", "SelectionSort" };

        int totalSteps = algorithms.length * inputSizes.size() * 2;
        int[] completedSteps = { 0 };

        Platform.runLater(() -> {
            resultContainer.getChildren().clear();
            addColumnTitles();
        });

        new Thread(() -> {
            for (int sizeIndex = 0; sizeIndex < inputSizes.size(); sizeIndex++) {
                int size = inputSizes.get(sizeIndex);
                int[] originalArray = generateRandomArray(size);

                for (int i = 0; i < algorithms.length; i++) {
                    SortAlgorithm algorithm = algorithms[i];
                    String name = names[i];

                    resultsJsonMap.putIfAbsent(name, new HashMap<>());
                    resultsJsonMap.get(name).putIfAbsent("Sequential", new java.util.ArrayList<>());
                    resultsJsonMap.get(name).putIfAbsent("Threaded", new java.util.ArrayList<>());

                    int finalSizeIndex = sizeIndex;

                    // Sequential
                    int[] arrayCopy = originalArray.clone();
                    String resultSequential = runSortAndCollectInfo(algorithm, arrayCopy, name, "Sequential", size);
                    Platform.runLater(() -> {
                        addResultCard(name, "Sequential", resultSequential, finalSizeIndex);
                        updateProgress(completedSteps, totalSteps);
                    });

                    // Threaded
                    arrayCopy = originalArray.clone();
                    String resultThreaded = runSortAndCollectInfo(algorithm, arrayCopy, name, "Threaded", size);
                    Platform.runLater(() -> {
                        addResultCard(name, "Threaded", resultThreaded, finalSizeIndex);
                        updateProgress(completedSteps, totalSteps);
                    });
                }
            }

            saveResultsAsGrafanaJson();
            Platform.runLater(() -> buttonStart.setDisable(false));
        }).start();
    }

    private void addColumnTitles() {
        for (var entry : algorithmColumn.entrySet()) {
            String algorithm = entry.getKey();
            int column = entry.getValue();

            Label title = new Label(algorithm);
            title.setFont(Font.font("Arial", 24));
            title.setStyle("-fx-text-fill: black; -fx-font-weight: bold;");
            resultContainer.add(title, column, 0);
            GridPane.setMargin(title, new Insets(10));
        }
    }

    private void addResultCard(String algorithmName, String mode, String text, int sizeIndex) {
        VBox card = new VBox();
        card.setPadding(new Insets(15));
        card.setSpacing(10);
        card.setStyle("-fx-background-color: #333333; -fx-border-color: #444444; -fx-border-radius: 10; -fx-background-radius: 10;");
        card.setMaxWidth(300);

        Text content = new Text(text);
        content.setFont(Font.font("Arial", 14));
        content.setFill(javafx.scene.paint.Color.WHITE);
        content.setWrappingWidth(280);

        card.getChildren().add(content);

        int column = algorithmColumn.getOrDefault(algorithmName, 0);
        int row = sizeIndex * 2 + (mode.equals("Sequential") ? 1 : 2);

        resultContainer.add(card, column, row);
        GridPane.setMargin(card, new Insets(10));
        GridPane.setHgrow(card, Priority.ALWAYS);
    }

    private String runSortAndCollectInfo(SortAlgorithm algorithm, int[] array, String name, String mode, int inputSize) {
        resetSpawnedThreadsIfNeeded(algorithm);

        long startTime = System.nanoTime();
        long usedMemoryBefore = getUsedMemory();
        double cpuBefore = getCpuLoadSafe();

        if ("Threaded".equals(mode)) {
            algorithm.sortUsingThreading(array);
        } else {
            algorithm.sort(array);
        }

        long endTime = System.nanoTime();
        double cpuAfter = getCpuLoadSafe();
        long usedMemoryAfter = getUsedMemory();
        int spawnedThreads = getSpawnedThreadsIfNeeded(algorithm);

        long executionTimeMs    = (endTime - startTime) / 1_000_000;
        long memoryUsedBytes    = usedMemoryAfter - usedMemoryBefore;
        double cpuLoadChangePct = (cpuAfter - cpuBefore) * 100;

        // --- Build Grafana-friendly JSON point ---
        JSONObject point = new JSONObject();
        point.put("measurement", "sort_benchmark");
        point.put("time", Instant.now().toString());

        JSONObject tags = new JSONObject();
        tags.put("algorithm", name);
        tags.put("mode", mode);
        tags.put("input_size", inputSize);
        point.put("tags", tags);

        JSONObject fields = new JSONObject();
        fields.put("execution_time_ms", executionTimeMs);
        fields.put("memory_used_bytes", memoryUsedBytes);
        fields.put("cpu_load_change_percent", cpuLoadChangePct);
        fields.put("threads_spawned", spawnedThreads);
        point.put("fields", fields);

        resultsJsonMap.get(name).get(mode).add(point);

        // Build on-screen summary
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" (").append(mode).append(")\n")
                .append("Input Size: ").append(inputSize).append("\n")
                .append("Execution Time: ").append(executionTimeMs).append(" ms\n")
                .append("Memory Used: ").append(memoryUsedBytes).append(" bytes\n")
                .append("CPU Load Change: ").append(String.format("%.2f", cpuLoadChangePct)).append(" %\n")
                .append("Threads Spawned: ").append(spawnedThreads).append("\n");
        return sb.toString();
    }

    private void saveResultsAsGrafanaJson() {
        JSONArray allPoints = new JSONArray();
        for (var algEntry : resultsJsonMap.entrySet()) {
            for (var modeEntry : algEntry.getValue().entrySet()) {
                for (JSONObject point : modeEntry.getValue()) {
                    allPoints.put(point);
                }
            }
        }

        try (FileWriter file = new FileWriter("benchmark_sort_benchmark.json")) {
            file.write(allPoints.toString(4));
            System.out.println("Grafana-friendly JSON saved to benchmark_sort_benchmark.json");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateProgress(int[] completedSteps, int totalSteps) {
        completedSteps[0]++;
        double progress = (double) completedSteps[0] / totalSteps;
        progressBar.setProgress(progress);
        progressLabel.setText(String.format("Progress: %.0f%%", progress * 100));
    }

    private int[] generateRandomArray(int size) {
        Random random = new Random();
        int[] arr = new int[size];
        for (int i = 0; i < size; i++) {
            arr[i] = random.nextInt(10000);
        }
        return arr;
    }

    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private double getCpuLoadSafe() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            double load = ((com.sun.management.OperatingSystemMXBean) osBean).getSystemCpuLoad();
            if (load >= 0.0) {
                return load;
            }
        }
        return 0.0;
    }

    private void resetSpawnedThreadsIfNeeded(SortAlgorithm algorithm) {
        if (algorithm instanceof MergeSort
                || algorithm instanceof SelectionSort
                || algorithm instanceof RadixSort) {
            algorithm.resetSpawnedThreads();
        }
    }

    private int getSpawnedThreadsIfNeeded(SortAlgorithm algorithm) {
        if (algorithm instanceof MergeSort
                || algorithm instanceof SelectionSort
                || algorithm instanceof RadixSort) {
            return algorithm.getSpawnedThreads();
        }
        return 0;
    }
}
*/
package org.example.sortalghoritms;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import sort.MergeSort;
import sort.RadixSort;
import sort.SelectionSort;
import sort.SortAlgorithm;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SortController {

    @FXML public ProgressBar progressBar;
    @FXML public Label progressLabel;
    @FXML public GridPane resultContainer;
    @FXML public Button buttonStart;

    private final List<Integer> inputSizes = List.of(10, 100, 1000, 10000, 100000, 500000);
    private final Map<String, Map<String, List<JSONObject>>> resultsJsonMap = new HashMap<>();
    private final Map<String, Integer> algorithmColumn = Map.of(
            "MergeSort", 0,
            "RadixSort", 1,
            "SelectionSort", 2
    );

    @FXML
    protected void onHelloButtonClick() {
        buttonStart.setDisable(true);

        SortAlgorithm[] algorithms = { new MergeSort(), new RadixSort(), new SelectionSort() };
        String[] names = { "MergeSort", "RadixSort", "SelectionSort" };
        int totalSteps = algorithms.length * inputSizes.size() * 2;
        int[] completedSteps = { 0 };

        Platform.runLater(() -> {
            resultContainer.getChildren().clear();
            addColumnTitles();
        });

        new Thread(() -> {
            // Warm-up JIT
            for (SortAlgorithm alg : algorithms) {
                alg.sort(generateRandomArray(100));
            }

            for (int sizeIndex = 0; sizeIndex < inputSizes.size(); sizeIndex++) {
                int size = inputSizes.get(sizeIndex);
                int[] originalArray = generateRandomArray(size);

                for (int i = 0; i < algorithms.length; i++) {
                    SortAlgorithm algorithm = algorithms[i];
                    String name = names[i];

                    // initialize JSON storage
                    resultsJsonMap
                            .computeIfAbsent(name, k -> new HashMap<>())
                            .computeIfAbsent("Sequential", k -> new java.util.ArrayList<>());
                    resultsJsonMap
                            .get(name)
                            .computeIfAbsent("Threaded", k -> new java.util.ArrayList<>());

                    // Sequential
                    String seq = runSortAndCollectInfo(algorithm, originalArray.clone(), name, "Sequential", size);
                    int finalSizeIndex = sizeIndex;
                    Platform.runLater(() -> {
                        addResultCard(name, "Sequential", seq, finalSizeIndex);
                        updateProgress(completedSteps, totalSteps);
                    });

                    // Threaded
                    String th  = runSortAndCollectInfo(algorithm, originalArray.clone(), name, "Threaded", size);
                    int finalSizeIndex1 = sizeIndex;
                    Platform.runLater(() -> {
                        addResultCard(name, "Threaded", th, finalSizeIndex1);
                        updateProgress(completedSteps, totalSteps);
                    });
                }
            }

            saveResultsAsGrafanaJson();
            Platform.runLater(() -> buttonStart.setDisable(false));
        }).start();
    }

    private void addColumnTitles() {
        algorithmColumn.forEach((alg, col) -> {
            Label title = new Label(alg);
            title.setFont(Font.font("Arial", 24));
            title.setStyle("-fx-text-fill: black; -fx-font-weight: bold;");
            resultContainer.add(title, col, 0);
            GridPane.setMargin(title, new Insets(10));
        });
    }

    private void addResultCard(String algorithmName, String mode, String text, int sizeIndex) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle(
                "-fx-background-color: #333333;"
                        + "-fx-border-color: #444444;"
                        + "-fx-border-radius: 10;"
                        + "-fx-background-radius: 10;"
        );
        card.setMaxWidth(300);

        Text content = new Text(text);
        content.setFont(Font.font("Arial", 14));
        content.setFill(javafx.scene.paint.Color.WHITE);
        content.setWrappingWidth(280);

        card.getChildren().add(content);

        int column = algorithmColumn.get(algorithmName);
        int row = sizeIndex * 2 + (mode.equals("Sequential") ? 1 : 2);
        resultContainer.add(card, column, row);
        GridPane.setMargin(card, new Insets(10));
        GridPane.setHgrow(card, Priority.ALWAYS);
    }

    private String runSortAndCollectInfo(
            SortAlgorithm algorithm,
            int[] array,
            String name,
            String mode,
            int inputSize
    ) {
        // for thread count reset
        resetSpawnedThreadsIfNeeded(algorithm);

        // --- for exact mem & CPU & GC ---
        System.gc();
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}


        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        ClassLoadingMXBean classBean = ManagementFactory.getClassLoadingMXBean();
        com.sun.management.OperatingSystemMXBean osBean =
                (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        long usedHeapBefore      = memBean.getHeapMemoryUsage().getUsed();
        long processCpuTimeBefore= osBean.getProcessCpuTime();
        long threadCpuTimeBefore = threadBean.getCurrentThreadCpuTime();
        long loadedClassesBefore = classBean.getLoadedClassCount();
        int  threadCountBefore   = threadBean.getThreadCount();
        long gcCountBefore       = 0, gcTimeBefore = 0;
        for (var g : gcBeans) {
            gcCountBefore += g.getCollectionCount();
            gcTimeBefore  += g.getCollectionTime();
        }

        long startTime = System.nanoTime();
        if ("Threaded".equals(mode)) {
            algorithm.sortUsingThreading(array);
        } else {
            algorithm.sort(array);
        }
        long endTime = System.nanoTime();

        // --- after ---
        long usedHeapAfter       = memBean.getHeapMemoryUsage().getUsed();
        long processCpuTimeAfter = osBean.getProcessCpuTime();
        long threadCpuTimeAfter  = threadBean.getCurrentThreadCpuTime();
        long loadedClassesAfter  = classBean.getLoadedClassCount();
        int  threadCountAfter    = threadBean.getThreadCount();
        long gcCountAfter        = 0, gcTimeAfter = 0;
        for (var g : gcBeans) {
            gcCountAfter += g.getCollectionCount();
            gcTimeAfter  += g.getCollectionTime();
        }
        int spawnedThreads = getSpawnedThreadsIfNeeded(algorithm);

        // deltas
        long executionTimeMs      = (endTime - startTime) / 1_000_000;
        long heapUsedBytes        = usedHeapAfter - usedHeapBefore;
        long procCpuTimeMs        = (processCpuTimeAfter - processCpuTimeBefore) / 1_000_000;
        long threadCpuTimeMs      = (threadCpuTimeAfter  - threadCpuTimeBefore)  / 1_000_000;
        long gcCountDelta         = gcCountAfter - gcCountBefore;
        long gcTimeDeltaMs        = (gcTimeAfter  - gcTimeBefore)  ;
        long classLoadedDelta     = loadedClassesAfter - loadedClassesBefore;
        int  threadCountPeakDelta = threadCountAfter - threadCountBefore;

        // --- build JSON point ---
        JSONObject point = new JSONObject();
        point.put("measurement", "sort_benchmark");
        point.put("time", Instant.now().toString());

        JSONObject tags = new JSONObject();
        tags.put("algorithm", name);
        tags.put("mode", mode);
        tags.put("input_size", inputSize);
        point.put("tags", tags);

        JSONObject fields = new JSONObject();
        fields.put("execution_time_ms",        executionTimeMs);
        fields.put("heap_memory_used_bytes",  heapUsedBytes);
        fields.put("process_cpu_time_ms",     procCpuTimeMs);
        fields.put("thread_cpu_time_ms",      threadCpuTimeMs);
        fields.put("gc_count",                gcCountDelta);
        fields.put("gc_time_ms",              gcTimeDeltaMs);
        fields.put("classes_loaded_delta",    classLoadedDelta);
        fields.put("thread_count_delta",      threadCountPeakDelta);
        fields.put("threads_spawned",         spawnedThreads);
        point.put("fields", fields);

        resultsJsonMap.get(name).get(mode).add(point);

        // --- build on-screen summary ---
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" (").append(mode).append(")\n")
                .append("Input Size: ").append(inputSize).append("\n")
                .append("Exec Time: ").append(executionTimeMs).append(" ms\n")
                .append("Heap Δ: ").append(heapUsedBytes).append(" bytes\n")
                .append("Proc CPU: ").append(procCpuTimeMs).append(" ms\n")
                .append("Thread CPU: ").append(threadCpuTimeMs).append(" ms\n")
                .append("GC Count: ").append(gcCountDelta).append("\n")
                .append("GC Time: ").append(gcTimeDeltaMs).append(" ms\n")
                .append("Classes Loaded Δ: ").append(classLoadedDelta).append("\n")
                .append("Threads Spawned: ").append(spawnedThreads).append("\n");
        return sb.toString();
    }

    private void saveResultsAsGrafanaJson() {
        // for each algorithm…
        for (var algEntry : resultsJsonMap.entrySet()) {
            String algorithm = algEntry.getKey();

            // …and for each mode under that algorithm
            for (var modeEntry : algEntry.getValue().entrySet()) {
                String mode = modeEntry.getKey();
                List<JSONObject> points = modeEntry.getValue();
                JSONArray arr = new JSONArray(points);

                // make a filename like benchmark_mergesort_sequential.json
                String fname = String.format(
                        "benchmark_%s_%s.json",
                        algorithm.toLowerCase(),
                        mode.toLowerCase()
                );

                try (FileWriter fw = new FileWriter(fname)) {
                    fw.write(arr.toString(4));
                    System.out.println("Wrote " + fname);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private void updateProgress(int[] completedSteps, int totalSteps) {
        completedSteps[0]++;
        double progress = (double) completedSteps[0] / totalSteps;
        progressBar.setProgress(progress);
        progressLabel.setText(String.format("Progress: %.0f%%", progress * 100));
    }

    private int[] generateRandomArray(int size) {
        Random random = new Random();
        int[] arr = new int[size];
        for (int i = 0; i < size; i++) arr[i] = random.nextInt(10_000);
        return arr;
    }

    private void resetSpawnedThreadsIfNeeded(SortAlgorithm algorithm) {
        if (algorithm instanceof MergeSort
                || algorithm instanceof SelectionSort
                || algorithm instanceof RadixSort) {
            algorithm.resetSpawnedThreads();
        }
    }

    private int getSpawnedThreadsIfNeeded(SortAlgorithm algorithm) {
        if (algorithm instanceof MergeSort
                || algorithm instanceof SelectionSort
                || algorithm instanceof RadixSort) {
            return algorithm.getSpawnedThreads();
        }
        return 0;
    }
}
