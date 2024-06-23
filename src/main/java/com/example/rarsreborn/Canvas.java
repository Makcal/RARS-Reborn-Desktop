package com.example.rarsreborn;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

// 4D5156 - Light grey
// 2B2D30 - Dark grey

public class Canvas extends Application {
    Pane root = new Pane();

    // Panels creating
    Rectangle codePart = createEditorPart();
    Rectangle terminalPart = createTerminalPart();
    Rectangle managePanel = createManagePanel();
    Rectangle statePanel = createStatePanel();
    Rectangle registersPart = createRegistersPart();

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

    public Canvas() {

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
        } else {
            editButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #2B2D30");
            executeButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #4D5156");
        }
    }
    private void changeRegistersPanelColor(Button clickedButton) {
        if (clickedButton == registerButton) {
            registerButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #4D5156");
            floatingButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #2B2D30");
            statusButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #2B2D30");
        } else if(clickedButton == floatingButton){
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
        registersButton.setLayoutX(1025.0);
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
        floatingButton.setLayoutX(1092.0);
        floatingButton.setScaleY(1.15);
        floatingButton.setScaleX(1.0);
        floatingButton.setLayoutY(45.0);
        root.getChildren().add(floatingButton);
        return floatingButton;
    }

    private Button createControlStatusButton() {
        Button controlStatusButton = new Button("Control and status");
        controlStatusButton.setStyle("-fx-border-color: black; -fx-border-width: 1; -fx-background-color: #2B2D30");
        controlStatusButton.setTextFill(Color.WHITE);
        controlStatusButton.setOnAction(event -> changeRegistersPanelColor(controlStatusButton));
        controlStatusButton.setLayoutX(1176.0);
        controlStatusButton.setScaleY(1.15);
        controlStatusButton.setScaleX(0.85);
        controlStatusButton.setLayoutY(45.0);
        root.getChildren().add(controlStatusButton);
        return controlStatusButton;
    }

    private Button createRunButton() {
        Button runButton = new Button("Run");
        runButton.setStyle("-fx-border-color: black; -fx-border-width: 1; -fx-background-color: #2B2D30");
        runButton.setTextFill(Color.GREENYELLOW);
//        runButton.setOnAction(this::handleClickRunBtn);

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
        } else if(clickedButton == debugButton){
            runButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #2B2D30");
            debugButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #4D5156");
            saveButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #2B2D30");
            stopButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #2B2D30");
        } else if(clickedButton == saveButton){
            runButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #2B2D30");
            runButton.setStyle("-fx-border-color: black; -fx-border-width: 1;-fx-background-color: #2B2D30");
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
        Rectangle terminalPart = new Rectangle(1024.0, 135.0);
        terminalPart.setLayoutY(581.5);
        terminalPart.setFill(Color.web("#1E1F22"));
        terminalPart.setStroke(Color.web("#2B2D30"));
        terminalPart.setStrokeWidth(4.0);
        root.getChildren().add(terminalPart);
        return terminalPart;
    }

    private Rectangle createEditorPart() {
        Rectangle codePart = new Rectangle(1024.0, 500.0);
        codePart.setLayoutY(76.0);
        codePart.setFill(Color.web("#1E1F22"));
        codePart.setStroke(Color.web("#2B2D30"));
        codePart.setStrokeWidth(4.0);
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
        menuButton.setOnAction(this::handleClickMenuBtn);
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
    private Rectangle createRegistersPart() {
        Rectangle registersPart = new Rectangle(300.0, 650.0);
        registersPart.setLayoutX(1024.0);
        registersPart.setLayoutY(78.0);
        registersPart.setFill(Color.web("#2B2D30"));
        registersPart.setStroke(Color.web("#1E1F22"));
        registersPart.setStrokeWidth(2.0);
        root.setStyle("-fx-background-color: #" + "1E1F22");
        root.getChildren().add(registersPart);
        return registersPart;
    }

    private void handleClickEditBtn(ActionEvent event) {

        System.out.println("Edit button clicked!");
    }

    private void handleClickExecuteBtn(ActionEvent event) {
        System.out.println("Execute button clicked!");
    }

    private void handleClickRunBtn(ActionEvent event) {
        System.out.println("Run button clicked!");
    }

    private void handleClickDebugBtn(ActionEvent event) {
        System.out.println("Debug button clicked!");
    }

    private void handleClickStopBtn(ActionEvent event) {
        System.out.println("Stop button clicked!");
    }

    private void handleClickSaveBtn(ActionEvent event) {
        System.out.println("Save button clicked!");
    }

    private void handleClickRegistersBtn(ActionEvent event) {
        System.out.println("Registers button clicked!");
    }

    private void handleClickFloatingBtn(ActionEvent event) {
        System.out.println("Floating point button clicked!");
    }

    private void handleClickStatusBtn(ActionEvent event) {
        System.out.println("Control and status button clicked!");
    }
    private void handleClickMenuBtn(ActionEvent event) {
        System.out.println("Menu button clicked!");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
