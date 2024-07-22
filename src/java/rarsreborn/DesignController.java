package rarsreborn;

import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Objects;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import rarsreborn.core.Presets;
import rarsreborn.core.core.environment.ITextInputDevice;
import rarsreborn.core.core.environment.events.*;
import rarsreborn.core.core.instruction.IInstruction;
import rarsreborn.core.core.memory.IMemory;
import rarsreborn.core.core.memory.Memory32;
import rarsreborn.core.core.memory.MemoryChangeEvent;
import rarsreborn.core.core.register.Register32ChangeEvent;
import rarsreborn.core.core.register.Register32;
import rarsreborn.core.core.register.floatpoint.RegisterFloat64;
import rarsreborn.core.core.register.floatpoint.RegisterFloat64ChangeEvent;
import rarsreborn.core.exceptions.execution.ExecutionException;
import rarsreborn.core.simulator.PausedEvent;
import rarsreborn.core.simulator.SimulatorRiscV;
import rarsreborn.core.simulator.StoppedEvent;
import rarsreborn.core.simulator.backstepper.BackStepFinishedEvent;


public class DesignController implements Initializable {
    @FXML
    public CheckBox check_box_reg_table_hex;
    @FXML
    public CheckBox check_box_memory_table_hex;

    @FXML
    private Button btn_break;
    @FXML
    private Button btn_debug;
    @FXML
    private Button btn_pause;
    @FXML
    private Button btn_resume;
    @FXML
    private Button btn_run;
    @FXML
    private Button btn_step_back;
    @FXML
    private Button btn_step_over;
    @FXML
    private Button btn_new_file;
    @FXML
    private Button btn_save;
    @FXML
    private Button btn_left_memory;
    @FXML
    private Button btn_right_memory;

    @FXML
    private MenuButton btn_burger_menu;

    @FXML
    private ImageView image_menu_new_file;
    @FXML
    private ImageView image_menu_save;

    @FXML
    private TextArea console_text_box;
    @FXML
    private TextArea initial_file_text_box;

    @FXML
    private TabPane tab_pane_files;

    @FXML
    private Tab initial_file_tab;
    @FXML
    private Tab execute_tab;

    @FXML
    private TableView<Register32> table_reg;
    @FXML
    private TableView<RegisterFloat64> table_float_reg;
    @FXML
    private TableView<Integer> table_memory;
    @FXML
    public TableView<Integer> table_code;
    @FXML
    public TableColumn<Integer, String> table_code_address;
    @FXML
    public TableColumn<Integer, String> table_code_code;
    @FXML
    public TableColumn<Integer, String> table_code_basic;
    @FXML
    private TableColumn<RegisterFloat64, Integer> table_float_reg_num;
    @FXML
    private TableColumn<RegisterFloat64, String> table_float_reg_name;
    @FXML
    private TableColumn<RegisterFloat64, String> table_float_reg_value;
    @FXML
    private TableColumn<Register32, String> table_reg_name;
    @FXML
    private TableColumn<Register32, Integer> table_reg_num;
    @FXML
    private TableColumn<Register32, String> table_reg_value;

    @FXML
    private ChoiceBox<String> choice_box_memory;
    @FXML
    private ChoiceBox<String> choice_box_value;

    @FXML
    private VBox root_VBox;

    @FXML
    private AnchorPane anchor_pane_instruments;
    @FXML
    private AnchorPane anchor_pane_reg_table_button;


    private final SimulatorRiscV simulator = Presets.getClassicalRiscVSimulator(new ITextInputDevice() {
        @Override
        public String requestString(int count) {
            String s = consoleScanner.readLine();
            return s.length() <= count ? s : s.substring(0, count);
        }

        @Override
        public int requestInt() {
            String s = consoleScanner.readLine();
            try {
                return Integer.parseInt(s);
            } catch (Exception e) {
                consoleUneditableText.append("\"").append(s).append("\" is not an Integer");
                console_text_box.appendText("\"" + s + "\" is not an Integer");
            }
            return 0;
        }

        @Override
        public byte requestChar() {
            String s = consoleScanner.readLine();
            try {
                return Byte.parseByte(s);
            } catch (Exception e) {
                console_text_box.appendText("\"" + s + "\" is not an Character");
            }
            return 0;
        }
    });
    private final IMemory memory = simulator.getMemory();

    private final ObservableList<Register32> registersList = FXCollections.observableArrayList(simulator.getRegisterFile().getAllRegisters());
    private final ObservableList<RegisterFloat64> floatRegistersList = FXCollections.observableArrayList(simulator.getFloatRegisterFile().getAllRegisters());
    private final ObservableList<IInstruction> instructions = FXCollections.observableArrayList();
    private final ObservableList<Integer> memoryAddresses = FXCollections.observableArrayList();

    private final StringBuilder consoleUneditableText = new StringBuilder();
    private TextAreaScanner consoleScanner;

    private final HashMap<Tab, URI> filesNamesLinker = new HashMap<>();

    private boolean debugMode = false;
    private boolean isDarkTheme = false;


    private int codeTableLastIndex = 0;
    private int memoryOffset;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        consoleScanner = new TextAreaScanner(console_text_box, consoleUneditableText);

        registersList.add(simulator.getProgramCounter());
        table_reg_name.setCellValueFactory(new PropertyValueFactory<>("name"));
        table_reg_num.setCellValueFactory(new PropertyValueFactory<>("numericName"));
        for (Register32 r : registersList) {
            r.addObserver(Register32ChangeEvent.class, (register32ChangeEvent) -> updateRegistersTable());
        }

