package com.example.maturitni_prace;
import javafx.fxml.FXML;

public class MenuController {

    @FXML
    private void openDarts() {
        MainApplication.show("darts.fxml");
    }

    @FXML
    private void openBowling() {
        MainApplication.show("bowling.fxml");
    }
}
