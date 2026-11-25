package ru.ofd.marking.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.ofd.marking.client.ApiClient;

/**
 * «ЛК приложения»: реестр агентов на RDP клиентов (имя, IP, порт, токен, вкл/выкл) и их статусы.
 */
public class AgentsPane {

    private final ApiClient api;
    private final Label statusLabel;
    private final TableView<Map<String, Object>> table = MainView.mapTable();
    private final TextField name = new TextField();
    private final TextField host = new TextField();
    private final TextField port = new TextField("18500");
    private final PasswordField token = new PasswordField();
    private final CheckBox tls = new CheckBox("HTTPS (TLS)");
    private final CheckBox enabled = new CheckBox("Опрашивать");
    private final TextField comment = new TextField();
    private final Label tokenHint = new Label();
    private final TextArea details = new TextArea();
    private Long editingId;

    public AgentsPane(ApiClient api, Label statusLabel) {
        this.api = api;
        this.statusLabel = statusLabel;
    }

    public BorderPane build() {
        MainView.setupColumns(table, "name:Агент", "baseUrl:Адрес", "stateRu:Состояние", "lastSuccessAt:Последний опрос",
                "lagMinutes:Отставание, мин", "pendingOnAgent:Не забрано", "receiptsInCentral:Чеков в центре",
                "kktsInCentral:ККТ", "innsText:ИНН", "agentMode:Режим", "lastErrorText:Ошибка");
        table.getSelectionModel().selectedItemProperty().addListener((obs, o, row) -> fill(row));

        name.setPromptText("например, РДП Самара");
        host.setPromptText("IP агента, например 10.10.50.21");
        port.setPrefColumnCount(6);
        token.setPromptText("токен из agent.yml или data\\agent-token.txt");
        comment.setPromptText("комментарий");
        enabled.setSelected(true);

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        form.addRow(0, new Label("Имя"), name, new Label("Порт"), port);
        form.addRow(1, new Label("IP / хост"), host, tls, enabled);
        form.addRow(2, new Label("Токен"), token, tokenHint);
        form.addRow(3, new Label("Комментарий"), comment);
        GridPane.setColumnSpan(comment, 3);
        name.setPrefWidth(260);
        host.setPrefWidth(260);

        Button add = new Button("Новый");
        add.setOnAction(e -> clear());
        Button save = new Button("Сохранить");
        save.setOnAction(e -> save());
        Button del = new Button("Удалить");
        del.setOnAction(e -> delete());
        Button test = new Button("Проверить связь");
        test.setOnAction(e -> withSelected(id -> show(api.testAgent(id))));
        Button poll = new Button("Опросить сейчас");
        poll.setOnAction(e -> withSelected(id -> {
            show(api.pollAgent(id));
            refresh();
        }));
        Button reset = new Button("Перечитать всё");
        reset.setTooltip(new Tooltip("Сбросить курсор: центр заново заберёт все чеки агента (дубли не появятся)"));
        reset.setOnAction(e -> withSelected(id -> {
            show(api.resetAgentCursor(id));
            refresh();
        }));
        Button refreshBtn = new Button("Обновить");
        refreshBtn.setOnAction(e -> refresh());
        HBox buttons = new HBox(8, add, save, del, new Separator(), test, poll, reset, refreshBtn);

        details.setEditable(false);
        details.setPrefRowCount(7);
        details.setPromptText("Результат проверки связи / опроса");

        Label hint = new Label("Центр сам обращается к агентам. На агенте в agent.allowed-ips должен быть IP центра, "
                + "токен в карточке должен совпадать с agent.token. Ключ ОФД хранится только на агенте.");
        hint.setWrapText(true);
        hint.getStyleClass().add("hint");

        VBox top = new VBox(8, MainView.title("Агенты (ЛК приложения)"), hint);
        top.setPadding(new Insets(16));
        VBox bottom = new VBox(10, form, buttons, details);
        bottom.setPadding(new Insets(12, 16, 16, 16));
        BorderPane p = new BorderPane();
        p.setTop(top);
        p.setCenter(table);
        p.setBottom(bottom);
        BorderPane.setMargin(table, new Insets(0, 16, 0, 16));
        return p;
    }