        table_float_reg_name.setCellValueFactory(new PropertyValueFactory<>("name"));
        table_float_reg_num.setCellValueFactory(new PropertyValueFactory<>("numericName"));
        for (RegisterFloat64 r : floatRegistersList) {
            r.addObserver(RegisterFloat64ChangeEvent.class, (float64ChangeEvent) -> updateFloatTable());
        }
        updateRegisterTables();

        choice_box_memory.setItems(FXCollections.observableArrayList("0x00400000 (.text)", "0x10010000 (.data)", "0x10040000 (.heap)", "current sp"));
        choice_box_value.setItems(FXCollections.observableArrayList("Decimal values", "Hexadecimal values", "ASCII"));
        choice_box_memory.getSelectionModel().selectedIndexProperty().addListener((observable) -> {
            switch (choice_box_memory.getSelectionModel().getSelectedIndex()) {
                case 0:
                    memoryOffset = Memory32.TEXT_SECTION_START;
                    break;
                case 1:
                    memoryOffset = Memory32.DATA_SECTION_START;
                    break;
                case 2:
                    memoryOffset = Memory32.HEAP_SECTION_START;
                    break;
                case 3:
                    memoryOffset = simulator.getProgramCounter().getValue() - (simulator.getProgramCounter().getValue() % 32);
                    break;
            }
            updateMemoryTable();
        });
        choice_box_value.getSelectionModel().selectedIndexProperty().addListener((observable -> updateMemoryTable()));
        choice_box_memory.getSelectionModel().select("0x10010000 (.data)");
        choice_box_value.getSelectionModel().select("Hexadecimal values");
        updateMemoryTable();

        table_code_address.setCellValueFactory(integerCellDataFeatures -> {
            try {
                return new ReadOnlyObjectWrapper<>(String.valueOf(integerCellDataFeatures.getValue() * 4 + Memory32.TEXT_SECTION_START));
            } catch (Exception e) {
                throw new RuntimeException();
            }
        });
        table_code_basic.setCellValueFactory(
                integerCellDataFeatures -> {
                    try {
                        return new ReadOnlyObjectWrapper<>(instructions.get(integerCellDataFeatures.getValue()).toString());
                    } catch (Exception e) {
                        throw new RuntimeException();
                    }
                });
        table_code_code.setCellValueFactory(
                integerCellDataFeatures -> {
                    try {
                        return new ReadOnlyObjectWrapper<>("0x" + Integer.toHexString(bytesToInt(instructions.get(integerCellDataFeatures.getValue()).serialize())));
                    } catch (Exception e) {
                        throw new RuntimeException();
                    }
                });

        simulator.getExecutionEnvironment().addObserver(ConsolePrintStringEvent.class, (event) -> {
            console_text_box.appendText(event.text());
            consoleUneditableText.append(event.text());
            consoleScanner.terminalMessage(event.text());
        });
        simulator.getExecutionEnvironment().addObserver(ConsolePrintCharEvent.class, (event) -> {
            console_text_box.appendText(String.valueOf((char) event.character()));
            consoleUneditableText.append((char) event.character());
            consoleScanner.terminalMessage(String.valueOf((char) event.character()));
        });
        simulator.getExecutionEnvironment().addObserver(ConsolePrintIntegerEvent.class, (event) -> {
            consoleUneditableText.append(event.value());
            console_text_box.appendText(String.valueOf(event.value()));
            consoleScanner.terminalMessage(String.valueOf(event.value()));
        });
        simulator.getExecutionEnvironment().addObserver(ConsolePrintIntegerHexEvent.class, (event) -> {
            console_text_box.appendText(Integer.toHexString(event.value()));
            consoleUneditableText.append(Integer.toHexString(event.value()));
            consoleScanner.terminalMessage(Integer.toHexString(event.value()));
        });
        simulator.getExecutionEnvironment().addObserver(ConsolePrintIntegerOctalEvent.class, (event) -> {
            console_text_box.appendText(Integer.toOctalString(event.value()));
            consoleUneditableText.append(Integer.toOctalString(event.value()));
            consoleScanner.terminalMessage(Integer.toOctalString(event.value()));
        });
        simulator.getExecutionEnvironment().addObserver(ConsolePrintIntegerBinaryEvent.class, (event) -> {
            console_text_box.appendText(Integer.toBinaryString(event.value()));
            consoleUneditableText.append(Integer.toBinaryString(event.value()));
            consoleScanner.terminalMessage(Integer.toBinaryString(event.value()));
        });
        simulator.getExecutionEnvironment().addObserver(ConsolePrintIntegerUnsignedEvent.class, (event) -> {
            console_text_box.appendText(Integer.toUnsignedString(event.value()));
            consoleUneditableText.append(Integer.toUnsignedString(event.value()));
            consoleScanner.terminalMessage(Integer.toUnsignedString(event.value()));
        });
        simulator.addObserver(StoppedEvent.class, (event) -> {
            debugMode = false;
            setDebugControlsVisible(false);
            btn_break.setDisable(true);
            btn_pause.setDisable(true);
            btn_resume.setDisable(true);
            table_code.getItems().clear();
            instructions.clear();
            updateButtonsState();
        });
        simulator.addObserver(BackStepFinishedEvent.class, (event) -> updateRegistersTable());
        memory.addObserver(MemoryChangeEvent.class, (event) -> table_memory.refresh());

