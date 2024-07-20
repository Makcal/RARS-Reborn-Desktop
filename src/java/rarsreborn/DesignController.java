package rarsreborn;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.layout.AnchorPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import rarsreborn.core.Presets;
import rarsreborn.core.core.environment.ITextInputDevice;
import rarsreborn.core.core.environment.events.*;
import rarsreborn.core.core.memory.IMemory;
import rarsreborn.core.core.register.Register32ChangeEvent;
import rarsreborn.core.core.register.Register32File;
import rarsreborn.core.core.register.Register32;
import rarsreborn.core.exceptions.execution.ExecutionException;
import rarsreborn.core.simulator.Simulator32;
import rarsreborn.core.simulator.StoppedEvent;
import rarsreborn.core.simulator.backstepper.BackStepFinishedEvent;


public class DesignController implements Initializable {
    @FXML
    private Tab base_edit_tab;

    @FXML
    private Tab base_execute_tab;

    @FXML
    private TabPane base_tab;

    @FXML
    private Button btn_break;

    @FXML
    private Button btn_debug;

    @FXML
    private Button btn_new_file;

    @FXML
    private Button btn_pause;

    @FXML
    private Button btn_resume;

    @FXML
    private Button btn_run;

    @FXML
    private Button btn_save;

    @FXML
    private Button btn_step_back;

    @FXML
    private Button btn_step_over;

    @FXML
    private TextArea console_box;

    @FXML
    private TabPane file_tab;

    @FXML
    private Tab initial_file_tab;

    @FXML
    private TextArea initial_file_text_box;
    
    @FXML
    private TableView<Register32> reg_table;
    
    @FXML
    private TableColumn<Register32, String> reg_table_name;
    
    @FXML
    private TableColumn<Register32, Integer> reg_table_num;
    
    @FXML
    private TableColumn<Register32, Integer> reg_table_value;

    
    private final Simulator32 simulator = Presets.getClassicalRiscVSimulator(new ITextInputDevice() {
        @Override
        public String requestString(int count) {
            String s = consoleScanner.readLine();
            return s.length() <= count ? s : s.substring(0, count);
        }

        @Override
        public int requestInt() {
            return 0;
        }

        @Override
        public byte requestChar() {
            return 0;
        }
    });
    private final Register32File registers = simulator.getRegisterFile();
    private final IMemory memory = simulator.getMemory();
    
    private final ObservableList<Register32> registersList = FXCollections.observableArrayList(registers.getAllRegisters());
    private final TextAreaScanner consoleScanner = new TextAreaScanner();
    private final StringBuilder consoleUneditableText = new StringBuilder();

    boolean debugMode = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        registersList.add(simulator.getProgramCounter());
        reg_table_name.setCellValueFactory(new PropertyValueFactory<>("name"));
        reg_table_num.setCellValueFactory(new PropertyValueFactory<>("number"));
        reg_table_value.setCellValueFactory(new PropertyValueFactory<>("value"));
        reg_table.setItems(registersList);
        for (Register32 r: registersList){
            r.addObserver(Register32ChangeEvent.class, (register32ChangeEvent) -> updateRegistersTable());
        }

