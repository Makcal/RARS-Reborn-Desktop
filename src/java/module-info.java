module com.example.rarsreborn {
    requires javafx.controls;
    requires javafx.fxml;
    requires rars.reborn.core;


    opens rarsreborn to javafx.fxml;
    exports rarsreborn;
}