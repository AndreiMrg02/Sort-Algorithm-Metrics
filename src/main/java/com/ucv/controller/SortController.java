package com.ucv.controller;

import com.ucv.model.*;
import com.ucv.sort.MergeSort;
import com.ucv.sort.RadixSort;
import com.ucv.sort.SelectionSort;
import com.ucv.sort.SortAlgorithm;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import org.controlsfx.control.CheckComboBox;
import org.controlsfx.control.Notifications;

import java.awt.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.List;

public class SortController implements Initializable {

    @FXML private AnchorPane paneWithFilters;
    @FXML private  CheckComboBox inputSizeCheckbox;
    @FXML private Button systemMetricsButton;
    @FXML private Button jvmMetricsButton;
    @FXML private Button generalMetricsButton;
    @FXML private WebView webViewPane;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;
    @FXML private GridPane resultContainer;
    @FXML private Button buttonStart;
    @FXML private  WebEngine webEngine;

    private final List<Integer> inputSizes = List.of(10, 100, 1_000, 10_000, 100_000,1_000_000,10_000_000);
    private final Map<String, Integer> algorithmColumn = Map.of(
            "MergeSort", 0,
            "RadixSort", 1,
            "SelectionSort", 2
    );
    private CheckComboBox<String> algorithmCheckbox;
    private CheckComboBox<String> modeCheckbox;
    private final SortResultDAO resultDAO = new SortResultDAO();
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        webEngine = webViewPane.getEngine();
        progressLabel.setTranslateY(45);
        progressLabel.setTranslateX(165);
        jvmMetricsButton.setOnAction(actionEvent -> {
            webEngine.load("http://localhost:3000/");
            try {
                Desktop.getDesktop().browse(new URI("http://localhost:3000/d/bemxln96d3x8gc/jvm-metrics-sortalgorithms?orgId=1&from=now-6h&to=now&timezone=Europe%2FBucharest&var-inputSize=10&var-algorithm=SelectionSort&var-mode=Sequential"));
            } catch (Exception ex){

            }

        });
        systemMetricsButton.setOnAction(actionEvent -> {
            webEngine.load("http://localhost:3000/");
            try {
                Desktop.getDesktop().browse(new URI("http://localhost:3000/d/aelanclsifshsd/system-metrics-sort-algorithms?orgId=1&from=now-6h&to=now&timezone=browser&var-algorithm=RadixSort&var-mode=Threaded&var-inputSize=10"));
            } catch (Exception ex){

            }

        });
        generalMetricsButton.setOnAction(actionEvent -> {
            webEngine.load("http://localhost:3000/");
            try {
                Desktop.getDesktop().browse(new URI("http://localhost:3000/d/demy3nutt7zeob/general-overview-metrics?orgId=1&from=now-6h&to=now&timezone=browser&var-inputSize=1000&var-algorithm=SelectionSort&var-mode=Threaded"));
            } catch (Exception ex){

            }

        });
        inputSizeCheckbox.getItems().addAll(FXCollections.observableArrayList(inputSizes));

        // ————— filtre deja existente —————
        AnchorPane.setTopAnchor(inputSizeCheckbox, 10.0);
        AnchorPane.setLeftAnchor(inputSizeCheckbox, 10.0);

        // 1. Filtru algoritmi
        List<String> algorithms = List.of("MergeSort", "RadixSort", "SelectionSort");
        algorithmCheckbox = new CheckComboBox<>(
                FXCollections.observableArrayList(algorithms)
        );
        algorithmCheckbox.setTooltip(new Tooltip("Select algorithms to include"));
        algorithmCheckbox.setTitle("Select Algorithm");
        algorithmCheckbox.setMinWidth(165);
        algorithmCheckbox.setMinHeight(35);
        algorithmCheckbox.setMaxWidth(165);
        algorithmCheckbox.setMaxHeight(35);
        algorithmCheckbox.setShowCheckedCount(true);
        AnchorPane.setTopAnchor(algorithmCheckbox, 60.0);
        AnchorPane.setLeftAnchor(algorithmCheckbox, 10.0);
        paneWithFilters.getChildren().add(algorithmCheckbox);

        // 2. Filtru moduri de rulare
        List<String> modes = List.of("Sequential", "Threaded");
        modeCheckbox = new CheckComboBox<>(
                FXCollections.observableArrayList(modes)
        );
        modeCheckbox.setTooltip(new Tooltip("Select execution modes"));
        modeCheckbox.setTitle("Select Mode");
        modeCheckbox.setMinWidth(165);
        modeCheckbox.setMinHeight(35);
        modeCheckbox.setMaxWidth(165);
        modeCheckbox.setMaxHeight(35);
        modeCheckbox.setShowCheckedCount(true);

        AnchorPane.setTopAnchor(modeCheckbox, 110.0);
        AnchorPane.setLeftAnchor(modeCheckbox, 10.0);
        paneWithFilters.getChildren().add(modeCheckbox);

    }

    @FXML
    protected void onStartClick() {

        List<Integer> sizes = inputSizeCheckbox.getCheckModel().getCheckedItems();
        List<String> selectedAlgs = algorithmCheckbox.getCheckModel().getCheckedItems();
        List<String> selectedModes = modeCheckbox.getCheckModel().getCheckedItems();

        // 2. Validare
        if (sizes.isEmpty() || selectedAlgs.isEmpty() || selectedModes.isEmpty()) {
            Notifications.create()
                    .title("No Selection")
                    .text("Please select at least one size, algorithm and mode.")
                    .hideAfter(Duration.seconds(3))
                    .position(Pos.TOP_CENTER)
                    .showWarning();
            return;
        }

        // 3. Pregătire UI și DB
        buttonStart.setDisable(true);
        resultContainer.getChildren().clear();
        resultDAO.deleteAll();
        addColumnTitles();

        // 4. Construcție dinamică a algoritmilor și numelor
        List<SortAlgorithm> algorithms = new ArrayList<>();
        List<String> algNames = new ArrayList<>();
        for (String alg : selectedAlgs) {
            switch (alg) {
                case "MergeSort":
                    algorithms.add(new MergeSort());
                    algNames.add("MergeSort");
                    break;
                case "RadixSort":
                    algorithms.add(new RadixSort());
                    algNames.add("RadixSort");
                    break;
                case "SelectionSort":
                    algorithms.add(new SelectionSort());
                    algNames.add("SelectionSort");
                    break;
            }
        }

        int totalSteps = sizes.size() * algorithms.size() * selectedModes.size();
        int[] completedSteps = { 0 };

        // 5. Thread de procesare
        new Thread(() -> {
            // warm-up JIT
            for (SortAlgorithm alg : algorithms) {
                alg.sort(generateRandomArray(100));
            }

            for (int sizeIndex = 0; sizeIndex < sizes.size(); sizeIndex++) {
                int size = sizes.get(sizeIndex);
                int[] baseArray = generateRandomArray(size);

                for (int algIndex = 0; algIndex < algorithms.size(); algIndex++) {
                    SortAlgorithm algorithm = algorithms.get(algIndex);
                    String name = algNames.get(algIndex);

                    for (String mode : selectedModes) {
                        // clone și sort
                        int[] arrayToSort = baseArray.clone();
                        String summary = runSortAndPersist(algorithm, arrayToSort, name, mode, size);

                        // actualizare UI
                        int finalSizeIndex = sizeIndex;
                        Platform.runLater(() -> {
                            addResultCard(name, mode, summary, finalSizeIndex);
                            updateProgress(completedSteps, totalSteps);
                        });
                    }
                }
            }

            // re-activează butonul
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
