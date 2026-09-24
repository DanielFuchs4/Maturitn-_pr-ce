module com.example.maturitni_prace {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.maturitni_prace to javafx.fxml;
    exports com.example.maturitni_prace;
}