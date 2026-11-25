package ru.ofd.marking.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import ru.ofd.marking.client.ApiClient;
import ru.ofd.marking.client.ClientConfig;

public class MainView extends BorderPane {

    private final ApiClient api;
    private final ClientConfig config;
    private final Label statusLabel = new Label("Готово");
    private final TabPane tabs = new TabPane();

    private final TableView<Map<String, Object>> dashTable = mapTable();
    private final TableView<Map<String, Object>> worstTable = mapTable();
    private final TableView<Map<String, Object>> groupsTable = mapTable();
    private final TableView<Map<String, Object>> matrixTable = mapTable();
    private final TableView<Map<String, Object>> receiptsTable = mapTable();
    private final TextArea receiptDetail = new TextArea();
    private final TextField nameFilter = new TextField();
    private final CheckBox markedOnly = new CheckBox("Только с маркировкой");
    private final TextField serverUrlField = new TextField();
    private final TextField thresholdField = new TextField();
    private final TextField periodField = new TextField();
    private final Label syncLabel = new Label();

    public MainView(ApiClient api, ClientConfig config) {
        this.api = api;
        this.config = config;
        setTop(buildToolbar());
        setCenter(tabs);
        setBottom(statusBar());
        tabs.getTabs().addAll(
                tab("Дашборд ТТ", dashPane()),
                tab("Худшие ТТ", worstPane()),
                tab("Группы", groupsPane()),
                tab("Матрица ТТ×группа", matrixPane()),
                tab("Чеки", receiptsPane()),
                tab("Настройки", settingsPane())
        );
        serverUrlField.setText(config.getServerUrl());
        thresholdField.setText(String.valueOf(config.getThreshold()));
        periodField.setText(String.valueOf(config.getPeriodDays()));
    }