        simulator.getExecutionEnvironment().addObserver(ConsolePrintStringEvent.class, (event) -> {
            console_box.appendText(event.text());
            consoleUneditableText.append(event.text());
        });
        simulator.getExecutionEnvironment().addObserver(ConsolePrintCharEvent.class, (event) -> {
            console_box.appendText(String.valueOf((char)event.character()));
            consoleUneditableText.append((char)event.character());
        });
        simulator.getExecutionEnvironment().addObserver(ConsolePrintIntegerEvent.class, (event) -> {
            console_box.appendText(String.valueOf(event.value()));
            consoleUneditableText.append(event.value());
        });
        simulator.getExecutionEnvironment().addObserver(ConsolePrintIntegerHexEvent.class, (event) -> {
            console_box.appendText(Integer.toHexString(event.value()));
            consoleUneditableText.append(Integer.toHexString(event.value()));
        });
        simulator.getExecutionEnvironment().addObserver(ConsolePrintIntegerOctalEvent.class, (event) -> {
            console_box.appendText(Integer.toOctalString(event.value()));
            consoleUneditableText.append(Integer.toOctalString(event.value()));
        });
        simulator.getExecutionEnvironment().addObserver(ConsolePrintIntegerBinaryEvent.class, (event) -> {
            console_box.appendText(Integer.toBinaryString(event.value()));
            consoleUneditableText.append(Integer.toBinaryString(event.value()));
        });
        simulator.getExecutionEnvironment().addObserver(ConsolePrintIntegerUnsignedEvent.class, (event) -> {
            console_box.appendText(Integer.toUnsignedString(event.value()));
            consoleUneditableText.append(Integer.toUnsignedString(event.value()));
        });
        simulator.addObserver(StoppedEvent.class, (event) -> {
            debugMode = false;
            setDebugControlsVisible(false);
            btn_break.setDisable(true);
        });
        simulator.addObserver(BackStepFinishedEvent.class, (event) -> updateRegistersTable());

        file_tab.getTabs().remove(initial_file_tab);

