package com.example.maturitni_prace;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class SipkyController {

    public static class Row {
        final String name;
        int remaining;
        int turns;
        int lastScore;
        int totalScored;
        int roundscount;
        final Deque<Integer> history = new ArrayDeque<>();

        Row(String name, int start) {
            this.name = name;
            this.remaining = start;
        }

        String average() {
            return turns == 0 ? "-" : String.format("%.1f", (double) totalScored / turns);
        }
    }

    @FXML
    private Label titleLabel, currentLabel, infoLabel, roundLabel, remainingLabel, avgLabel, lastLabel;
    @FXML
    private VBox setupBox;
    @FXML
    private HBox gameBox, othersBox;
    @FXML
    private TextField nameField, scoreField;
    @FXML
    private ListView<String> playerList;
    @FXML
    private ComboBox<Integer> modeBox;

    private final List<String> players = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private final Deque<Integer> playerOrder = new ArrayDeque<>(); // pro tlačítko Zpět
    private int current = 0;
    private boolean finished = false;

    @FXML
    private void initialize() {
        modeBox.setItems(FXCollections.observableArrayList(121, 301, 501));
        modeBox.setValue(301);
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
        if (modeBox.getValue() != 121) {
            r.turns++;
        } else {
            r.roundscount++;
        }
        int start = modeBox.getValue();
        if (start == 121 && r.roundscount == 3) {
            r.remaining = 121;
            r.lastScore = 0;
            r.totalScored = 0;
            r.turns++;
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
        Row r = rows.get(current);

        // číslo kola = kolikátý celý průchod všemi hráči se právě hraje
        int round = playerOrder.size() / rows.size() + 1;
        roundLabel.setText("Kolo: " + round);

        currentLabel.setText(finished ? "Vyhrál " + r.name : r.name);
        remainingLabel.setText(String.valueOf(r.remaining));
        avgLabel.setText(r.average());
        lastLabel.setText(r.history.isEmpty() ? "-" : String.valueOf(r.lastScore));

        // ostatní hráči v pořadí, v jakém půjdou na řadu
        othersBox.getChildren().clear();
        for (int i = 1; i < rows.size(); i++) {
            Row o = rows.get((current + i) % rows.size());
            Label name = new Label(o.name);
            Label rem = new Label(String.valueOf(o.remaining));
            String style = "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: black;";
            name.setStyle(style);
            rem.setStyle(style);
            VBox box = new VBox(2, name, rem);
            box.setAlignment(Pos.CENTER);
            othersBox.getChildren().add(box);
        }
        boolean hasOthers = rows.size() > 1;
        othersBox.setVisible(hasOthers);
        othersBox.setManaged(hasOthers);
    }
}