    private HBox buildToolbar() {
        Button refresh = new Button("Обновить");
        refresh.setOnAction(e -> refreshAll());
        Button sync = new Button("Синхронизация ОФД");
        sync.setOnAction(e -> {
            sync.setDisable(true);
            runAsync(() -> {
                try {
                    Platform.runLater(() -> statusLabel.setText("Синхронизация запущена…"));
                    api.triggerSync();
                    for (int i = 0; i < 120; i++) {
                        Thread.sleep(1000);
                        JsonNode st = api.syncStatus();
                        boolean running = st.path("running").asBoolean(false);
                        String msg = st.path("message").asText("-");
                        Platform.runLater(() -> {
                            syncLabel.setText("Синхронизация: " + msg + " [" + st.path("mode").asText("?") + "]");
                            statusLabel.setText(running ? "Синхронизация…" : "Синхронизация завершена");
                        });
                        if (!running && i > 0) {
                            break;
                        }
                    }
                    Platform.runLater(this::refreshAll);
                } finally {
                    Platform.runLater(() -> sync.setDisable(false));
                }
            });
        });
        Button export = new Button("Excel");
        export.setOnAction(e -> exportCurrent());
        Label brand = new Label("ОФД · Маркировка");
        brand.getStyleClass().add("brand");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(12, brand, spacer, sync, refresh, export);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(12, 16, 12, 16));
        bar.getStyleClass().add("toolbar");
        return bar;
    }

    private HBox statusBar() {
        HBox box = new HBox(16, statusLabel, syncLabel);
        box.setPadding(new Insets(8, 16, 8, 16));
        box.getStyleClass().add("status-bar");
        return box;
    }

    private BorderPane dashPane() {
        setupColumns(dashTable, "displayName:ТТ", "totalReceipts:Чеков",
                "markedReceipts:С маркировкой", "markedPercent:%");
        return wrap(dashTable, "Доля чеков с маркированными товарами по торговым точкам");
    }

    private BorderPane worstPane() {
        setupColumns(worstTable, "displayName:ТТ", "markedPercent:%",
                "totalReceipts:Чеков", "markedReceipts:Маркированных");
        Label hint = new Label("ТТ ниже порога (настройка, по умолчанию 25%)");
        hint.getStyleClass().add("hint");
        BorderPane p = wrap(worstTable, "Рейтинг худших торговых точек");
        ((VBox) p.getTop()).getChildren().add(hint);
        return p;
    }

    private BorderPane groupsPane() {
        setupColumns(groupsTable, "groupTitle:Группа", "totalItems:Позиций",
                "markedItems:Маркированных", "markedPercent:%");
        return wrap(groupsTable, "Статистика по товарным группам");
    }

    private BorderPane matrixPane() {
        setupColumns(matrixTable, "tradePointName:ТТ", "groupTitle:Группа",
                "totalItems:Позиций", "markedItems:Маркированных", "markedPercent:%");
        return wrap(matrixTable, "Матрица торговая точка × товарная группа");
    }

    private BorderPane receiptsPane() {
        setupColumns(receiptsTable, "id:ID", "docDateTime:Дата", "tradePointName:ТТ",
                "kktRegNumber:ККТ", "docNumber:ФД", "totalSumKopecks:Сумма (коп.)", "hasMarkedGoods:Маркировка");
        receiptsTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Map<String, Object> row = receiptsTable.getSelectionModel().getSelectedItem();
                if (row != null && row.get("id") != null) {
                    long id = ((Number) row.get("id")).longValue();
                    runAsync(() -> {
                        JsonNode detail = api.receipt(id);
                        Platform.runLater(() -> receiptDetail.setText(pretty(detail)));
                    });
                }
            }
        });
        Button search = new Button("Найти");
        search.setOnAction(e -> loadReceipts());
        nameFilter.setPromptText("Наименование позиции");
        HBox filters = new HBox(10, nameFilter, markedOnly, search);
        filters.setPadding(new Insets(0, 0, 8, 0));
        receiptDetail.setEditable(false);
        receiptDetail.setPrefRowCount(12);
        receiptDetail.setPromptText("Двойной клик по чеку — детали позиций");
        SplitPane split = new SplitPane(receiptsTable, receiptDetail);
        split.setOrientation(javafx.geometry.Orientation.VERTICAL);
        split.setDividerPositions(0.55);
        BorderPane p = new BorderPane();
        VBox top = new VBox(6, title("Поиск чеков"), filters);
        top.setPadding(new Insets(16));
        p.setTop(top);
        p.setCenter(split);
        BorderPane.setMargin(split, new Insets(0, 16, 16, 16));
        return p;
    }

    private BorderPane settingsPane() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.addRow(0, new Label("URL сервера"), serverUrlField);
        grid.addRow(1, new Label("Порог худших ТТ, %"), thresholdField);
        grid.addRow(2, new Label("Период, дней"), periodField);
        serverUrlField.setPrefWidth(420);
        Button save = new Button("Сохранить");
        save.setOnAction(e -> {
            config.setServerUrl(serverUrlField.getText().trim());
            config.setThreshold(Double.parseDouble(thresholdField.getText().trim()));
            config.setPeriodDays(Integer.parseInt(periodField.getText().trim()));
            config.save();
            runAsync(() -> {
                api.updateSettings(Map.of(
                        "worst_threshold_percent", thresholdField.getText().trim(),
                        "default_period_days", periodField.getText().trim()));
                Platform.runLater(() -> {
                    statusLabel.setText("Настройки сохранены");
                    refreshAll();
                });
            });
        });
        VBox box = new VBox(16, title("Настройки"), grid, save,
                new Label("API KEY ОФД задаётся только на сервере (OFD_API_KEY), не в клиенте."));
        box.setPadding(new Insets(16));
        return new BorderPane(box);
    }

    public void selectTab(int index) {
        tabs.getSelectionModel().select(index);
    }

    public void refreshAll() {
        runAsync(() -> {
            try {
                List<Map<String, Object>> dash = api.tradePoints();
                List<Map<String, Object>> worst = api.worst(config.getThreshold());
                List<Map<String, Object>> groups = api.groups();
                List<Map<String, Object>> matrix = api.matrix();
                JsonNode sync = api.syncStatus();
                Platform.runLater(() -> {
                    dashTable.setItems(FXCollections.observableArrayList(dash));
                    worstTable.setItems(FXCollections.observableArrayList(worst));
                    groupsTable.setItems(FXCollections.observableArrayList(groups));
                    matrixTable.setItems(FXCollections.observableArrayList(matrix));
                    syncLabel.setText("Синхронизация: " + sync.path("message").asText("-")
                            + " [" + sync.path("mode").asText("?") + "]");
                    statusLabel.setText("Данные обновлены");
                });
                loadReceipts();
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("Ошибка: " + ex.getMessage()));
            }
        });
    }

    private void loadReceipts() {
        runAsync(() -> {
            try {
                JsonNode page = api.receipts(null, markedOnly.isSelected(), nameFilter.getText(), 0);
                var list = FXCollections.<Map<String, Object>>observableArrayList();
                if (page.has("content")) {
                    for (JsonNode n : page.get("content")) {
                        list.add(apiMapper(n));
                    }
                }
                Platform.runLater(() -> receiptsTable.setItems(list));
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("Ошибка чеков: " + ex.getMessage()));
            }
        });
    }

    private void exportCurrent() {
        String type = switch (tabs.getSelectionModel().getSelectedIndex()) {
            case 1 -> "trade-points";
            case 2 -> "groups";
            case 3 -> "matrix";
            default -> "trade-points";
        };
        FileChooser fc = new FileChooser();
        fc.setInitialFileName("ofd-" + type + ".xlsx");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        File file = fc.showSaveDialog(getScene().getWindow());
        if (file == null) return;
        runAsync(() -> {
            try {
                byte[] data = api.export(type);
                Files.write(file.toPath(), data);
                Platform.runLater(() -> statusLabel.setText("Экспорт: " + file.getName()));
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("Экспорт ошибка: " + ex.getMessage()));
            }
        });
    }

    private static Map<String, Object> apiMapper(JsonNode n) {
        return new com.fasterxml.jackson.databind.ObjectMapper()
                .convertValue(n, new com.fasterxml.jackson.core.type.TypeReference<>() {});
    }

    private static String pretty(JsonNode n) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .writerWithDefaultPrettyPrinter().writeValueAsString(n);
        } catch (Exception e) {
            return n.toString();
        }
    }

    private static void runAsync(ThrowingRunnable r) {
        Thread t = new Thread(() -> {
            try {
                r.run();
            } catch (Exception e) {
                Platform.runLater(() -> {
                    Alert a = new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK);
                    a.setHeaderText("Ошибка");
                    a.showAndWait();
                });
            }
        }, "ui-async");
        t.setDaemon(true);
        t.start();
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static Tab tab(String title, javafx.scene.Node content) {
        Tab t = new Tab(title, content);
        t.setClosable(false);
        return t;
    }

    private static Label title(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("screen-title");
        return l;
    }

    private static BorderPane wrap(TableView<?> table, String titleText) {
        BorderPane p = new BorderPane();
        VBox top = new VBox(title(titleText));
        top.setPadding(new Insets(16));
        p.setTop(top);
        p.setCenter(table);
        BorderPane.setMargin(table, new Insets(0, 16, 16, 16));
        return p;
    }

    @SafeVarargs
    private static void setupColumns(TableView<Map<String, Object>> table, String... specs) {
        table.getColumns().clear();
        for (String spec : specs) {
            String[] p = spec.split(":", 2);
            TableColumn<Map<String, Object>, String> col = new TableColumn<>(p[1]);
            col.setCellValueFactory(cd -> {
                Object v = cd.getValue().get(p[0]);
                if (v instanceof Number n && p[0].toLowerCase().contains("percent")) {
                    return new SimpleStringProperty(String.format("%.1f", n.doubleValue()));
                }
                return new SimpleStringProperty(v == null ? "" : String.valueOf(v));
            });
            col.setPrefWidth(160);
            table.getColumns().add(col);
        }
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private static TableView<Map<String, Object>> mapTable() {
        TableView<Map<String, Object>> t = new TableView<>();
        t.setPlaceholder(new Label("Нет данных"));
        return t;
    }
}