        console_box.setTextFormatter(new TextFormatter<String>((Change c) -> {
            String proposed = c.getControlNewText();
            if (proposed.startsWith(consoleUneditableText.toString())) {
                return  c;
            } else {
                return null ;
            }
        }));
        console_box.setEditable(false);
        console_box.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                event.consume();
                console_box.appendText("\n");
                int charPtr = console_box.getText().length() - 2;
                while (console_box.getText().charAt(charPtr) != '\n'){
                    charPtr--;
                    if (charPtr == -1){
                        charPtr = 0;
                        break;
                    }
                }
                consoleUneditableText.append(console_box.getText().substring(charPtr));
                consoleScanner.addInput(console_box.getText().substring(charPtr));
            }
        });

        setControlsDisable(true);
        setDebugControlsVisible(false);
        btn_break.setDisable(true);
    }

    @FXML
    private void CreateNewFile() {
        final String[] fileName = {""};
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("fileCreationDesign.fxml"));
            Stage stage = new Stage();
            stage.setTitle("New file");
            stage.setResizable(false);
            stage.setScene(new Scene(fxmlLoader.load(), 231, 148));
            stage.setAlwaysOnTop(true);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setOnHiding((event) -> {
                fileName[0] = ( (FileCreationDesignController) fxmlLoader.getController()).getName();
                stage.close();
            });
            stage.showAndWait();
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }
        if (!fileName[0].isEmpty()) {

            Tab newTab = new Tab(fileName[0]);
            newTab.setOnClosed(event -> {
                if (file_tab.getTabs().isEmpty()) {
                    setControlsDisable(true);
                }
            });
            file_tab.getTabs().add(newTab);

            AnchorPane newAnchorPane = new AnchorPane();
            newTab.setContent(newAnchorPane);

            TabPane newTabPane = new TabPane();
            Tab newEditTab = new Tab("EDIT");
            newEditTab.setClosable(false);
            Tab newExecuteTab = new Tab("EXECUTE");
            newExecuteTab.setClosable(false);
            newTabPane.getTabs().addAll(newEditTab, newExecuteTab);

            newAnchorPane.getChildren().add(newTabPane);
            AnchorPane.setTopAnchor(newTabPane, 0.0);
            AnchorPane.setBottomAnchor(newTabPane, 0.0);
            AnchorPane.setRightAnchor(newTabPane, 0.0);
            AnchorPane.setLeftAnchor(newTabPane, 0.0);


            AnchorPane newEditPane = new AnchorPane();
            newEditTab.setContent(newEditPane);
            TextArea newTextArea = new TextArea();
            newEditPane.getChildren().add(newTextArea);

            newTextArea.setStyle(initial_file_text_box.getStyle());
            newTextArea.setFont(initial_file_text_box.getFont());

            AnchorPane.setTopAnchor(newTextArea, 0.0);
            AnchorPane.setBottomAnchor(newTextArea, 0.0);
            AnchorPane.setRightAnchor(newTextArea, 0.0);
            AnchorPane.setLeftAnchor(newTextArea, 0.0);

            setControlsDisable(false);
        }
    }

    @FXML
    private void OnRunBtnAction() {
        preStartActions();
        try {
            String content = ((TextArea)((Parent)((TabPane)((Parent) file_tab.getSelectionModel().getSelectedItem().getContent()).getChildrenUnmodifiable().get(0)).getTabs().get(0).getContent()).getChildrenUnmodifiable().get(0)).getText();
            simulator.compile(content);
            (new Thread(() -> {
                try {
                    btn_break.setDisable(false);
                    simulator.startWorkerAndRun();
                } catch (ExecutionException e) {
                    console_box.appendText(e.getMessage());
                    btn_break.setDisable(false);
                }
            })).start();
        } catch (Exception e) {
            console_box.setText(e.getMessage());
        }
    }
    @FXML
    private void onStopBtnAction(){
        simulator.stop();
    }
    @FXML
    private void onDebugBtnAction(){
        preStartActions();
        debugMode = true;
        try {
            String content = ((TextArea)((Parent)((TabPane)((Parent) file_tab.getSelectionModel().getSelectedItem().getContent()).getChildrenUnmodifiable().get(0)).getTabs().get(0).getContent()).getChildrenUnmodifiable().get(0)).getText();
            simulator.compile(content);
            (new Thread(() -> {
                try {
                    btn_break.setDisable(false);
                    simulator.startWorker();
                } catch (Exception e) {
                    console_box.appendText(e.getMessage());
                    btn_break.setDisable(true);
                }
            })).start();
            setDebugControlsVisible(true);
        } catch (Exception e) {
            console_box.setText(e.getMessage());
        }
    }
    @FXML
    private void onStepBackBtnAction() {
        if (debugMode){
            (new Thread(() -> {
                try {
                    if (simulator.isRunning()) {
                        simulator.stepBack();
                    }
                    else {
                        debugMode = false;
                    }
                } catch (Exception e) {
                    console_box.appendText(e.getMessage());
                }
            })).start();
        }
    }
    @FXML
    private void onStepOverBtnAction() {
        if (debugMode){
            (new Thread(() -> {
                try {
                    if (simulator.isRunning()) {
                        simulator.runSteps(1);
                    }
                    else {
                        debugMode = false;
                    }
                } catch (Exception e) {
                    console_box.appendText(e.getMessage());
                }
            })).start();
        }
        updateRegistersTable();
    }
    @FXML
    private void onPauseBtnAction() {
    }
    @FXML
    private void onResumeBtnAction() {
    }
    @FXML
    private void closeCurrentFile(){
        if (!file_tab.getTabs().isEmpty()) {
            file_tab.getTabs().remove(file_tab.getSelectionModel().getSelectedItem());
        }
    }
    @FXML
    private void closeAllFiles(){
        file_tab.getTabs().clear();
    }
    @FXML
    private void closeApplication(){
        Platform.exit();
    }

    private void setDebugControlsVisible(boolean visible){
        btn_step_back.setVisible(visible);
        btn_step_over.setVisible(visible);
        btn_pause.setVisible(visible);
        btn_resume.setVisible(visible);
    }
    private void setControlsDisable(boolean enabled){
        btn_run.setDisable(enabled);
        btn_debug.setDisable(enabled);
    }
    private void updateRegistersTable() {
        reg_table.refresh();
    }
    private void preStartActions(){
        consoleUneditableText.setLength(0);
        console_box.setText("");
        consoleScanner.update();
        console_box.setEditable(true);
        simulator.stop();
    }
}