    public void refresh() {
        MainView.runAsync(() -> {
            List<Map<String, Object>> rows = api.agents();
            for (Map<String, Object> r : rows) {
                r.put("stateRu", stateRu(String.valueOf(r.get("state"))));
                Object inns = r.get("inns");
                r.put("innsText", inns instanceof List<?> l ? String.join(", ", l.stream().map(String::valueOf).toList()) : "");
                Object err = r.get("lastError");
                r.put("lastErrorText", err == null ? "" : err);
            }
            Platform.runLater(() -> {
                Long keep = editingId;
                table.setItems(FXCollections.observableArrayList(rows));
                if (keep != null) {
                    rows.stream().filter(r -> keep.equals(num(r.get("id")))).findFirst()
                            .ifPresent(r -> table.getSelectionModel().select(r));
                }
            });
        });
    }

    static String stateRu(String s) {
        return switch (s) {
            case "ONLINE" -> "● на связи";
            case "OFFLINE" -> "○ нет связи";
            case "ERROR" -> "✖ ошибка";
            case "DISABLED" -> "выключен";
            case "NEW" -> "ещё не опрашивался";
            default -> s;
        };
    }

    private void fill(Map<String, Object> row) {
        if (row == null) return;
        editingId = num(row.get("id"));
        name.setText(str(row.get("name")));
        host.setText(str(row.get("host")));
        port.setText(str(row.get("port")));
        tls.setSelected(Boolean.TRUE.equals(row.get("useTls")));
        enabled.setSelected(Boolean.TRUE.equals(row.get("enabled")));
        comment.setText(str(row.get("comment")));
        token.clear();
        tokenHint.setText(Boolean.TRUE.equals(row.get("tokenSet"))
                ? "задан (" + str(row.get("tokenHint")) + "), пусто — не менять" : "не задан");
    }

    private void clear() {
        editingId = null;
        table.getSelectionModel().clearSelection();
        name.clear();
        host.clear();
        port.setText("18500");
        token.clear();
        tls.setSelected(false);
        enabled.setSelected(true);
        comment.clear();
        tokenHint.setText("");
        details.clear();
    }

    private void save() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name.getText().trim());
        body.put("host", host.getText().trim());
        try {
            body.put("port", Integer.parseInt(port.getText().trim()));
        } catch (NumberFormatException e) {
            statusLabel.setText("Порт должен быть числом");
            return;
        }
        body.put("useTls", tls.isSelected());
        body.put("enabled", enabled.isSelected());
        body.put("comment", comment.getText());
        body.put("token", token.getText());
        Long id = editingId;
        MainView.runAsync(() -> {
            JsonNode res = id == null ? api.createAgent(body) : api.updateAgent(id, body);
            Platform.runLater(() -> {
                editingId = res.path("id").asLong();
                token.clear();
                statusLabel.setText("Агент «" + res.path("name").asText() + "» сохранён");
            });
            refresh();
        });
    }

    private void delete() {
        if (editingId == null) return;
        Long id = editingId;
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "Удалить агента «" + name.getText() + "»? Уже загруженные чеки останутся в центре.",
                ButtonType.OK, ButtonType.CANCEL);
        a.setHeaderText("Удаление агента");
        if (a.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        MainView.runAsync(() -> {
            api.deleteAgent(id);
            Platform.runLater(this::clear);
            refresh();
        });
    }

    private void withSelected(MainView.ThrowingRunnableWithId action) {
        if (editingId == null) {
            statusLabel.setText("Выберите агента в таблице");
            return;
        }
        long id = editingId;
        details.setText("Выполняется…");
        MainView.runAsync(() -> action.run(id));
    }

    private void show(JsonNode n) {
        String text;
        try {
            text = new com.fasterxml.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(n);
        } catch (Exception e) {
            text = String.valueOf(n);
        }
        String t = text;
        Platform.runLater(() -> details.setText(t));
    }

    private static Long num(Object o) {
        return o instanceof Number n ? n.longValue() : null;
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}
