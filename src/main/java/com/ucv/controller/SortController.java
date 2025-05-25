package com.ucv.controller;

import com.ucv.model.*;
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
import com.ucv.sort.MergeSort;
import com.ucv.sort.RadixSort;
import com.ucv.sort.SelectionSort;
import com.ucv.sort.SortAlgorithm;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SortController {

    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;
    @FXML private GridPane resultContainer;
    @FXML private Button buttonStart;

    private final List<Integer> inputSizes = List.of(10, 100, 1_000, 10_000, 100_000);
    private final Map<String, Integer> algorithmColumn = Map.of(
            "MergeSort", 0,
            "RadixSort", 1,
            "SelectionSort", 2
    );

    private final SortResultDAO resultDAO = new SortResultDAO();

    @FXML
    protected void onStartClick() {
        buttonStart.setDisable(true);
        resultContainer.getChildren().clear();
        resultDAO.deleteAll();
        addColumnTitles();

        SortAlgorithm[] algorithms = { new MergeSort(), new RadixSort(), new SelectionSort() };
        String[] names = { "MergeSort", "RadixSort", "SelectionSort" };
        int totalSteps = algorithms.length * inputSizes.size() * 2;
        int[] completedSteps = { 0 };

        new Thread(() -> {
            // Warm-up for JIT
            for (SortAlgorithm alg : algorithms) {
                alg.sort(generateRandomArray(100));
            }

            for (int sizeIndex = 0; sizeIndex < inputSizes.size(); sizeIndex++) {
                int size = inputSizes.get(sizeIndex);
                int[] baseArray = generateRandomArray(size);

                for (int i = 0; i < algorithms.length; i++) {
                    SortAlgorithm algorithm = algorithms[i];
                    String name = names[i];

                    // Sequential execution
                    int[] seqArray = baseArray.clone();
                    String seqSummary = runSortAndPersist(algorithm, seqArray, name, "Sequential", size);
                    int finalSizeIndex = sizeIndex;
                    Platform.runLater(() -> {
                        addResultCard(name, "Sequential", seqSummary, finalSizeIndex);
                        updateProgress(completedSteps, totalSteps);
                    });

                    // Threaded execution
                    int[] thArray = baseArray.clone();
                    String thSummary = runSortAndPersist(algorithm, thArray, name, "Threaded", size);
                    Platform.runLater(() -> {
                        addResultCard(name, "Threaded", thSummary, finalSizeIndex);
                        updateProgress(completedSteps, totalSteps);
                    });
                }
            }
            Platform.runLater(() -> buttonStart.setDisable(false));
        }).start();
    }

    private void addColumnTitles() {
        algorithmColumn.forEach((alg, col) -> {
            Label title = new Label(alg);
            title.setFont(Font.font("Arial", 24));
            GridPane.setMargin(title, new Insets(10));
            resultContainer.add(title, col, 0);
        });
    }

    private void addResultCard(String algorithm, String mode, String text, int sizeIndex) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: #333333; -fx-border-radius: 8; -fx-background-radius: 8;");

        Text content = new Text(text);
        content.setFont(Font.font("Arial", 14));
        content.setWrappingWidth(280);
        content.setFill(javafx.scene.paint.Color.WHITE);

        card.getChildren().add(content);
        int col = algorithmColumn.get(algorithm);
        int row = sizeIndex * 2 + ("Sequential".equals(mode) ? 1 : 2);
        resultContainer.add(card, col, row);
        GridPane.setHgrow(card, Priority.ALWAYS);
        GridPane.setMargin(card, new Insets(5));
    }

    private String runSortAndPersist(
            SortAlgorithm algorithm,
            int[] array,
            String name,
            String mode,
            int size
    ) {
        algorithm.resetSpawnedThreads();

        // Metricele JVM și de sistem
        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
        ThreadMXBean thrBean = ManagementFactory.getThreadMXBean();
        com.sun.management.OperatingSystemMXBean osBean =
                (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        // Captură înainte de sortare
        long heapBefore = memBean.getHeapMemoryUsage().getUsed();
        long cpuBefore = thrBean.getCurrentThreadCpuTime();
        double cpuJvmBefore = osBean.getProcessCpuLoad();
        double cpuSysBefore = osBean.getSystemCpuLoad();

        long totalRAM = osBean.getTotalPhysicalMemorySize();
        long freeRAM = osBean.getFreePhysicalMemorySize();
        long usedRAM = totalRAM - freeRAM;

        long totalSwap = osBean.getTotalSwapSpaceSize();
        long freeSwap = osBean.getFreeSwapSpaceSize();

        int processors = osBean.getAvailableProcessors();
        long jvmUptime = ManagementFactory.getRuntimeMXBean().getUptime();

        // Timp execuție
        long start = System.nanoTime();
        if ("Threaded".equals(mode)) {
            algorithm.sortUsingThreading(array);
        } else {
            algorithm.sort(array);
        }
        long end = System.nanoTime();

        // Captură după sortare
        long heapAfter = memBean.getHeapMemoryUsage().getUsed();
        long cpuAfter = thrBean.getCurrentThreadCpuTime();
        double cpuJvmAfter = osBean.getProcessCpuLoad();
        double cpuSysAfter = osBean.getSystemCpuLoad();

        // Calcule finale
        long timeMs = (end - start) / 1_000_000;
        long heapDelta = heapAfter - heapBefore;
        long cpuTimeMs = (cpuAfter - cpuBefore) / 1_000_000;
        double cpuLoadAvg = (cpuJvmBefore + cpuJvmAfter) / 2.0;
        double systemCpuLoad = (cpuSysBefore + cpuSysAfter) / 2.0;
        long usedRamMb = (osBean.getTotalPhysicalMemorySize() - osBean.getFreePhysicalMemorySize()) / (1024 * 1024);
        long totalRamMb = osBean.getTotalPhysicalMemorySize() / (1024 * 1024);
        long swapTotalMb = totalSwap / (1024 * 1024);
        long swapFreeMb = freeSwap / (1024 * 1024);
        int threads = algorithm.getSpawnedThreads();

        SortMetadata meta = new SortMetadata(name, mode, size, timeMs, threads);
        JvmMetrics jvm = new JvmMetrics(heapDelta, cpuTimeMs, cpuLoadAvg);
        SystemMetrics sys = new SystemMetrics(systemCpuLoad, processors, totalRamMb, usedRamMb,
                swapTotalMb, swapFreeMb, jvmUptime);

        SortResult result = new SortResult(meta, jvm, sys);
        resultDAO.save(result);

        // Format afișat
        return String.format(
                "%s (%s)%nSize: %d%nTime: %d ms%nHeap Δ: %,d bytes%nThreads: %d%n" +
                        "CPU Load JVM: %.2f%%%nCPU Time: %d ms%nSystem CPU: %.2f%%%n" +
                        "RAM: %d / %d MB%nSwap: %d free / %d MB%nCores: %d%nUptime: %d ms",
                name, mode, size, timeMs, heapDelta, threads,
                cpuLoadAvg * 100, cpuTimeMs, systemCpuLoad * 100,
                usedRamMb, totalRamMb, swapFreeMb, swapTotalMb,
                processors, jvmUptime
        );
    }



    private int[] generateRandomArray(int size) {
        Random rnd = new Random();
        int[] arr = new int[size];
        for (int i = 0; i < size; i++) arr[i] = rnd.nextInt(10_000);
        return arr;
    }

    private void updateProgress(int[] done, int total) {
        done[0]++;
        double progress = (double) done[0] / total;
        Platform.runLater(() -> {
            progressBar.setProgress(progress);
            progressLabel.setText(String.format("Progress: %.0f%%", progress * 100));
        });
    }

}
