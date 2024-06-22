module com.example.rarsreborn {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.rarsreborn to javafx.fxml;
    exports com.example.rarsreborn;
}