module rarsreborn.desktop {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;
    requires rarsreborn.core;

    opens rarsreborn to javafx.fxml;
    exports rarsreborn;
}