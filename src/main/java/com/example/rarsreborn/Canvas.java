package com.example.rarsreborn;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.DirectionalLight;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

// 4D5156 - Light grey
// 2B2D30 - Dark grey

public class Canvas extends Application {
    AnchorPane root = new AnchorPane();

    // Panels creating
    Rectangle codePart = createEditorPart();
    Rectangle executeWindow = createExecuteWindow();
    Rectangle terminalPart = createTerminalPart();
    Rectangle managePanel = createManagePanel();
    Rectangle statePanel = createStatePanel();
    Rectangle registersPart = createRegistersPart();
    Rectangle memoryWindow = createMemoryWindow();
    TextArea codeTextBox = createCodeTextBox();
    Label memoryText;
    Label executeText;

    // Buttons creating
    Button editButton = createEditButton();
    Button executeButton = createExecuteButton();
    Button registerButton = createRegistersButton();
    Button floatingButton = createFloatingPointButton();
    Button statusButton = createControlStatusButton();
    Button menuButton = createMenuButton();
    Button runButton = createRunButton();
    Button debugButton = createDebugButton();
    Button stopButton = createStopButton();
    Button saveButton = createSaveButton();
    Rectangle menuPanel = menuPanel();
    private boolean menuState = false;

    public Canvas() {

    }
    public TextArea createCodeTextBox() {
        TextArea code = new TextArea();
        code.setLayoutY(76);
        code.setPrefWidth(924);
        code.setPrefHeight(500);

        code.setStyle("-fx-control-inner-background: #1E1F22; -fx-text-fill: white; -fx-font-size: 14pt;" +
                "-fx-border-color:black; -fx-border-width: 2px; -fx-background-color: #2B2D30;");
        root.getChildren().add(code);

        return code;
    }

