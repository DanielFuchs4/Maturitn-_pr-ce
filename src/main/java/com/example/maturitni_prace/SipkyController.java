package com.example.maturitni_prace;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Šipky: odečítání od 301/501/701. Zadává se součet za kolo (3 šipky). */
public class SipkyController {

    public static class Row {
        final String name;
        int remaining;
        int turns;
        int lastScore;
        int totalScored;
        int roundscount;
        final Deque<Integer> history = new ArrayDeque<>(); // odečtené body (0 = přehození)

        Row(String name, int start) {
            this.name = name;
            this.remaining = start;
        }

        String average() {
            return turns == 0 ? "-" : String.format("%.1f", (double) totalScored / turns);
        }
    }

    @FXML
    private Label titleLabel, currentLabel, infoLabel;
    @FXML
    private VBox setupBox;
    @FXML
    private BorderPane gameBox;
    @FXML
    private TextField nameField, scoreField;
    @FXML
    private ListView<String> playerList;
    @FXML
    private ComboBox<Integer> modeBox;
    @FXML
    private TableView<Row> table;
    @FXML
    private TableColumn<Row, String> colName, colRemaining, colLast, colAvg, colTurns;

    // --- stav hry ---
    private final List<String> players = new ArrayList<>();
    private final ObservableList<Row> rows = FXCollections.observableArrayList();
    private final Deque<Integer> playerOrder = new ArrayDeque<>(); // pro tlačítko Zpět
    private int current = 0;
    private boolean finished = false;

    @FXML
    private void initialize() {
        modeBox.setItems(FXCollections.observableArrayList(121,301, 501));
        modeBox.setValue(301);

        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().name));
        colRemaining.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().remaining)));
        colLast.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().turns == 0 ? "-" : String.valueOf(d.getValue().lastScore)));
        colAvg.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().average()));
        colTurns.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().turns)));

        table.setItems(rows);
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Row item, boolean empty) {
                super.updateItem(item, empty);
                boolean active = !empty && item != null && rows.indexOf(item) == current && !finished;
                setStyle(active ? "-fx-background-color: #cfe8ff;" : "");
            }
        });

        showSetup();
    }

    @FXML
    private void onBack() {
        MainApplication.showMenu();
    }

    @FXML
    private void onAddPlayer() {
        String n = nameField.getText().trim();
        if (!n.isEmpty()) {
            players.add(n);
            playerList.getItems().setAll(players);
            nameField.clear();
        }
    }

    @FXML
    private void onRemovePlayer() {
        int i = playerList.getSelectionModel().getSelectedIndex();
        if (i >= 0) {
            players.remove(i);
            playerList.getItems().setAll(players);
        }
    }

    @FXML
    private void onStart() {
        if (players.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Přidej alespoň jednoho hráče.").showAndWait();
            return;
        }
        int start = modeBox.getValue();
        rows.clear();
        playerOrder.clear();
        for (String p : players) rows.add(new Row(p, start));
        current = 0;
        finished = false;
        infoLabel.setText("");
        titleLabel.setText("🎯 Šipky – " + start);
        setupBox.setVisible(false);
        setupBox.setManaged(false);
        gameBox.setVisible(true);
        gameBox.setManaged(true);
        refresh();
        scoreField.requestFocus();
    }

    @FXML
    private void onNewGame() {
        showSetup();
    }

    @FXML
    private void onSubmit() {
        if (finished) return;
        int score;
        try {
            score = Integer.parseInt(scoreField.getText().trim());
        } catch (NumberFormatException ex) {
            infoLabel.setText("Zadej celé číslo.");
            return;
        }
        if (score < 0 || score > 180) {
            infoLabel.setText("Za jedno kolo lze získat 0–180 bodů.");
            return;
        }

        Row r = rows.get(current);
        int after = r.remaining - score;
        int applied;
        if (after < 0 || after == 1) {
            applied = 0; // přehození – kolo se nezapočítá
            infoLabel.setText(r.name + " přehodil – kolo se nezapočítává.");
        } else {
            applied = score;
            infoLabel.setText("");
            r.remaining = after;
        }
        r.history.push(applied);
        r.lastScore = applied;
        r.totalScored += applied;
        if (modeBox.getValue() != 121){
            r.turns++;
        } else {
            r.roundscount++;
        }
        int start = modeBox.getValue();
        if (start == 121 && r.roundscount == 3){
            r.remaining = 121;
            r.lastScore = 0;
            r.totalScored = 0;
            r.turns ++;
            r.roundscount = 0;

        }
        playerOrder.push(current);
        scoreField.clear();

        if (r.remaining == 0) {
            finished = true;
            refresh();
            new Alert(Alert.AlertType.INFORMATION,
                    "Vyhrál " + r.name + " za " + r.turns + " kol! 🎉").showAndWait();
            return;
        }
        current = (current + 1) % rows.size();
        refresh();
        scoreField.requestFocus();
    }

    @FXML
    private void onUndo() {
        if (playerOrder.isEmpty()) return;
        int idx = playerOrder.pop();
        Row r = rows.get(idx);
        int applied = r.history.pop();
        r.remaining += applied;
        r.totalScored -= applied;
        r.turns--;
        r.lastScore = r.history.isEmpty() ? 0 : r.history.peek();
        current = idx;
        finished = false;
        infoLabel.setText("");
        refresh();
    }

    private void showSetup() {
        titleLabel.setText("🎯 Šipky – nová hra");
        gameBox.setVisible(false);
        gameBox.setManaged(false);
        setupBox.setVisible(true);
        setupBox.setManaged(true);
    }

    private void refresh() {
        table.refresh();
        currentLabel.setText(finished ? "Konec hry" : "Na řadě: " + rows.get(current).name);
    }
}