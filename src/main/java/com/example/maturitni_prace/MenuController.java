package com.example.maturitni_prace;
import javafx.fxml.FXML;

public class MenuController {

    @FXML
    private void sipky() {
        MainApplication.show("sipky.fxml");
    }

    @FXML
    private void kuzelky() {
        MainApplication.show("kuzelky.fxml");
    }

}