    public void start(Stage primaryStage) {
        Scene scene = new Scene(root, 1280.0, 720.0);
        primaryStage.setTitle("Initial");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Button createEditButton() {
        Button editButton = new Button("Edit");
        editButton.setStyle("-fx-border-color: black; -fx-border-width: 1; -fx-background-color: #4D5156");
        editButton.setTextFill(Color.WHITE);
        editButton.setOnAction(event -> changeEditExecuteColor(editButton));
        editButton.setScaleX(1.4);
        editButton.setScaleY(1.2);
        editButton.setLayoutX(7.0);
        editButton.setLayoutY(45.0);
        root.getChildren().add(editButton);
        return editButton;
    }

    private Button createExecuteButton() {
        Button executeButton = new Button("Execute");
        executeButton.setStyle("-fx-border-color: black; -fx-border-width: 1; -fx-background-color: #2B2D30");
        executeButton.setTextFill(Color.WHITE);
        executeButton.setOnAction(event -> changeEditExecuteColor(executeButton));
        executeButton.setScaleX(1.1);
        executeButton.setScaleY(1.2);
        executeButton.setLayoutX(55.0);
        executeButton.setLayoutY(45.0);
        root.getChildren().add(executeButton);
        return executeButton;
    }

    private void changeEditExecuteColor(Button clickedButton) {
        if (clickedButton == editButton) {
            editButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #4D5156");
            executeButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #2B2D30");
            closeExecuteWindow();
            openEditWindow();
            closeMemoryWindow();
            openTerminalPart();
            codeTextBox.setVisible(true);
        } else {
            editButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #2B2D30");
            executeButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #4D5156");
            openExecuteWindow();
            openMemoryWindow();
            closeEditWindow();
            closeTerminalPart();
            codeTextBox.setVisible(false);
        }
    }
    private Rectangle createExecuteWindow() {
        Rectangle executePart = new Rectangle(920, 300.0);
        executePart.setLayoutY(76.0);
        executePart.setFill(Color.web("#1E1F22"));
        executePart.setStroke(Color.web("#2B2D30"));
        executePart.setStrokeWidth(4.0);
        executePart.setVisible(false);
        root.getChildren().add(executePart);

        executeText = new Label();
        executeText.setText("Text segment");
        executeText.setStyle("-fx-font-size: 25px;");
        executeText.setTextFill(Color.WHITE);
        executeText.setLayoutX(4);
        executeText.setLayoutY(76);
        executeText.setVisible(false);
        root.getChildren().add(executeText);

        return executePart;
    }
    private Rectangle createMemoryWindow() {
        Rectangle memoryWindow = new Rectangle(920, 420);
        memoryWindow.setLayoutX(3);
        memoryWindow.setLayoutY(381);
        memoryWindow.setFill(Color.web("#2B2D30"));
        memoryWindow.setStroke(Color.web("#1E1F22"));
        memoryWindow.setStrokeWidth(0.5);
        memoryWindow.setVisible(false);
        root.getChildren().add(memoryWindow);

        memoryText = new Label();
        memoryText.setText("Data segment");
        memoryText.setStyle("-fx-font-size: 25px;");
        memoryText.setTextFill(Color.WHITE);
        memoryText.setLayoutX(4);
        memoryText.setLayoutY(383);
        memoryText.setVisible(false);
        root.getChildren().add(memoryText);
        return memoryWindow;
    }
    private void openTerminalPart() {
        terminalPart.setVisible(true);
    }
    private void closeTerminalPart() {
        terminalPart.setVisible(false);
    }
    private void openMemoryWindow(){
        memoryWindow.setVisible(true);
        memoryText.setVisible(true);
    }
    private void closeMemoryWindow(){
        memoryWindow.setVisible(false);
        memoryText.setVisible(false);
    }
    private void openEditWindow() {
        codePart.setVisible(true);
    }
    private void closeEditWindow() {
        codePart.setVisible(false);
    }
    private void openExecuteWindow() {
        executeWindow.setVisible(true);
        executeText.setVisible(true);
    }
    private void closeExecuteWindow() {
        executeWindow.setVisible(false);
        executeText.setVisible(false);
    }

    private void changeRegistersPanelColor(Button clickedButton) {
        if (clickedButton == registerButton) {
            registerButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #4D5156");
            floatingButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #2B2D30");
            statusButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #2B2D30");
        } else if (clickedButton == floatingButton) {
            floatingButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #4D5156");
            registerButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #2B2D30");
            statusButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #2B2D30");
        } else {
            statusButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #4D5156");
            registerButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #2B2D30");
            floatingButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #2B2D30");
        }
    }


    private Button createRegistersButton() {
        Button registersButton = new Button("Registers");
        registersButton.setStyle("-fx-border-color: black; -fx-border-width: 1; -fx-background-color: #2B2D30");
        registersButton.setTextFill(Color.WHITE);
        registersButton.setOnAction(event -> changeRegistersPanelColor(registersButton));
        registersButton.setLayoutX(940.0);
        registersButton.setScaleX(1.5);
        registersButton.setScaleY(1.15);
        registersButton.setLayoutY(45.0);
        root.getChildren().add(registersButton);
        return registersButton;
    }

    private Button createFloatingPointButton() {
        Button floatingButton = new Button("Floating point");
        floatingButton.setStyle("-fx-border-color: black; -fx-border-width: 1; -fx-background-color: #2B2D30");
        floatingButton.setTextFill(Color.WHITE);
        floatingButton.setOnAction(event -> changeRegistersPanelColor(floatingButton));
        floatingButton.setLayoutX(1027.0);
        floatingButton.setScaleX(1.05);
        floatingButton.setScaleY(1.15);
        floatingButton.setLayoutY(45.0);
        root.getChildren().add(floatingButton);
        return floatingButton;
    }

    private Button createControlStatusButton() {
        Button controlStatusButton = new Button("Control and status");
        controlStatusButton.setStyle("-fx-border-color: black; -fx-border-width: 1; -fx-background-color: #2B2D30");
        controlStatusButton.setTextFill(Color.WHITE);
        controlStatusButton.setOnAction(event -> changeRegistersPanelColor(controlStatusButton));
        controlStatusButton.setLayoutX(1143.0);
        controlStatusButton.setScaleX(1.34);
        controlStatusButton.setScaleY(1.15);
        controlStatusButton.setLayoutY(45.0);
        root.getChildren().add(controlStatusButton);
        return controlStatusButton;
    }

    private Button createRunButton() {
        Button runButton = new Button("Run");
        runButton.setStyle("-fx-border-color: black; -fx-border-width: 1; -fx-background-color: #2B2D30");
        runButton.setTextFill(Color.GREENYELLOW);
        runButton.setOnAction(event -> changeTopPanelButtonsColor(runButton));
        runButton.setLayoutX(50.0);
        runButton.setLayoutY(7.0);
        root.getChildren().add(runButton);
        return runButton;
    }

    private Button createDebugButton() {
        Button debugButton = new Button("Debug");
        debugButton.setStyle("-fx-border-color: black; -fx-border-width: 1; -fx-background-color: #2B2D30");
        debugButton.setTextFill(Color.GREENYELLOW);
        debugButton.setOnAction(event -> changeTopPanelButtonsColor(debugButton));
        debugButton.setLayoutX(95.0);
        debugButton.setLayoutY(7.0);
        root.getChildren().add(debugButton);
        return debugButton;
    }

    private Button createStopButton() {
        Button stopButton = new Button("Stop");
        stopButton.setStyle("-fx-border-color: black; -fx-border-width: 1; -fx-background-color: #2B2D30");
        stopButton.setTextFill(Color.RED);
        stopButton.setOnAction(event -> changeTopPanelButtonsColor(stopButton));
        stopButton.setLayoutX(155.0);
        stopButton.setLayoutY(7.0);
        root.getChildren().add(stopButton);
        return stopButton;
    }

    private Button createSaveButton() {
        Button saveButton = new Button("Save");
        saveButton.setStyle("-fx-border-color: black; -fx-border-width: 1; -fx-background-color: #2B2D30");
        saveButton.setTextFill(Color.MEDIUMPURPLE);
        saveButton.setOnAction(event -> changeTopPanelButtonsColor(saveButton));
        saveButton.setLayoutX(205.0);
        saveButton.setLayoutY(7.0);
        root.getChildren().add(saveButton);
        return saveButton;
    }

    private void changeTopPanelButtonsColor(Button clickedButton) {
        if (clickedButton == runButton) {
            runButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #4D5156");
            debugButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #2B2D30");
            saveButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #2B2D30");
            stopButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #2B2D30");
        } else if (clickedButton == debugButton) {
            runButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #2B2D30");
            debugButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #4D5156");
            saveButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #2B2D30");
            stopButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #2B2D30");
        } else if (clickedButton == saveButton) {
            runButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #2B2D30");
            debugButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #2B2D30");
            saveButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #4D5156");
            stopButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #2B2D30");
        } else {
            runButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #2B2D30");
            debugButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #2B2D30");
            saveButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #2B2D30");
            stopButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #4D5156");
        }
    }

    private Rectangle createTerminalPart() {
        Rectangle terminalPart = new Rectangle(924.0, 135.0);
        terminalPart.setLayoutY(581.5);
        terminalPart.setFill(Color.web("#1E1F22"));
        terminalPart.setStroke(Color.web("#2B2D30"));
        terminalPart.setStrokeWidth(4.0);
        terminalPart.setVisible(true);
        root.getChildren().add(terminalPart);
        return terminalPart;
    }

    private Rectangle createEditorPart() {
        Rectangle codePart = new Rectangle(924.0, 500.0);
        codePart.setLayoutY(76.0);
        codePart.setFill(Color.web("#1E1F22"));
        codePart.setStroke(Color.web("#2B2D30"));
        codePart.setStrokeWidth(4.0);
        codePart.setVisible(true);
        root.getChildren().add(codePart);
        return codePart;
    }

    private Rectangle createManagePanel() {
        Rectangle managePanel = new Rectangle(1280.0, 41.0);
        managePanel.setFill(Color.web("#2B2D30"));
        managePanel.setStroke(Color.BLACK);
        managePanel.setStrokeWidth(2.0);
        root.getChildren().add(managePanel);
        return managePanel;
    }

    private Rectangle createRegistersPart() {
        Rectangle registersPart = new Rectangle(400.0, 650.0);
        registersPart.setLayoutX(924.0);
        registersPart.setLayoutY(78.0);
        registersPart.setFill(Color.web("#2B2D30"));
        registersPart.setStroke(Color.web("#1E1F22"));
        registersPart.setStrokeWidth(2.0);
        root.setStyle("-fx-background-color: #" + "1E1F22");
        root.getChildren().add(registersPart);
        return registersPart;
    }

    private Rectangle createStatePanel() {
        Rectangle statePanel = new Rectangle(1280.0, 36.0);
        statePanel.setLayoutY(41.0);
        statePanel.setLayoutX(1.0);
        statePanel.setFill(Color.web("#2B2D30"));
        statePanel.setStroke(Color.BLACK);
        statePanel.setStrokeWidth(2.0);
        root.getChildren().add(statePanel);
        return statePanel;
    }

    private Button createMenuButton() {
        Rectangle menuBackground = new Rectangle(44.0, 40.5);
        menuBackground.setLayoutX(2.0);
        menuBackground.setFill(Color.web("#1E1F22"));
        Button menuButton = new Button();
        menuButton.setLayoutX(2);
        menuButton.setStyle("-fx-background-color: transparent;");
        menuButton.setOnAction(event -> menuManage());
        menuButton.setScaleX(3.85);
        menuButton.setScaleY(1.8);

        Rectangle topLine = new Rectangle(25.0, 3.0);
        topLine.setLayoutX(10.0);
        topLine.setLayoutY(10.0);
        topLine.setFill(Color.WHITE);
        Rectangle midLine = new Rectangle(25.0, 3.0);
        midLine.setLayoutX(10.0);
        midLine.setLayoutY(18.0);
        midLine.setFill(Color.WHITE);
        Rectangle botLine = new Rectangle(25.0, 3.0);
        botLine.setLayoutX(10.0);
        botLine.setLayoutY(26.0);
        botLine.setFill(Color.WHITE);
        root.getChildren().add(menuBackground);
        root.getChildren().add(topLine);
        root.getChildren().add(midLine);
        root.getChildren().add(botLine);
        root.getChildren().add(menuButton);
        return menuButton;

    }

    private void menuManage() {
        if (menuState == false) {
            menuState = true;
            menuPanel.setVisible(true);
        } else {
            menuState = false;
            menuPanel.setVisible(false);
        }
    }

    private Rectangle menuPanel() {
        Rectangle menu = new Rectangle(220, 400);
        menu.setLayoutY(40);
        menu.setFill(Color.web("#4D5156"));
        menu.setStroke(Color.web("#2B2D30"));
        menu.setStrokeWidth(4.0);
        root.getChildren().add(menu);
        menu.setVisible(false);
        return menu;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