        simulator.addObserver(PausedEvent.class, (event) -> {
            btn_pause.setDisable(true);
            btn_resume.setDisable(false);
        });
        simulator.getProgramCounter().addObserver(Register32ChangeEvent.class, (event) -> updateCodeTableFocus((event.newValue() - Memory32.TEXT_SECTION_START) / 4));
        simulator.addObserver(BackStepFinishedEvent.class, (event) -> updateCodeTableFocus((simulator.getProgramCounter().getValue() - Memory32.TEXT_SECTION_START) / 4));

        root_VBox.addEventFilter(KeyEvent.KEY_PRESSED, ke -> {
            switch (ke.getCode()) {
                case KeyCode.S:
                    if (ke.isControlDown()) {
                        if (ke.isShiftDown()) {
                            saveFileAs();
                        } else {
                            saveFile();
                        }
                    }
                    break;
                case KeyCode.N:
                    if (ke.isControlDown()) {
                        createNewTab();
                    }
                    break;
                case KeyCode.O:
                    if (ke.isControlDown()) {
                        openFile();
                    }
                    break;
                case KeyCode.W:
                    if (ke.isControlDown()) {
                        if (ke.isShiftDown()){
                            closeAllFiles();
                        }
                        else {
                            closeCurrentFile();
                        }
                    }
                    break;
                case KeyCode.F5:
                    if (ke.isShiftDown()){
                        onDebugBtnAction();
                    }
                    else {
                        OnRunBtnAction();
                    }
                    break;
                case KeyCode.F6:
                    if (debugMode){
                        onStepBackBtnAction();
                    }
                    break;
                case KeyCode.F7:
                    if (debugMode){
                        onStepOverBtnAction();
                    }
                    break;
                case KeyCode.F8:
                    onPauseBtnAction();
                    break;
                case KeyCode.F9:
                    onResumeBtnAction();
                    break;
                case KeyCode.F10:
                    onStopBtnAction();
                    break;
            }
        });

        console_text_box.setTextFormatter(new TextFormatter<String>((Change c) -> {
            String proposed = c.getControlNewText();
            if (proposed.startsWith(consoleUneditableText.toString())) {
                return c;
            } else {
                return null;
            }
        }));
        console_text_box.setEditable(false);

        tab_pane_files.getTabs().remove(initial_file_tab);
        setControlsDisable(true);
        setDebugControlsVisible(false);
        btn_break.setDisable(true);
        btn_resume.setDisable(true);
        btn_pause.setDisable(true);

