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
import rarsreborn.core.simulator.events.PausedEvent;
import rarsreborn.core.simulator.SimulatorRiscV;
import rarsreborn.core.simulator.events.StoppedEvent;
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
    private AnchorPane anchor_pane_reg_table_bottom;


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
                console_text_box.appendText("\"" + s + "\" is not an Integer");
                consoleUneditableText.append("\"").append(s).append("\" is not an Integer");
            }
            return 0;
        }

        @Override
        public float requestFloat() {
            String s = consoleScanner.readLine();
            try {
                return Float.parseFloat(s);
            } catch (Exception e) {
                console_text_box.appendText("\"" + s + "\" is not an Integer");
                consoleUneditableText.append("\"").append(s).append("\" is not an Integer");
            }
            return 0;
        }

        @Override
        public double requestDouble() {
            String s = consoleScanner.readLine();
            try {
                return Double.parseDouble(s);
            } catch (Exception e) {
                console_text_box.appendText("\"" + s + "\" is not an Integer");
                consoleUneditableText.append("\"").append(s).append("\" is not an Integer");
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
                consoleUneditableText.append("\"").append(s).append("\" is not an Character");
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

    private Tab runningTab;

    private final HashMap<Tab, URI> filesNamesLinker = new HashMap<>();
    private final HashMap<Button, ImageView> lightImages = new HashMap<>();
    private final HashMap<Button, ImageView> darkImages = new HashMap<>();

    private boolean isDarkTheme = false;

    private int codeTableLastIndex = 0;
    private int memoryOffset;
    private int fontSize = 20;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        createButtonIcons("/rarsreborn/Images/lightTheme/", lightImages);
        createButtonIcons("/rarsreborn/Images/darkTheme/", darkImages);

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

        table_code_address.setCellValueFactory(tableRow -> {
            try {
                return new ReadOnlyObjectWrapper<>("0x" + Long.toHexString(tableRow.getValue() * 4 + Memory32.TEXT_SECTION_START));
            } catch (Exception e) {
                throw new RuntimeException();
            }
        });
        table_code_basic.setCellValueFactory(tableRow -> {
            try {
                return new ReadOnlyObjectWrapper<>(instructions.get(tableRow.getValue()).toString());
            } catch (Exception e) {
                throw new RuntimeException();
            }
        });
        table_code_code.setCellValueFactory(tableRow -> {
            try {
                return new ReadOnlyObjectWrapper<>("0x" + Integer.toHexString(bytesToInt(instructions.get(tableRow.getValue()).serialize())));
            } catch (Exception e) {
                throw new RuntimeException();
            }
        });

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
        check_box_memory_table_hex.setSelected(true);
        updateMemoryTable();

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
            console_text_box.appendText(String.valueOf(event.value()));
            consoleUneditableText.append(event.value());
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
            setDebugControlsVisible(false);
            clearTabsColor();

            table_code.getItems().clear();
            instructions.clear();

            updateButtonsState();
        });
        simulator.addObserver(BackStepFinishedEvent.class, (event) -> updateRegistersTable());
        simulator.addObserver(PausedEvent.class, (event) -> updateButtonsState());
        simulator.addObserver(BackStepFinishedEvent.class, (event) -> updateCodeTableFocus((simulator.getProgramCounter().getValue() - Memory32.TEXT_SECTION_START) / 4));
        simulator.getProgramCounter().addObserver(Register32ChangeEvent.class, (event) -> updateCodeTableFocus((event.newValue() - Memory32.TEXT_SECTION_START) / 4));
        memory.addObserver(MemoryChangeEvent.class, (event) -> table_memory.refresh());

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
                        if (ke.isShiftDown()) {
                            closeAllFiles();
                        } else {
                            closeCurrentFile();
                        }
                    }
                    break;
                case KeyCode.EQUALS:
                    if (ke.isControlDown()){
                        if (fontSize == 50){
                            break;
                        }
                        fontSize += 2;
                        updateTextFontScale();
                    }
                    break;
                case KeyCode.MINUS:
                    if (ke.isControlDown()){
                        if (fontSize == 10){
                            break;
                        }
                        fontSize -= 2;
                        updateTextFontScale();
                    }
                    break;
                case KeyCode.F5:
                    if (ke.isShiftDown()) {
                        runFileStepMode();
                    } else {
                        runFile();
                    }
                    break;
                case KeyCode.F6:
                    if (simulator.isPaused()) {
                        stepBack();
                    }
                    break;
                case KeyCode.F7:
                    if (simulator.isPaused()) {
                        stepOver();
                    }
                    break;
                case KeyCode.F8:
                    pauseRunning();
                    break;
                case KeyCode.F9:
                    resumeRunning();
                    break;
                case KeyCode.F10:
                    stopRunning();
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
        createNewTab();
        tab_pane_files.getSelectionModel().select(1);

        setDebugControlsVisible(false);
        updateButtonsState();
        updateTextFontScale();
    }

    @FXML
    private void createNewFile() {
        createNewTab();
    }

    @FXML
    private void runFile() {
        preStartActions();
        try {
            Tab curTab = tab_pane_files.getSelectionModel().getSelectedItem();
            if (runningTab != null && Objects.equals(curTab.getText(), "EXECUTE")) {
                curTab = runningTab;
                simulator.compile(((TextArea) ((Parent) curTab.getContent()).getChildrenUnmodifiable().get(0)).getText());
                instructions.addAll(simulator.getProgramInstructions());
                curTab.setStyle("-fx-border-color: green; -fx-border-width: 1.5px; -fx-font-size: 17px; -fx-focus-color: transparent; -fx-pref-height: 30px");
                updateCodeTable();
                updateButtonsState();
                (new Thread(() -> {
                    try {
                        simulator.startWorkerAndRun();
                    } catch (ExecutionException e) {
                        console_text_box.appendText(e.getMessage() + "\n");
                        consoleUneditableText.append(e.getMessage()).append("\n");
                    }
                })).start();
            } else if (!Objects.equals(curTab.getText(), "EXECUTE")) {
                simulator.compile(((TextArea) ((Parent) curTab.getContent()).getChildrenUnmodifiable().get(0)).getText());
                instructions.addAll(simulator.getProgramInstructions());
                curTab.setStyle("-fx-border-color: green; -fx-border-width: 1.5px; -fx-font-size: 17px; -fx-focus-color: transparent; -fx-pref-height: 30px");
                runningTab = curTab;
                updateCodeTable();
                updateButtonsState();
                (new Thread(() -> {
                    try {
                        simulator.startWorkerAndRun();
                    } catch (ExecutionException e) {
                        console_text_box.appendText(e.getMessage() + "\n");
                        consoleUneditableText.append(e.getMessage()).append("\n");
                    }
                })).start();
            }
        } catch (Exception e) {
            console_text_box.setText(e.getMessage() + "\n");
        }
    }

    @FXML
    private void stopRunning() {
        simulator.stop();
    }

    @FXML
    private void runFileStepMode() {
        preStartActions();
        try {
            Tab curTab = tab_pane_files.getSelectionModel().getSelectedItem();

            if (runningTab != null && Objects.equals(curTab.getText(), "EXECUTE")) {
                curTab = runningTab;
                simulator.compile(((TextArea) ((Parent) curTab.getContent()).getChildrenUnmodifiable().get(0)).getText());
                instructions.addAll(simulator.getProgramInstructions());
                curTab.setStyle("-fx-border-color: green; -fx-border-width: 1.5px; -fx-font-size: 17px; -fx-focus-color: transparent; -fx-pref-height: 30px");
                updateCodeTable();
                updateButtonsState();
                (new Thread(() -> {
                    try {
                        simulator.startWorker();
                    } catch (Exception e) {
                        console_text_box.appendText(e.getMessage() + "\n");
                        consoleUneditableText.append(e.getMessage()).append("\n");
                    }
                })).start();
            } else if(!Objects.equals(curTab.getText(), "EXECUTE")){
                simulator.compile(((TextArea) ((Parent) curTab.getContent()).getChildrenUnmodifiable().get(0)).getText());
                instructions.addAll(simulator.getProgramInstructions());
                curTab.setStyle("-fx-border-color: green; -fx-border-width: 1.5px; -fx-font-size: 17px; -fx-focus-color: transparent; -fx-pref-height: 30px");
                runningTab = curTab;
                updateCodeTable();
                updateButtonsState();
                (new Thread(() -> {
                    try {
                        simulator.startWorker();
                    } catch (Exception e) {
                        console_text_box.appendText(e.getMessage() + "\n");
                        consoleUneditableText.append(e.getMessage()).append("\n");
                    }
                })).start();
            }
            setDebugControlsVisible(true);
            tab_pane_files.getSelectionModel().select(0);

        } catch (Exception e) {
            console_text_box.setText(e.getMessage() + "\n");
        }
    }

    @FXML
    private void stepBack() {
        if (simulator.isPaused()) {
            (new Thread(() -> {
                try {
                    if (simulator.isRunning()) {
                        simulator.stepBack();
                    }
                } catch (Exception e) {
                    console_text_box.appendText(e.getMessage() + "\n");
                    consoleUneditableText.append(e.getMessage()).append("\n");
                }
            })).start();
        }
        updateRegistersTable();
    }

    @FXML
    private void stepOver() {
        if (simulator.isRunning()) {
            (new Thread(() -> {
                try {
                    if (simulator.isRunning()) {
                        simulator.runSteps(1);
                    }
                } catch (Exception e) {
                    console_text_box.appendText(e.getMessage() + "\n");
                    consoleUneditableText.append(e.getMessage()).append("\n");
                }
            })).start();
        }
    }

    @FXML
    private void pauseRunning() {
        simulator.pause();
        tab_pane_files.getSelectionModel().select(0);
        setDebugControlsVisible(true);
    }

    @FXML
    private void resumeRunning() {
        simulator.run();
    }

    @FXML
    private void closeCurrentFile() {
        if (!tab_pane_files.getTabs().isEmpty()) {
            if (!Objects.equals(tab_pane_files.getSelectionModel().getSelectedItem().getText(), "EXECUTE")) {
                tab_pane_files.getTabs().remove(tab_pane_files.getSelectionModel().getSelectedItem());
            }
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
            console_text_box.appendText(e.getMessage() + "\n");
            consoleUneditableText.append(e.getMessage()).append("\n");
        }
    }

    @FXML
    private void saveFile() {
        Tab tab = tab_pane_files.getSelectionModel().getSelectedItem();
        if (!Objects.equals(tab.getText(), "EXECUTE")) {
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
                    console_text_box.appendText(e.getMessage() + "\n");
                    consoleUneditableText.append(e.getMessage()).append("\n");
                }
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
                createNewTab(file.getName().split("\\.")[0]);
                (((TextArea) ((Parent) tab_pane_files.getSelectionModel().getSelectedItem().getContent()).getChildrenUnmodifiable().get(0))).setText(new String(Files.readAllBytes(file.toPath())));
            }
        } catch (Exception e) {
            console_text_box.appendText(e.getMessage() + "\n");
            consoleUneditableText.append(e.getMessage()).append("\n");
        }
    }

    @FXML
    void memoryTableLeft() {
        memoryOffset -= 32;
        if (memoryOffset >= 0) {
            updateMemoryTable();
        } else {
            memoryOffset += 32;
        }
    }

    @FXML
    void memoryTableRight() {
        memoryOffset += 32;
        if (memoryOffset <= memory.getSize()) {
            updateMemoryTable();
        } else {
            memoryOffset -= 32;
        }
    }

    @FXML
    private void changeTheme() {
        HashMap<Button, ImageView> map;
        if (isDarkTheme) {
            map = lightImages;
            anchor_pane_instruments.setStyle("-fx-background-color: #F7F8FA;");
            anchor_pane_reg_table_bottom.setStyle("-fx-background-color: #F7F8FA");
            check_box_reg_table_hex.setStyle("-fx-text-fill: #242628");
            check_box_memory_table_hex.setStyle("-fx-text-fill: #242628");
            root_VBox.setStyle("-fx-background-color: #F7F8FA");
            root_VBox.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/rarsreborn/Styles/global.css")).toExternalForm());
            root_VBox.getStylesheets().remove(Objects.requireNonNull(getClass().getResource("/rarsreborn/Styles/darkTheme.css")).toExternalForm());
            btn_burger_menu.setGraphic(new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/rarsreborn/Images/lightTheme/Menu.png")))));
        } else {
            map = darkImages;
            anchor_pane_instruments.setStyle("-fx-background-color: #242628;");
            anchor_pane_reg_table_bottom.setStyle("-fx-background-color: #242628;");
            check_box_reg_table_hex.setStyle("-fx-text-fill: #F7F8FA");
            check_box_memory_table_hex.setStyle("-fx-text-fill: #F7F8FA");
            root_VBox.setStyle("-fx-background-color: #242628");
            root_VBox.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/rarsreborn/Styles/darkTheme.css")).toExternalForm());
            root_VBox.getStylesheets().remove(Objects.requireNonNull(getClass().getResource("/rarsreborn/Styles/global.css")).toExternalForm());
            btn_burger_menu.setGraphic(new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/rarsreborn/Images/darkTheme/Menu.png")))));
        }
        map.forEach(Labeled::setGraphic);
        isDarkTheme = !isDarkTheme;
        updateButtonsState();
    }

    @FXML
    public void changeAddressView() {
        updateMemoryTable();
    }

    @FXML
    public void changeValueView() {
        updateRegisterTables();
    }

    @FXML
    private void codeTableMouseClicked() {
        updateCodeTableFocus(codeTableLastIndex);
    }

    private void createNewTab() {
        createNewTab("New file");
    }

    private void createNewTab(String fileName) {
        Tab newTab = new Tab(fileName);
        newTab.setOnClosed(event -> {
            if (tab_pane_files.getTabs().size() == 1) {
                updateButtonsState();
            }
        });
        tab_pane_files.getTabs().add(newTab);
        newTab.setStyle(execute_tab.getStyle());

        AnchorPane newAnchorPane = new AnchorPane();
        newTab.setContent(newAnchorPane);

        TextArea newTextArea = new TextArea();
        newAnchorPane.getChildren().add(newTextArea);

        newTextArea.setStyle(initial_file_text_box.getStyle());

        AnchorPane.setTopAnchor(newTextArea, 0.0);
        AnchorPane.setBottomAnchor(newTextArea, 0.0);
        AnchorPane.setRightAnchor(newTextArea, 0.0);
        AnchorPane.setLeftAnchor(newTextArea, 0.0);
        tab_pane_files.getSelectionModel().select(newTab);
        updateButtonsState();
    }

    private void setDebugControlsVisible(boolean visible) {
        btn_step_back.setVisible(visible);
        btn_step_over.setVisible(visible);
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

    private void updateCodeTable() {
        final ObservableList<Integer> ints = FXCollections.observableArrayList();
        for (int i = 0; i < instructions.size(); i++) {
            ints.add(i);
        }
        table_code.getItems().addAll(ints);
    }

    private void updateCodeTableFocus(int index) {
        table_code.getSelectionModel().select(index);
        codeTableLastIndex = index;
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

    private int bytesToInt(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        return byteBuffer.getInt();
    }

    public void updateButtonsState() {
        Platform.runLater(() -> {
            String path;
            if (isDarkTheme) {
                path = "/rarsreborn/Images/darkTheme/";
            } else {
                path = "/rarsreborn/Images/lightTheme/";
            }

            if (tab_pane_files.getTabs().size() == 1) {
                btn_run.setGraphic(new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream(path + "Run.png")))));
                btn_debug.setGraphic(new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream(path + "Debug.png")))));
                btn_run.setDisable(true);
                btn_debug.setDisable(true);
                btn_break.setDisable(true);
                btn_pause.setDisable(true);
                btn_resume.setDisable(true);
            } else if (!simulator.isRunning()) {
                btn_run.setGraphic(new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream(path + "Run.png")))));
                btn_debug.setGraphic(new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream(path + "Debug.png")))));
                btn_run.setDisable(false);
                btn_debug.setDisable(false);
                btn_break.setDisable(true);
                btn_pause.setDisable(true);
                btn_resume.setDisable(true);
            } else if (simulator.isPaused()) {
                btn_run.setGraphic(new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream(path + "Rerun.png")))));
                btn_debug.setGraphic(new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream(path + "Redebug.png")))));
                btn_run.setDisable(false);
                btn_debug.setDisable(false);
                btn_break.setDisable(false);
                btn_pause.setDisable(true);
                btn_resume.setDisable(false);
            } else {
                btn_run.setGraphic(new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream(path + "Rerun.png")))));
                btn_debug.setGraphic(new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream(path + "Redebug.png")))));
                btn_run.setDisable(false);
                btn_debug.setDisable(false);
                btn_break.setDisable(false);
                btn_pause.setDisable(false);
                btn_resume.setDisable(true);
            }
        });
    }

    private ImageView createImage(String theme, String name) {
        return new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream(theme + name))));
    }

    private void createButtonIcons(String path, HashMap<Button, ImageView> images) {
        images.put(btn_new_file, createImage(path, "New file.png"));
        images.put(btn_save, createImage(path, "Save.png"));
        images.put(btn_step_back, createImage(path, "Undo.png"));
        images.put(btn_pause, createImage(path, "Pause.png"));
        images.put(btn_step_over, createImage(path, "Next step.png"));
        images.put(btn_resume, createImage(path, "Next Breakpoint.png"));
        images.put(btn_break, createImage(path, "Stop.png"));
        images.put(btn_break, createImage(path, "Stop.png"));
        images.put(btn_left_memory, createImage(path, "LeftArrow.png"));
        images.put(btn_right_memory, createImage(path, "RightArrow.png"));
    }

    private void clearTabsColor() {
        for (Tab current : tab_pane_files.getTabs()) {
            if (isDarkTheme) {
                current.setStyle("-fx-background-color: #2B2D30; -fx-font-size: 17px; -fx-focus-color: transparent; -fx-pref-height: 26px");
            } else {
                current.setStyle("-fx-background-color: white; -fx-font-size: 17px; -fx-focus-color: transparent; -fx-pref-height: 26px");
            }
        }
    }

    private void updateTextFontScale(){
        initial_file_tab.setStyle("-fx-font-size: %dpx;".formatted(fontSize));
        console_text_box.setStyle("-fx-font-size: %dpx;".formatted(fontSize));
        for (Tab t: tab_pane_files.getTabs()){
            if (!Objects.equals(t.getText(), "EXECUTE")){
                ((Parent) t.getContent()).getChildrenUnmodifiable().get(0).setStyle("-fx-font-size: %dpx;".formatted(fontSize));
            }
        }
    }
}
