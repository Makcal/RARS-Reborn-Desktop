module com.example.rarsreborn {
    requires javafx.controls;
    requires javafx.fxml;
    requires rarsreborn.core;


    opens rarsreborn to javafx.fxml;
    exports rarsreborn;
}