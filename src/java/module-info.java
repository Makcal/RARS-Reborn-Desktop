module com.example.rarsreborn {
    requires javafx.controls;
    requires javafx.fxml;


    opens rarsreborn to javafx.fxml;
    exports rarsreborn;
}