        createNewTab();
        tab_pane_files.getSelectionModel().select(1);
        updateRegistersTable();
        updateButtonsState();
    }

    @FXML
    private void CreateNewFile() {
        createNewTab();
    }


    @FXML
    private void OnRunBtnAction() {
        preStartActions();
        try {
            Tab curTab = tab_pane_files.getSelectionModel().getSelectedItem();
            if (!Objects.equals(curTab.getText(), "EXECUTE")) {
                btn_pause.setDisable(false);
                String content = ((TextArea) ((Parent) curTab.getContent()).getChildrenUnmodifiable().get(0)).getText();
                simulator.compile(content);
                instructions.addAll(simulator.getProgramInstructions());
                updateCodeTable();
                if (isDarkTheme) {
                    Image rerun = new Image(getClass().getResourceAsStream("/rarsreborn/Images/darkTheme/Rerun.png"));
                    btn_run.setGraphic(new ImageView(rerun));
                } else {
                    Image rerun = new Image(getClass().getResourceAsStream("/rarsreborn/Images/lightTheme/Rerun.png"));
                    btn_run.setGraphic(new ImageView(rerun));
                }
                updateButtonsState();
                (new Thread(() -> {
                    try {
                        btn_break.setDisable(false);
                        simulator.startWorkerAndRun();
                    } catch (ExecutionException e) {
                        console_text_box.appendText(e.getMessage() + "\n");
                        btn_break.setDisable(false);
                    }
                })).start();
            }
        } catch (Exception e) {
            console_text_box.setText(e.getMessage()+ "\n");
        }
    }

    @FXML
    private void onStopBtnAction() {
        simulator.stop();
    }

    @FXML
    private void onDebugBtnAction() {
        preStartActions();
        debugMode = true;
        try {
            Tab curTab = tab_pane_files.getSelectionModel().getSelectedItem();
            if (!Objects.equals(curTab.getText(), "EXECUTE")) {
                btn_resume.setDisable(false);
                String content = ((TextArea) ((Parent) curTab.getContent()).getChildrenUnmodifiable().get(0)).getText();
                simulator.compile(content);
                instructions.addAll(simulator.getProgramInstructions());
                updateCodeTable();
                if (isDarkTheme) {
                    Image redebug = new Image(getClass().getResourceAsStream("/rarsreborn/Images/darkTheme/Redebug.png"));
                    btn_debug.setGraphic(new ImageView(redebug));
                } else {
                    Image redebug = new Image(getClass().getResourceAsStream("/rarsreborn/Images/lightTheme/Redebug.png"));
                    btn_debug.setGraphic(new ImageView(redebug));
                }
                updateButtonsState();
                (new Thread(() -> {
                    try {
                        btn_break.setDisable(false);
                        simulator.startWorker();
                    } catch (Exception e) {
                        console_text_box.appendText(e.getMessage()+ "\n");
                        btn_break.setDisable(true);
                    }
                })).start();
                setDebugControlsVisible(true);
                tab_pane_files.getSelectionModel().select(0);
            }
        } catch (Exception e) {
            console_text_box.setText(e.getMessage()+ "\n");
        }
    }

    @FXML
    private void onStepBackBtnAction() {
        if (debugMode) {
            (new Thread(() -> {
                try {
                    if (simulator.isRunning()) {
                        simulator.stepBack();
                    } else {
                        debugMode = false;
                    }
                } catch (Exception e) {
                    console_text_box.appendText(e.getMessage() + "\n");
                }
            })).start();
        }
    }

    @FXML
    private void onStepOverBtnAction() {
        if (debugMode) {
            (new Thread(() -> {
                try {
                    if (simulator.isRunning()) {
                        simulator.runSteps(1);
                    } else {
                        debugMode = false;
                    }
                } catch (Exception e) {
                    console_text_box.appendText(e.getMessage()+ "\n");
                }
            })).start();
        }
        updateRegistersTable();
    }

    @FXML
    private void onPauseBtnAction() {
        simulator.pause();
        btn_pause.setDisable(true);
        btn_resume.setDisable(false);
        debugMode = true;
        btn_resume.setDisable(false);
        tab_pane_files.getSelectionModel().select(0);
        setDebugControlsVisible(true);
    }

    @FXML
    private void onResumeBtnAction() {
        btn_pause.setDisable(false);
        btn_resume.setDisable(true);
        simulator.run();
    }

    @FXML
    private void closeCurrentFile() {
        if (!tab_pane_files.getTabs().isEmpty()) {
            if (Objects.equals(tab_pane_files.getSelectionModel().getSelectedItem().getText(), "EXECUTE")) {
                return;
            }
            tab_pane_files.getTabs().remove(tab_pane_files.getSelectionModel().getSelectedItem());
        }
    }

    @FXML
    private void closeAllFiles() {
        tab_pane_files.getTabs().removeIf(t -> !Objects.equals(t.getText(), "EXECUTE"));
    }

    @FXML
    private void closeApplication() {
        Platform.exit();
    }

    @FXML
    private void saveFileAs() {
        try {
            Tab tab = tab_pane_files.getSelectionModel().getSelectedItem();
            if (Objects.equals(tab.getText(), "EXECUTE")) {
                return;
            }
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save File");
            fileChooser.setInitialFileName(tab.getText());
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ASM files (*.asm)", "*.asm"));


            File newFile = fileChooser.showSaveDialog(Window.getWindows().get(0));
            if (newFile != null) {
                FileWriter currentFile = new FileWriter(newFile);
                currentFile.write(((TextArea) ((Parent) tab.getContent()).getChildrenUnmodifiable().get(0)).getText());
                currentFile.close();
                filesNamesLinker.put(tab, newFile.toURI());
                tab.setText(newFile.getName().split("\\.")[0]);
            }
        } catch (Exception e) {
            console_text_box.appendText(e.getMessage()+ "\n");
        }
    }

    @FXML
    private void saveFile() {
        Tab tab = tab_pane_files.getSelectionModel().getSelectedItem();
        if (Objects.equals(tab.getText(), "EXECUTE")) {
            return;
        }
        if (filesNamesLinker.get(tab) == null) {
            saveFileAs();
        } else if (!Files.exists(Path.of(filesNamesLinker.get(tab)))) {
            saveFileAs();
        } else {
            try {
                File newFile = new File(filesNamesLinker.get(tab));
                FileWriter currentFile = new FileWriter(newFile);
                currentFile.write(((TextArea) ((Parent) tab.getContent()).getChildrenUnmodifiable().get(0)).getText());
                currentFile.close();
            } catch (Exception e) {
                console_text_box.appendText(e.getMessage()+ "\n");
            }
        }
    }

    @FXML
    public void openFile() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Choose the file");
            FileChooser.ExtensionFilter asmFilter = new FileChooser.ExtensionFilter("ASM files (*.asm)", "*.asm");
            fileChooser.getExtensionFilters().add(asmFilter);
            File file = fileChooser.showOpenDialog(Window.getWindows().get(0));

            if (file != null) {
                String content = new String(Files.readAllBytes(file.toPath()));
                createNewTab(file.getName().split("\\.")[0]);
                ((TextArea) ((Parent) ((TabPane) ((Parent) tab_pane_files.getSelectionModel().getSelectedItem().getContent()).getChildrenUnmodifiable().get(0)).getTabs().get(0).getContent()).getChildrenUnmodifiable().get(0)).setText(content);
            }
        } catch (Exception e) {
            console_text_box.appendText(e.getMessage()+ "\n");
        }
    }

    @FXML
    void onMemoryLeftAction() {
        memoryOffset -= 32;
        if (memoryOffset >= 0) {
            updateMemoryTable();
        } else {
            memoryOffset += 32;
        }
    }

    @FXML
    void onMemoryRightAction() {
        memoryOffset += 32;
        if (memoryOffset <= memory.getSize()) {
            updateMemoryTable();
        } else {
            memoryOffset -= 32;
        }
    }

    @FXML
    private void changeTheme() {
        if (isDarkTheme) {
            anchor_pane_instruments.setStyle("-fx-background-color: #F7F8FA;");
            anchor_pane_reg_table_button.setStyle("-fx-background-color: #F7F8FA");
            check_box_reg_table_hex.setStyle("-fx-text-fill: #242628");
            check_box_memory_table_hex.setStyle("-fx-text-fill: #242628");
            root_VBox.setStyle("-fx-background-color: #F7F8FA");

            Image leftArrow = new Image(getClass().getResourceAsStream("/rarsreborn/Images/lightTheme/leftArrow.png"));
            btn_left_memory.setGraphic(new ImageView(leftArrow));

            Image rightArrow = new Image(getClass().getResourceAsStream("/rarsreborn/Images/lightTheme/rightArrow.png"));
            btn_right_memory.setGraphic(new ImageView(rightArrow));

            root_VBox.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/rarsreborn/Styles/global.css")).toExternalForm());
            root_VBox.getStylesheets().remove(Objects.requireNonNull(getClass().getResource("/rarsreborn/Styles/darkTheme.css")).toExternalForm());
            updateButtonsState();
        } else {
            anchor_pane_instruments.setStyle("-fx-background-color: #242628;");
            anchor_pane_reg_table_button.setStyle("-fx-background-color: #242628;");
            check_box_reg_table_hex.setStyle("-fx-text-fill: #F7F8FA");
            check_box_memory_table_hex.setStyle("-fx-text-fill: #F7F8FA");
            root_VBox.setStyle("-fx-background-color: #242628");

            Image leftArrow = new Image(getClass().getResourceAsStream("/rarsreborn/Images/darkTheme/leftArrow.png"));
            btn_left_memory.setGraphic(new ImageView(leftArrow));

            Image rightArrow = new Image(getClass().getResourceAsStream("/rarsreborn/Images/darkTheme/rightArrow.png"));
            btn_right_memory.setGraphic(new ImageView(rightArrow));

            root_VBox.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/rarsreborn/Styles/darkTheme.css")).toExternalForm());
            root_VBox.getStylesheets().remove(Objects.requireNonNull(getClass().getResource("/rarsreborn/Styles/global.css")).toExternalForm());
            updateButtonsState();
        }
        isDarkTheme = !isDarkTheme;
    }

    private void createNewTab(){
        createNewTab("New file");
    }

    private void createNewTab(String fileName) {
        Tab newTab = new Tab(fileName);
        newTab.setOnClosed(event -> {
            if (tab_pane_files.getTabs().size() == 1) {
                setControlsDisable(true);
            }
        });
        tab_pane_files.getTabs().add(newTab);
        newTab.setStyle(execute_tab.getStyle());

        AnchorPane newAnchorPane = new AnchorPane();
        newTab.setContent(newAnchorPane);


        TextArea newTextArea = new TextArea();
        newAnchorPane.getChildren().add(newTextArea);

        newTextArea.setStyle(initial_file_text_box.getStyle());
        newTextArea.setFont(initial_file_text_box.getFont());

        AnchorPane.setTopAnchor(newTextArea, 0.0);
        AnchorPane.setBottomAnchor(newTextArea, 0.0);
        AnchorPane.setRightAnchor(newTextArea, 0.0);
        AnchorPane.setLeftAnchor(newTextArea, 0.0);

        setControlsDisable(false);
        tab_pane_files.getSelectionModel().select(newTab);
    }

    private void setDebugControlsVisible(boolean visible) {
        btn_step_back.setVisible(visible);
        btn_step_over.setVisible(visible);
    }

    private void setControlsDisable(boolean enabled) {
        btn_run.setDisable(enabled);
        btn_debug.setDisable(enabled);
    }

    private void updateRegistersTable() {
        table_reg.refresh();
    }

    private void updateFloatTable() {
        table_float_reg.refresh();
    }

    private void updateMemoryTable() {
        table_memory.getItems().clear();
        memoryAddresses.clear();
        for (int i = 0; i < 8; i++) {
            memoryAddresses.add(memoryOffset + (i * 32));
        }
        StringBuilder buildString = new StringBuilder();
        if (!check_box_memory_table_hex.isSelected()) {
            //noinspection unchecked
            ((TableColumn<Integer, String>) table_memory.getColumns().get(0)).setCellValueFactory(integerCellDataFeatures -> {
                try {
                    return new ReadOnlyObjectWrapper<>(String.valueOf(integerCellDataFeatures.getValue()));
                } catch (Exception e) {
                    throw new RuntimeException();
                }
            });
        } else {
            //noinspection unchecked
            ((TableColumn<Integer, String>) table_memory.getColumns().get(0)).setCellValueFactory(integerCellDataFeatures -> {
                try {
                    buildString.setLength(0);
                    buildString.append("0x");
                    buildString.append(Integer.toHexString(integerCellDataFeatures.getValue()));
                    while (buildString.length() < 10) {
                        buildString.insert(2, 0);
                    }
                    return new ReadOnlyObjectWrapper<>(buildString.toString());
                } catch (Exception e) {
                    throw new RuntimeException();
                }
            });
        }
        switch (choice_box_value.getSelectionModel().getSelectedIndex()) {
            case 0:
                for (int i = 1; i < 9; i++) {
                    int finalI = i;
                    //noinspection unchecked
                    ((TableColumn<Integer, String>) table_memory.getColumns().get(i)).setCellValueFactory(integerCellDataFeatures -> {
                        try {
                            return new ReadOnlyObjectWrapper<>(String.valueOf(memory.getMultiple(integerCellDataFeatures.getValue() + ((finalI - 1) * 4), 4)));
                        } catch (Exception e) {
                            throw new RuntimeException();
                        }
                    });
                }
                break;
            case 1:
                for (int i = 1; i < 9; i++) {
                    int finalI = i;
                    //noinspection unchecked
                    ((TableColumn<Integer, String>) table_memory.getColumns().get(i)).setCellValueFactory(integerCellDataFeatures -> {
                        try {
                            buildString.setLength(0);
                            buildString.append("0x");
                            buildString.append(Long.toHexString(memory.getMultiple(integerCellDataFeatures.getValue() + ((finalI - 1) * 4), 4)));
                            while (buildString.length() < 10) {
                                buildString.insert(2, 0);
                            }

                            return new ReadOnlyObjectWrapper<>(buildString.toString());
                        } catch (Exception e) {
                            throw new RuntimeException();
                        }
                    });
                }
                break;
            case 2:
                for (int i = 1; i < 9; i++) {
                    int finalI = i;
                    //noinspection unchecked
                    ((TableColumn<Integer, String>) table_memory.getColumns().get(i)).setCellValueFactory(integerCellDataFeatures -> {
                        try {
                            buildString.setLength(0);
                            for (int j = 0; j < 4; j++) {
                                int k = memory.getByte(integerCellDataFeatures.getValue() + ((finalI - 1) * 4) + j);
                                if (k < 126 && k > 32) {
                                    buildString.append((char) k);
                                } else {
                                    buildString.append("\\").append(Integer.toHexString(k));
                                }
                                buildString.append(" ");
                            }
                            return new ReadOnlyObjectWrapper<>(buildString.toString());
                        } catch (Exception e) {
                            throw new RuntimeException();
                        }

                    });
                }
                break;
        }
        table_memory.getItems().addAll(memoryAddresses);
        table_memory.refresh();
    }

    private void preStartActions() {
        consoleUneditableText.setLength(0);
        console_text_box.setText("");
        consoleScanner.update();
        console_text_box.setEditable(true);
        if (simulator.isRunning()) {
            simulator.stop();
        }
    }

    @FXML
    public void OnChangeAddressViewAction(ActionEvent event) {
        updateMemoryTable();
    }

    public void OnChangeValueView(ActionEvent event) {
        updateRegisterTables();
    }

    private void updateRegisterTables() {
        table_reg.getItems().clear();
        table_float_reg.getItems().clear();
        if (check_box_reg_table_hex.isSelected()) {
            table_float_reg_value.setCellValueFactory(integerCellDataFeatures -> {
                try {
                    StringBuilder buildString = new StringBuilder();
                    buildString.setLength(0);
                    buildString.append("0x");
                    buildString.append(Long.toHexString(integerCellDataFeatures.getValue().getLong()));
                    while (buildString.length() < 10) {
                        buildString.insert(2, 0);
                    }

                    return new ReadOnlyObjectWrapper<>(buildString.toString());
                } catch (Exception e) {
                    throw new RuntimeException();
                }
            });
            table_reg_value.setCellValueFactory(integerCellDataFeatures -> {
                try {
                    StringBuilder buildString = new StringBuilder();
                    buildString.setLength(0);
                    buildString.append("0x");
                    buildString.append(Long.toHexString(integerCellDataFeatures.getValue().getValue()));
                    while (buildString.length() < 10) {
                        buildString.insert(2, 0);
                    }

                    return new ReadOnlyObjectWrapper<>(buildString.toString());
                } catch (Exception e) {
                    throw new RuntimeException();
                }
            });
        } else {
            table_float_reg_value.setCellValueFactory(new PropertyValueFactory<>("float"));
            table_reg_value.setCellValueFactory(new PropertyValueFactory<>("value"));
        }
        table_reg.getItems().addAll(registersList);
        table_float_reg.getItems().addAll(floatRegistersList);
    }

    private void updateCodeTable(){
        final ObservableList<Integer> ints = FXCollections.observableArrayList();
        for (int i = 0; i < instructions.size(); i++){
            ints.add(i);
        }
        table_code.getItems().addAll(ints);
    }

    private int bytesToInt(byte[] bytes){
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        return byteBuffer.getInt();
    }

    private void updateCodeTableFocus(int index){
        table_code.getSelectionModel().select(index);
        codeTableLastIndex = index;
    }
    @FXML
    private void codeTableMouseClicked(){
        updateCodeTableFocus(codeTableLastIndex);
    }

    public void updateButtonsState() {
        String darkPathTheme = "/rarsreborn/Images/darkTheme/";
        String lightPathTheme = "/rarsreborn/Images/lightTheme/";
        Platform.runLater(() -> {

            if (simulator.isRunning() && !debugMode && !simulator.isPaused()) {
                if (isDarkTheme) {
                    Image burger = new Image(getClass().getResourceAsStream(darkPathTheme + "burger.png"));
                    btn_burger_menu.setGraphic(new ImageView(burger));

                    Image newFile = new Image(getClass().getResourceAsStream(darkPathTheme + "New file.png"));
                    btn_new_file.setGraphic(new ImageView(newFile));

                    Image save = new Image(getClass().getResourceAsStream(darkPathTheme + "Save.png"));
                    btn_save.setGraphic(new ImageView(save));

                    Image stepBack = new Image(getClass().getResourceAsStream(darkPathTheme + "Undo.png"));
                    btn_step_back.setGraphic(new ImageView(stepBack));

                    Image pause = new Image(getClass().getResourceAsStream(darkPathTheme + "Pause.png"));
                    btn_pause.setGraphic(new ImageView(pause));

                    Image stepNext = new Image(getClass().getResourceAsStream(darkPathTheme + "Next step.png"));
                    btn_step_over.setGraphic(new ImageView(stepNext));

                    Image nextBreakPoint = new Image(getClass().getResourceAsStream(darkPathTheme + "Next Breakpoint.png"));
                    btn_resume.setGraphic(new ImageView(nextBreakPoint));

                    Image stop = new Image(getClass().getResourceAsStream(darkPathTheme + "Stop.png"));
                    btn_break.setGraphic(new ImageView(stop));

                    Image rerun = new Image(getClass().getResourceAsStream(darkPathTheme + "Rerun.png"));
                    btn_run.setGraphic(new ImageView(rerun));

                    Image debug = new Image(getClass().getResourceAsStream(darkPathTheme + "debug.png"));
                    btn_debug.setGraphic(new ImageView(debug));


                    image_menu_new_file.setImage(new Image(getClass().getResourceAsStream(darkPathTheme + "New file.png")));
                    image_menu_save.setImage(new Image(getClass().getResourceAsStream(darkPathTheme + "Save.png")));

                } else {
                    Image burger = new Image(getClass().getResourceAsStream(lightPathTheme + "Menu.png"));
                    btn_burger_menu.setGraphic(new ImageView(burger));

                    Image newFile = new Image(getClass().getResourceAsStream(lightPathTheme + "New file.png"));
                    btn_new_file.setGraphic(new ImageView(newFile));

                    Image save = new Image(getClass().getResourceAsStream(lightPathTheme + "Save.png"));
                    btn_save.setGraphic(new ImageView(save));

                    Image stepBack = new Image(getClass().getResourceAsStream(lightPathTheme + "Undo.png"));
                    btn_step_back.setGraphic(new ImageView(stepBack));

                    Image pause = new Image(getClass().getResourceAsStream(lightPathTheme + "Pause.png"));
                    btn_pause.setGraphic(new ImageView(pause));

                    Image stepNext = new Image(getClass().getResourceAsStream(lightPathTheme + "Next step.png"));
                    btn_step_over.setGraphic(new ImageView(stepNext));

                    Image nextBreakPoint = new Image(getClass().getResourceAsStream(lightPathTheme + "Next Breakpoint.png"));
                    btn_resume.setGraphic(new ImageView(nextBreakPoint));

                    Image stop = new Image(getClass().getResourceAsStream(lightPathTheme + "Stop.png"));
                    btn_break.setGraphic(new ImageView(stop));

                    Image debug = new Image(getClass().getResourceAsStream(lightPathTheme + "debug.png"));
                    btn_debug.setGraphic(new ImageView(debug));

                    Image rerun = new Image(getClass().getResourceAsStream(lightPathTheme + "Rerun.png"));
                    btn_run.setGraphic(new ImageView(rerun));

                    image_menu_new_file.setImage(new Image(getClass().getResourceAsStream(lightPathTheme + "New file.png")));
                    image_menu_save.setImage(new Image(getClass().getResourceAsStream(lightPathTheme + "Save.png")));
                }
            } else if (debugMode) {
                if (isDarkTheme) {
                    Image burger = new Image(getClass().getResourceAsStream(darkPathTheme + "burger.png"));
                    btn_burger_menu.setGraphic(new ImageView(burger));

                    Image newFile = new Image(getClass().getResourceAsStream(darkPathTheme + "New file.png"));
                    btn_new_file.setGraphic(new ImageView(newFile));

                    Image save = new Image(getClass().getResourceAsStream(darkPathTheme + "Save.png"));
                    btn_save.setGraphic(new ImageView(save));

                    Image stepBack = new Image(getClass().getResourceAsStream(darkPathTheme + "Undo.png"));
                    btn_step_back.setGraphic(new ImageView(stepBack));

                    Image pause = new Image(getClass().getResourceAsStream(darkPathTheme + "Pause.png"));
                    btn_pause.setGraphic(new ImageView(pause));

                    Image stepNext = new Image(getClass().getResourceAsStream(darkPathTheme + "Next step.png"));
                    btn_step_over.setGraphic(new ImageView(stepNext));

                    Image nextBreakPoint = new Image(getClass().getResourceAsStream(darkPathTheme + "Next Breakpoint.png"));
                    btn_resume.setGraphic(new ImageView(nextBreakPoint));

                    Image stop = new Image(getClass().getResourceAsStream(darkPathTheme + "Stop.png"));
                    btn_break.setGraphic(new ImageView(stop));

                    Image rerun = new Image(getClass().getResourceAsStream(darkPathTheme + "Run.png"));
                    btn_run.setGraphic(new ImageView(rerun));

                    Image debug = new Image(getClass().getResourceAsStream(darkPathTheme + "Redebug.png"));
                    btn_debug.setGraphic(new ImageView(debug));


                    image_menu_new_file.setImage(new Image(getClass().getResourceAsStream(darkPathTheme + "New file.png")));
                    image_menu_save.setImage(new Image(getClass().getResourceAsStream(darkPathTheme + "Save.png")));
                } else {
                    Image burger = new Image(getClass().getResourceAsStream(lightPathTheme + "Menu.png"));
                    btn_burger_menu.setGraphic(new ImageView(burger));

                    Image newFile = new Image(getClass().getResourceAsStream(lightPathTheme + "New file.png"));
                    btn_new_file.setGraphic(new ImageView(newFile));

                    Image save = new Image(getClass().getResourceAsStream(lightPathTheme + "Save.png"));
                    btn_save.setGraphic(new ImageView(save));

                    Image stepBack = new Image(getClass().getResourceAsStream(lightPathTheme + "Undo.png"));
                    btn_step_back.setGraphic(new ImageView(stepBack));

                    Image pause = new Image(getClass().getResourceAsStream(lightPathTheme + "Pause.png"));
                    btn_pause.setGraphic(new ImageView(pause));

                    Image stepNext = new Image(getClass().getResourceAsStream(lightPathTheme + "Next step.png"));
                    btn_step_over.setGraphic(new ImageView(stepNext));

                    Image nextBreakPoint = new Image(getClass().getResourceAsStream(lightPathTheme + "Next Breakpoint.png"));
                    btn_resume.setGraphic(new ImageView(nextBreakPoint));

                    Image stop = new Image(getClass().getResourceAsStream(lightPathTheme + "Stop.png"));
                    btn_break.setGraphic(new ImageView(stop));

                    Image debug = new Image(getClass().getResourceAsStream(lightPathTheme + "Redebug.png"));
                    btn_debug.setGraphic(new ImageView(debug));

                    Image rerun = new Image(getClass().getResourceAsStream(lightPathTheme + "Run.png"));
                    btn_run.setGraphic(new ImageView(rerun));

                    image_menu_new_file.setImage(new Image(getClass().getResourceAsStream(lightPathTheme + "New file.png")));
                    image_menu_save.setImage(new Image(getClass().getResourceAsStream(lightPathTheme + "Save.png")));
                }
            } else if (!simulator.isRunning()) {
                if (isDarkTheme) {
                    Image burger = new Image(getClass().getResourceAsStream(darkPathTheme + "burger.png"));
                    btn_burger_menu.setGraphic(new ImageView(burger));

                    Image newFile = new Image(getClass().getResourceAsStream(darkPathTheme + "New file.png"));
                    btn_new_file.setGraphic(new ImageView(newFile));

                    Image save = new Image(getClass().getResourceAsStream(darkPathTheme + "Save.png"));
                    btn_save.setGraphic(new ImageView(save));

                    Image stepBack = new Image(getClass().getResourceAsStream(darkPathTheme + "Undo.png"));
                    btn_step_back.setGraphic(new ImageView(stepBack));

                    Image pause = new Image(getClass().getResourceAsStream(darkPathTheme + "Pause.png"));
                    btn_pause.setGraphic(new ImageView(pause));

                    Image stepNext = new Image(getClass().getResourceAsStream(darkPathTheme + "Next step.png"));
                    btn_step_over.setGraphic(new ImageView(stepNext));

                    Image nextBreakPoint = new Image(getClass().getResourceAsStream(darkPathTheme + "Next Breakpoint.png"));
                    btn_resume.setGraphic(new ImageView(nextBreakPoint));

                    Image stop = new Image(getClass().getResourceAsStream(darkPathTheme + "Stop.png"));
                    btn_break.setGraphic(new ImageView(stop));

                    Image rerun = new Image(getClass().getResourceAsStream(darkPathTheme + "Run.png"));
                    btn_run.setGraphic(new ImageView(rerun));

                    Image debug = new Image(getClass().getResourceAsStream(darkPathTheme + "debug.png"));
                    btn_debug.setGraphic(new ImageView(debug));


                    image_menu_new_file.setImage(new Image(getClass().getResourceAsStream(darkPathTheme + "New file.png")));
                    image_menu_save.setImage(new Image(getClass().getResourceAsStream(darkPathTheme + "Save.png")));

                } else {
                    Image burger = new Image(getClass().getResourceAsStream(lightPathTheme + "Menu.png"));
                    btn_burger_menu.setGraphic(new ImageView(burger));

                    Image newFile = new Image(getClass().getResourceAsStream(lightPathTheme + "New file.png"));
                    btn_new_file.setGraphic(new ImageView(newFile));

                    Image save = new Image(getClass().getResourceAsStream(lightPathTheme + "Save.png"));
                    btn_save.setGraphic(new ImageView(save));

                    Image stepBack = new Image(getClass().getResourceAsStream(lightPathTheme + "Undo.png"));
                    btn_step_back.setGraphic(new ImageView(stepBack));

                    Image pause = new Image(getClass().getResourceAsStream(lightPathTheme + "Pause.png"));
                    btn_pause.setGraphic(new ImageView(pause));

                    Image stepNext = new Image(getClass().getResourceAsStream(lightPathTheme + "Next step.png"));
                    btn_step_over.setGraphic(new ImageView(stepNext));

                    Image nextBreakPoint = new Image(getClass().getResourceAsStream(lightPathTheme + "Next Breakpoint.png"));
                    btn_resume.setGraphic(new ImageView(nextBreakPoint));

                    Image stop = new Image(getClass().getResourceAsStream(lightPathTheme + "Stop.png"));
                    btn_break.setGraphic(new ImageView(stop));

                    Image debug = new Image(getClass().getResourceAsStream(lightPathTheme + "Debug.png"));
                    btn_debug.setGraphic(new ImageView(debug));

                    Image rerun = new Image(getClass().getResourceAsStream(lightPathTheme + "Run.png"));
                    btn_run.setGraphic(new ImageView(rerun));

                    image_menu_new_file.setImage(new Image(getClass().getResourceAsStream(lightPathTheme + "New file.png")));
                    image_menu_save.setImage(new Image(getClass().getResourceAsStream(lightPathTheme + "Save.png")));
                }
            }
        });
    }
}
