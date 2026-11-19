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
        int attempts;  // počet využitých pokusů v režimu 121 (max 3)
        int checkouts; // počet úspěšných zavření 121
        final Deque<Integer> history = new ArrayDeque<>();

        Row(String name, int start) {
            this.name = name;
            this.remaining = start;
        }

        String average() {
            return turns == 0 ? "-" : String.format("%.1f", (double) totalScored / turns);
        }
    }

    private static class TurnSnapshot {
        final int playerIndex;
        final int prevRemaining;
        final int prevLastScore;
        final int prevTotalScored;
        final int prevTurns;
        final int prevAttempts;
        final int prevCheckouts;

        TurnSnapshot(int playerIndex, Row r) {
            this.playerIndex = playerIndex;
            this.prevRemaining = r.remaining;
            this.prevLastScore = r.lastScore;
            this.prevTotalScored = r.totalScored;
            this.prevTurns = r.turns;
            this.prevAttempts = r.attempts;
            this.prevCheckouts = r.checkouts;
        }
    }

    @FXML
    private Label titleLabel, currentLabel, attemptsLabel, checkoutsLabel, infoLabel, roundLabel, remainingLabel, avgLabel, lastLabel;
    @FXML
    private HBox checkoutsBox;
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
    private final Deque<TurnSnapshot> undoStack = new ArrayDeque<>();
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
        undoStack.clear();
        for (String p : players) {
            rows.add(new Row(p, start));
        }
        current = 0;
        finished = false;
        setInfoError("");
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
            setInfoError("Zadej celé číslo.");
            return;
        }
        if (score < 0 || score > 180) {
            setInfoError("Za jedno kolo lze získat 0–180 bodů.");
            return;
        }

        int start = modeBox.getValue();
        Row r = rows.get(current);
        undoStack.push(new TurnSnapshot(current, r));

        int after = r.remaining - score;
        int applied;
        boolean busted = (after < 0 || after == 1);

        if (busted) {
            applied = 0; // přehození – kolo se nezapočítá
            setInfoError(r.name + " přehodil – kolo se nezapočítává.");
        } else {
            applied = score;
            setInfoError("");
            r.remaining = after;
        }

        r.history.push(applied);
        r.lastScore = applied;
        r.totalScored += applied;
        r.turns++;
        r.attempts++;
        scoreField.clear();

        // Hráč dosáhl přesně 0
        if (r.remaining == 0) {
            if (start == 121) {
                r.checkouts++;
                r.remaining = 121;
                r.attempts = 0;
                setInfoSuccess("🎯 " + r.name + " zavřel 121! (Zavřeno: " + r.checkouts + "×)");
                current = (current + 1) % rows.size();
                refresh();
                scoreField.requestFocus();
                return;
            } else {
                finished = true;
                refresh();
                new Alert(Alert.AlertType.INFORMATION,
                        "Vyhrál " + r.name + " za " + r.turns + " kol! 🎉").showAndWait();
                return;
            }
        }

        // Pravidlo pro 121: max 3 pokusy, jinak reset zpět na 121
        if (start == 121 && r.attempts >= 3) {
            r.remaining = 121;
            r.attempts = 0;
            setInfoError(r.name + " nestihl 121 na 3 pokusy – začíná znovu od 121!");
        }

        current = (current + 1) % rows.size();
        refresh();
        scoreField.requestFocus();
    }

    @FXML
    private void onUndo() {
        if (undoStack.isEmpty()) return;
        TurnSnapshot snap = undoStack.pop();
        Row r = rows.get(snap.playerIndex);

        if (!r.history.isEmpty()) {
            r.history.pop();
        }
        r.remaining = snap.prevRemaining;
        r.lastScore = snap.prevLastScore;
        r.totalScored = snap.prevTotalScored;
        r.turns = snap.prevTurns;
        r.attempts = snap.prevAttempts;
        r.checkouts = snap.prevCheckouts;

        current = snap.playerIndex;
        finished = false;
        setInfoError("");
        refresh();
        scoreField.requestFocus();
    }

    private void setInfoError(String msg) {
        infoLabel.getStyleClass().remove("ng-info-success");
        infoLabel.setText(msg);
    }

    private void setInfoSuccess(String msg) {
        if (!infoLabel.getStyleClass().contains("ng-info-success")) {
            infoLabel.getStyleClass().add("ng-info-success");
        }
        infoLabel.setText(msg);
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
        boolean is121 = modeBox.getValue() != null && modeBox.getValue() == 121;

        // číslo kola = kolikátý celý průchod všemi hráči se právě hraje
        int round = undoStack.size() / rows.size() + 1;
        roundLabel.setText("Kolo: " + round);

        currentLabel.setText(finished ? "Vyhrál " + r.name : r.name);
        remainingLabel.setText(String.valueOf(r.remaining));
        avgLabel.setText(r.average());
        lastLabel.setText(r.history.isEmpty() ? "-" : String.valueOf(r.lastScore));

        // počítadlo zavření nad políčkem pro skóre (režim 121)
        checkoutsBox.setVisible(is121);
        checkoutsBox.setManaged(is121);
        if (is121) {
            checkoutsLabel.setText(r.checkouts + "×");
        }

        // ukazatel pokusů pro režim 121
        attemptsLabel.setVisible(is121 && !finished);
        attemptsLabel.setManaged(is121 && !finished);
        if (is121 && !finished) {
            int currentAttempt = r.attempts + 1;
            if (currentAttempt == 3) {
                attemptsLabel.setText("Pokus: 3 / 3 (poslední!)");
                attemptsLabel.setStyle("-fx-text-fill: #ff758f; -fx-border-color: rgba(255, 117, 143, 0.55); -fx-background-color: rgba(255, 117, 143, 0.16);");
            } else {
                attemptsLabel.setText("Pokus: " + currentAttempt + " / 3");
                attemptsLabel.setStyle("");
            }
        }

        // ostatní hráči v pořadí, v jakém půjdou na řadu
        othersBox.getChildren().clear();
        for (int i = 1; i < rows.size(); i++) {
            Row o = rows.get((current + i) % rows.size());
            String headerText = is121
                    ? o.name + " (" + (o.attempts + 1) + "/3) ·  " + o.checkouts + "×"
                    : o.name;
            Label name = new Label(headerText);
            Label rem = new Label(String.valueOf(o.remaining));
            name.getStyleClass().add("ng-other-name");
            rem.getStyleClass().add("ng-other-score");

            VBox box = new VBox(2, name, rem);
            box.setAlignment(Pos.CENTER);
            box.getStyleClass().add("ng-other-card");
            othersBox.getChildren().add(box);
        }
        boolean hasOthers = rows.size() > 1;
        othersBox.setVisible(hasOthers);
        othersBox.setManaged(hasOthers);
    }
}
