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

public class HelloController {

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
