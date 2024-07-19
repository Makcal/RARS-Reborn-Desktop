package rarsreborn;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.layout.AnchorPane;
import rarsreborn.core.Presets;
import rarsreborn.core.core.environment.ITextInputDevice;
import rarsreborn.core.core.environment.events.*;
import rarsreborn.core.core.memory.IMemory;
import rarsreborn.core.core.register.Register32ChangeEvent;
import rarsreborn.core.core.register.Register32File;
import rarsreborn.core.core.register.Register32;
import rarsreborn.core.exceptions.execution.ExecutionException;
import rarsreborn.core.simulator.Simulator32;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import rarsreborn.core.simulator.StopEvent;
import rarsreborn.core.simulator.backstepper.BackStepper;

import java.net.URL;
import java.util.ResourceBundle;


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
    private Button btn_newfile;

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
    private TextArea initial_file_textbox;

    @FXML
    private MenuButton menu_btn;

    @FXML
    private MenuItem menu_item_close;

    @FXML
    private MenuItem menu_item_close_all;

    @FXML
    private MenuItem menu_item_exit;

    @FXML
    private MenuItem menu_item_new;

    @FXML
    private MenuItem menu_item_open;

    @FXML
    private MenuItem menu_item_save;

    @FXML
    private MenuItem menu_item_save_as;

    @FXML
    private TableView<Register32> reg_table;

    @FXML
    private TableColumn<Register32, String> reg_table_name;
    @FXML
    private TableColumn<Register32, Integer> reg_table_num;


    @FXML
    private TableColumn<Register32, Integer> reg_table_value;

    Simulator32 simulator = Presets.getClassicalRiscVSimulator(new ITextInputDevice() {
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
    Register32File registers = simulator.getRegisterFile();
    IMemory memory = simulator.getMemory();

    ObservableList<Register32> registersList = FXCollections.observableArrayList(registers.getAllRegisters());

    TextAreaScanner consoleScanner;
    final StringBuilder consoleUneditableText = new StringBuilder();

    boolean debugMode = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        registersList.add(simulator.getProgramCounter());
        reg_table_name.setCellValueFactory(new PropertyValueFactory<Register32, String>("name"));
        reg_table_num.setCellValueFactory(new PropertyValueFactory<Register32, Integer>("number"));
        reg_table_value.setCellValueFactory(new PropertyValueFactory<Register32, Integer>("value"));
        reg_table.setItems(registersList);
        for (Register32 r: registersList){
            r.addObserver(Register32ChangeEvent.class, (register32ChangeEvent) -> {
                updateRegistersTable();
            });
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
        simulator.addObserver(StopEvent.class, (event) -> {
            debugMode = false;
            btn_step_back.setVisible(false);
            btn_step_over.setVisible(false);
        });
        simulator.addObserver(BackStepper.class, (event) -> {
            updateRegistersTable();
        });

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

        consoleScanner = new TextAreaScanner();
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

        btn_run.setDisable(true);
        btn_debug.setDisable(true);
        btn_step_back.setVisible(false);
        btn_step_over.setVisible(false);
    }

    private void updateRegistersTable() {
        reg_table.refresh();
    }

    @FXML
    void OnBtnRunAction(ActionEvent event) {
        preStartActions();
        try {
            String content = ((TextArea)((Parent)((TabPane)((Parent) file_tab.getSelectionModel().getSelectedItem().getContent()).getChildrenUnmodifiable().get(0)).getTabs().get(0).getContent()).getChildrenUnmodifiable().get(0)).getText();
            simulator.compile(content);
            (new Thread(() -> {
                try {
                    simulator.startWorkerAndRun();
                } catch (ExecutionException e) {
                    console_box.appendText(e.getMessage());
                }
            })).start();
        } catch (Exception e) {
            console_box.setText(e.getMessage());
        }
    }

    @FXML
    void OnMenuItemNewAction(ActionEvent event) {
        Tab newTab = new Tab("NEW TAB");
        newTab.setOnClosed(new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
                if (file_tab.getTabs().isEmpty()){
                    btn_run.setDisable(true);
                    btn_debug.setDisable(true);
                }
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

        newTextArea.setStyle(initial_file_textbox.getStyle());
        newTextArea.setFont(initial_file_textbox.getFont());

        AnchorPane.setTopAnchor(newTextArea, 0.0);
        AnchorPane.setBottomAnchor(newTextArea, 0.0);
        AnchorPane.setRightAnchor(newTextArea, 0.0);
        AnchorPane.setLeftAnchor(newTextArea, 0.0);

        btn_run.setDisable(false);
        btn_debug.setDisable(false);
    }

    @FXML
    void onStopBtnAction(ActionEvent event){
        for (Register32 r: registersList){
            System.out.println(r.getName() + " " + r.getValue());
        }
    }

    @FXML
    void onDebugBtnAction(ActionEvent event){
        preStartActions();
        debugMode = true;
        try {
            String content = ((TextArea)((Parent)((TabPane)((Parent) file_tab.getSelectionModel().getSelectedItem().getContent()).getChildrenUnmodifiable().get(0)).getTabs().get(0).getContent()).getChildrenUnmodifiable().get(0)).getText();
            simulator.compile(content);
            (new Thread(() -> {
                try {
                    simulator.startWorker();
                } catch (Exception e) {
                    console_box.appendText(e.getMessage());
                }
            })).start();
            btn_step_back.setVisible(true);
            btn_step_over.setVisible(true);
        } catch (Exception e) {
            console_box.setText(e.getMessage());
        }
    }

    @FXML
    public void onStepBackBtnAction(ActionEvent event) {
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
    public void onStepOverBtnAction(ActionEvent event) {
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
    }

    private void preStartActions(){
        consoleUneditableText.setLength(0);
        console_box.setText("");
        consoleScanner.update();
        console_box.setEditable(true);
        try {
            simulator.reset();
        }
        catch (Exception ignored){
        }
    }

    public void onPauseBtnAction(ActionEvent event) {
    }

    public void onResumeBtnAction(ActionEvent event) {
    }
}