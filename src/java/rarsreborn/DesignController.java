package rarsreborn;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

import java.net.URL;
import java.util.ResourceBundle;


public class DesignController implements Initializable {
    @FXML
    private Button btn_break;
    @FXML
    private Button btn_debug;
    @FXML
    private Button btn_run;
    @FXML
    private Button btn_save;
    @FXML
    private TextArea code_edit_box;
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
    private TabPane file_tab;
    @FXML
    private TabPane base_tab;
    @FXML
    private Tab initial_file_tab;
    @FXML
    private Tab base_edit_tab;
    @FXML
    private Tab base_execute_tab;
    @FXML
    private TextArea initial_file_textbox;
    @FXML
    private TextArea console_box;
    final StringBuilder consoleUneditableText = new StringBuilder();

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

        simulator.getExecutionEnvironment().addObserver(ConsolePrintStringEvent.class, (ConsolePrintStringEvent) -> {
            console_box.appendText(ConsolePrintStringEvent.text());
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
    }

    private void updateRegistersTable() {
        reg_table.refresh();
    }

    @FXML
    void OnBtnRunAction(ActionEvent event) {
        console_box.setText("");
        consoleUneditableText.setLength(0);
        consoleScanner.update();
        console_box.setEditable(true);
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
        file_tab.getTabs().add(newTab);

        AnchorPane newAnchorPane = new AnchorPane();
        newTab.setContent(newAnchorPane);

        TabPane newTabPane = new TabPane();
        Tab newEditTab = new Tab("EDIT");
        Tab newExecuteTab = new Tab("EXECUTE");
        newTabPane.getTabs().addAll(newEditTab, newExecuteTab);

        newAnchorPane.getChildren().add(newTabPane);
        newTabPane.setPrefSize(base_tab.getPrefWidth(), base_tab.getPrefHeight());

        AnchorPane newEditPane = new AnchorPane();
        newEditTab.setContent(newEditPane);
        TextArea newTextArea = new TextArea();
        newEditPane.getChildren().add(newTextArea);

        newTextArea.setStyle(initial_file_textbox.getStyle());
        newTextArea.setPrefSize(initial_file_textbox.getPrefWidth(), initial_file_textbox.getPrefHeight());
        newTextArea.setFont(initial_file_textbox.getFont());

        btn_run.setDisable(false);
    }

    @FXML
    void onStopBtnAction(ActionEvent event){
        for (Register32 r: registersList){
            System.out.println(r.getName() + " " + r.getValue());
        }
    }
}