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
import rarsreborn.core.core.register.Register32File;
import rarsreborn.core.core.register.Register32;
import rarsreborn.core.core.register.floatpoint.RegisterFloat64;
import rarsreborn.core.core.register.floatpoint.RegisterFloat64ChangeEvent;
import rarsreborn.core.core.register.floatpoint.RegisterFloat64File;
import rarsreborn.core.exceptions.execution.ExecutionException;
import rarsreborn.core.simulator.PausedEvent;
import rarsreborn.core.simulator.SimulatorRiscV;
import rarsreborn.core.simulator.StoppedEvent;
import rarsreborn.core.simulator.backstepper.BackStepFinishedEvent;


public class DesignController implements Initializable {
    @FXML
    public CheckBox table_hex;
    @FXML
    public CheckBox hex_address_choice;
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
    private Button btn_newfile;
    @FXML
    private Button btn_save;
    @FXML
    private MenuButton burgerMenu;
    @FXML
    private ImageView smallNewFile;
    @FXML
    private ImageView smallSave;


    @FXML
    private TextArea console_box;
    @FXML
    private TextArea initial_file_text_box;

    @FXML
    private TabPane file_tab;

    @FXML
    private Tab initial_file_tab;
    @FXML
    private Tab execute_tab;

    @FXML
    private TableView<Register32> reg_table;
    @FXML
    private TableView<Integer> memory_table;
    @FXML
    private TableView<RegisterFloat64> float_reg_table;
    @FXML
    public TableView<Integer> code_table;
    @FXML
    public TableColumn<Integer, String> code_table_address;
    @FXML
    public TableColumn<Integer, String> code_table_code;
    @FXML
    public TableColumn<Integer, String> code_table_basic;

    @FXML
    private TableColumn<RegisterFloat64, Integer> floating_table_num;
    @FXML
    private TableColumn<RegisterFloat64, String> floating_table_name;
    @FXML
    private TableColumn<RegisterFloat64, String> floating_table_value;
    @FXML
    private TableColumn<Register32, String> reg_table_name;
    @FXML
    private TableColumn<Register32, Integer> reg_table_num;
    @FXML
    private TableColumn<Register32, String> reg_table_value;

    @FXML
    private ChoiceBox<String> memory_choice;
    @FXML
    private ChoiceBox<String> value_choice;
    @FXML
    private VBox rootVBox;
    @FXML
    private AnchorPane header;
    private boolean isDarkTheme = false;

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
                console_box.appendText("\"" + s + "\" is not an Integer");
            }
            return 0;
        }

        @Override
        public byte requestChar() {
            String s = consoleScanner.readLine();
            try {
                return Byte.parseByte(s);
            } catch (Exception e) {
                console_box.appendText("\"" + s + "\" is not an Character");
            }
            return 0;
        }
    });

    private final Register32File registers = simulator.getRegisterFile();
    private final RegisterFloat64File floatRegisters = simulator.getFloatRegisterFile();

    private final ObservableList<Register32> registersList = FXCollections.observableArrayList(registers.getAllRegisters());
    private final ObservableList<RegisterFloat64> floatRegistersList = FXCollections.observableArrayList(floatRegisters.getAllRegisters());

    private final ObservableList<IInstruction> instructions = FXCollections.observableArrayList();

    private final IMemory memory = simulator.getMemory();
    ObservableList<Integer> memoryAddresses = FXCollections.observableArrayList();
    int memoryOffset;
    private final StringBuilder consoleUneditableText = new StringBuilder();
    private TextAreaScanner consoleScanner;

    private boolean debugMode = false;

    private final HashMap<Tab, URI> filesNamesLinker = new HashMap<>();

    private int lastCodeTableIndex = 0;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        consoleScanner = new TextAreaScanner(console_box, consoleUneditableText);

        registersList.add(simulator.getProgramCounter());
        reg_table_name.setCellValueFactory(new PropertyValueFactory<>("name"));
        reg_table_num.setCellValueFactory(new PropertyValueFactory<>("numericName"));
        for (Register32 r : registersList) {
            r.addObserver(Register32ChangeEvent.class, (register32ChangeEvent) -> updateRegistersTable());
        }

        floating_table_name.setCellValueFactory(new PropertyValueFactory<>("name"));
        floating_table_num.setCellValueFactory(new PropertyValueFactory<>("numericName"));
        for (RegisterFloat64 r : floatRegistersList) {
            r.addObserver(RegisterFloat64ChangeEvent.class, (float64ChangeEvent) -> updateFloatTable());
        }
        updateRegisterTables();

        memory_choice.setItems(FXCollections.observableArrayList("0x00400000 (.text)", "0x10010000 (.data)", "0x10040000 (.heap)", "current sp"));
        value_choice.setItems(FXCollections.observableArrayList("Decimal values", "Hexadecimal values", "ASCII"));
        memory_choice.getSelectionModel().selectedIndexProperty().addListener((observable) -> {
            switch (memory_choice.getSelectionModel().getSelectedIndex()) {
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
        value_choice.getSelectionModel().selectedIndexProperty().addListener((observable -> updateMemoryTable()));
        memory_choice.getSelectionModel().select("0x10010000 (.data)");
        value_choice.getSelectionModel().select("Hexadecimal values");
        updateMemoryTable();

        code_table_address.setCellValueFactory(integerCellDataFeatures -> {
            try {
                return new ReadOnlyObjectWrapper<>(String.valueOf(integerCellDataFeatures.getValue() * 4 + Memory32.TEXT_SECTION_START));
            } catch (Exception e) {
                throw new RuntimeException();
            }
        });
        code_table_basic.setCellValueFactory(
                integerCellDataFeatures -> {
                    try {
                        return new ReadOnlyObjectWrapper<>(instructions.get(integerCellDataFeatures.getValue()).toString());
                    } catch (Exception e) {
                        throw new RuntimeException();
                    }
                });
        code_table_code.setCellValueFactory(
                integerCellDataFeatures -> {
                    try {
                        return new ReadOnlyObjectWrapper<>("0x" + Integer.toHexString(bytesToInt(instructions.get(integerCellDataFeatures.getValue()).serialize())));
                    } catch (Exception e) {
                        throw new RuntimeException();
                    }
                });

        simulator.getExecutionEnvironment().addObserver(ConsolePrintStringEvent.class, (event) -> {
            console_box.appendText(event.text());
            consoleUneditableText.append(event.text());
            consoleScanner.terminalMessage(event.text());
        });
        simulator.getExecutionEnvironment().addObserver(ConsolePrintCharEvent.class, (event) -> {
            console_box.appendText(String.valueOf((char) event.character()));
            consoleUneditableText.append((char) event.character());
            consoleScanner.terminalMessage(String.valueOf((char) event.character()));
        });
        simulator.getExecutionEnvironment().addObserver(ConsolePrintIntegerEvent.class, (event) -> {
            consoleUneditableText.append(event.value());
            console_box.appendText(String.valueOf(event.value()));
            consoleScanner.terminalMessage(String.valueOf(event.value()));
        });
        simulator.getExecutionEnvironment().addObserver(ConsolePrintIntegerHexEvent.class, (event) -> {
            console_box.appendText(Integer.toHexString(event.value()));
            consoleUneditableText.append(Integer.toHexString(event.value()));
            consoleScanner.terminalMessage(Integer.toHexString(event.value()));
        });
        simulator.getExecutionEnvironment().addObserver(ConsolePrintIntegerOctalEvent.class, (event) -> {
            console_box.appendText(Integer.toOctalString(event.value()));
            consoleUneditableText.append(Integer.toOctalString(event.value()));
            consoleScanner.terminalMessage(Integer.toOctalString(event.value()));
        });
        simulator.getExecutionEnvironment().addObserver(ConsolePrintIntegerBinaryEvent.class, (event) -> {
            console_box.appendText(Integer.toBinaryString(event.value()));
            consoleUneditableText.append(Integer.toBinaryString(event.value()));
            consoleScanner.terminalMessage(Integer.toBinaryString(event.value()));
        });
        simulator.getExecutionEnvironment().addObserver(ConsolePrintIntegerUnsignedEvent.class, (event) -> {
            console_box.appendText(Integer.toUnsignedString(event.value()));
            consoleUneditableText.append(Integer.toUnsignedString(event.value()));
            consoleScanner.terminalMessage(Integer.toUnsignedString(event.value()));
        });
        simulator.addObserver(StoppedEvent.class, (event) -> {
            debugMode = false;
            setDebugControlsVisible(false);
            btn_break.setDisable(true);
            btn_pause.setDisable(true);
            btn_resume.setDisable(true);
            code_table.getItems().clear();
            instructions.clear();
            updateButtonsState();
        });
        simulator.addObserver(BackStepFinishedEvent.class, (event) -> updateRegistersTable());
        memory.addObserver(MemoryChangeEvent.class, (event) -> memory_table.refresh());

        simulator.addObserver(PausedEvent.class, (event) -> {
            btn_pause.setDisable(true);
            btn_resume.setDisable(false);
        });
        simulator.getProgramCounter().addObserver(Register32ChangeEvent.class, (event) -> updateCodeTableFocus((event.newValue() - Memory32.TEXT_SECTION_START) / 4));
        simulator.addObserver(BackStepFinishedEvent.class, (event) -> updateCodeTableFocus((simulator.getProgramCounter().getValue() - Memory32.TEXT_SECTION_START) / 4));

        rootVBox.addEventFilter(KeyEvent.KEY_PRESSED, ke -> {
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

        console_box.setTextFormatter(new TextFormatter<String>((Change c) -> {
            String proposed = c.getControlNewText();
            if (proposed.startsWith(consoleUneditableText.toString())) {
                return c;
            } else {
                return null;
            }
        }));
        console_box.setEditable(false);

        file_tab.getTabs().remove(initial_file_tab);
        setControlsDisable(true);
        setDebugControlsVisible(false);
        btn_break.setDisable(true);
        btn_resume.setDisable(true);
        btn_pause.setDisable(true);

        createNewTab();
        file_tab.getSelectionModel().select(1);
        updateRegistersTable();
    }

    @FXML
    private void CreateNewFile() {
        createNewTab();
    }


    @FXML
    private void OnRunBtnAction() {
        preStartActions();
        try {
            Tab curTab = file_tab.getSelectionModel().getSelectedItem();
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
                        console_box.appendText(e.getMessage() + "\n");
                        btn_break.setDisable(false);
                    }
                })).start();
            }
        } catch (Exception e) {
            console_box.setText(e.getMessage()+ "\n");
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
            Tab curTab = file_tab.getSelectionModel().getSelectedItem();
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
                        console_box.appendText(e.getMessage()+ "\n");
                        btn_break.setDisable(true);
                    }
                })).start();
                setDebugControlsVisible(true);
                file_tab.getSelectionModel().select(0);
            }
        } catch (Exception e) {
            console_box.setText(e.getMessage()+ "\n");
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
                    console_box.appendText(e.getMessage() + "\n");
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
                    console_box.appendText(e.getMessage()+ "\n");
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
        file_tab.getSelectionModel().select(0);
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
        if (!file_tab.getTabs().isEmpty()) {
            if (Objects.equals(file_tab.getSelectionModel().getSelectedItem().getText(), "EXECUTE")) {
                return;
            }
            file_tab.getTabs().remove(file_tab.getSelectionModel().getSelectedItem());
        }
    }

    @FXML
    private void closeAllFiles() {
        file_tab.getTabs().removeIf(t -> !Objects.equals(t.getText(), "EXECUTE"));
    }

    @FXML
    private void closeApplication() {
        Platform.exit();
    }

    @FXML
    private void saveFileAs() {
        try {
            Tab tab = file_tab.getSelectionModel().getSelectedItem();
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
            console_box.appendText(e.getMessage()+ "\n");
        }
    }

    @FXML
    private void saveFile() {
        Tab tab = file_tab.getSelectionModel().getSelectedItem();
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
                console_box.appendText(e.getMessage()+ "\n");
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
                ((TextArea) ((Parent) ((TabPane) ((Parent) file_tab.getSelectionModel().getSelectedItem().getContent()).getChildrenUnmodifiable().get(0)).getTabs().get(0).getContent()).getChildrenUnmodifiable().get(0)).setText(content);
            }
        } catch (Exception e) {
            console_box.appendText(e.getMessage()+ "\n");
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
            header.setStyle("-fx-background-color: #F7F8FA;");
            rootVBox.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/rarsreborn/Styles/global.css")).toExternalForm());
            rootVBox.getStylesheets().remove(Objects.requireNonNull(getClass().getResource("/rarsreborn/Styles/darkTheme.css")).toExternalForm());
            updateButtonsState();
        } else {
            header.setStyle("-fx-background-color: #242628;");
            rootVBox.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/rarsreborn/Styles/darkTheme.css")).toExternalForm());
            rootVBox.getStylesheets().remove(Objects.requireNonNull(getClass().getResource("/rarsreborn/Styles/global.css")).toExternalForm());
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
            if (file_tab.getTabs().size() == 1) {
                setControlsDisable(true);
            }
        });
        file_tab.getTabs().add(newTab);
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
        file_tab.getSelectionModel().select(newTab);
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
        reg_table.refresh();
    }

    private void updateFloatTable() {
        float_reg_table.refresh();
    }

    private void updateMemoryTable() {
        memory_table.getItems().clear();
        memoryAddresses.clear();
        for (int i = 0; i < 8; i++) {
            memoryAddresses.add(memoryOffset + (i * 32));
        }
        StringBuilder buildString = new StringBuilder();
        if (!hex_address_choice.isSelected()) {
            //noinspection unchecked
            ((TableColumn<Integer, String>) memory_table.getColumns().get(0)).setCellValueFactory(integerCellDataFeatures -> {
                try {
                    return new ReadOnlyObjectWrapper<>(String.valueOf(integerCellDataFeatures.getValue()));
                } catch (Exception e) {
                    throw new RuntimeException();
                }
            });
        } else {
            //noinspection unchecked
            ((TableColumn<Integer, String>) memory_table.getColumns().get(0)).setCellValueFactory(integerCellDataFeatures -> {
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
        switch (value_choice.getSelectionModel().getSelectedIndex()) {
            case 0:
                for (int i = 1; i < 9; i++) {
                    int finalI = i;
                    //noinspection unchecked
                    ((TableColumn<Integer, String>) memory_table.getColumns().get(i)).setCellValueFactory(integerCellDataFeatures -> {
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
                    ((TableColumn<Integer, String>) memory_table.getColumns().get(i)).setCellValueFactory(integerCellDataFeatures -> {
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
                    ((TableColumn<Integer, String>) memory_table.getColumns().get(i)).setCellValueFactory(integerCellDataFeatures -> {
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
        memory_table.getItems().addAll(memoryAddresses);
        memory_table.refresh();
    }

    private void preStartActions() {
        consoleUneditableText.setLength(0);
        console_box.setText("");
        consoleScanner.update();
        console_box.setEditable(true);
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
        reg_table.getItems().clear();
        float_reg_table.getItems().clear();
        if (table_hex.isSelected()) {
            floating_table_value.setCellValueFactory(integerCellDataFeatures -> {
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
            reg_table_value.setCellValueFactory(integerCellDataFeatures -> {
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
            floating_table_value.setCellValueFactory(new PropertyValueFactory<>("float"));
            reg_table_value.setCellValueFactory(new PropertyValueFactory<>("value"));
        }
        reg_table.getItems().addAll(registersList);
        float_reg_table.getItems().addAll(floatRegistersList);
    }

    private void updateCodeTable(){
        final ObservableList<Integer> ints = FXCollections.observableArrayList();
        for (int i = 0; i < instructions.size(); i++){
            ints.add(i);
        }
        code_table.getItems().addAll(ints);
    }

    private int bytesToInt(byte[] bytes){
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        return byteBuffer.getInt();
    }

    private void updateCodeTableFocus(int index){
        code_table.getSelectionModel().select(index);
        lastCodeTableIndex = index;
    }
    @FXML
    private void codeTableMouseClicked(){
        updateCodeTableFocus(lastCodeTableIndex);
    }

    public void updateButtonsState() {
        String darkPathTheme = "/rarsreborn/Images/darkTheme/";
        String lightPathTheme = "/rarsreborn/Images/lightTheme/";
        Platform.runLater(() -> {

            if (simulator.isRunning() && !debugMode && !simulator.isPaused()) {
                if (isDarkTheme) {
                    Image burger = new Image(getClass().getResourceAsStream(darkPathTheme + "burger.png"));
                    burgerMenu.setGraphic(new ImageView(burger));

                    Image newFile = new Image(getClass().getResourceAsStream(darkPathTheme + "New file.png"));
                    btn_newfile.setGraphic(new ImageView(newFile));

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


                    smallNewFile.setImage(new Image(getClass().getResourceAsStream(darkPathTheme + "New file.png")));
                    smallSave.setImage(new Image(getClass().getResourceAsStream(darkPathTheme + "Save.png")));

                } else {
                    Image burger = new Image(getClass().getResourceAsStream(lightPathTheme + "Menu.png"));
                    burgerMenu.setGraphic(new ImageView(burger));

                    Image newFile = new Image(getClass().getResourceAsStream(lightPathTheme + "New file.png"));
                    btn_newfile.setGraphic(new ImageView(newFile));

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

                    smallNewFile.setImage(new Image(getClass().getResourceAsStream(lightPathTheme + "New file.png")));
                    smallSave.setImage(new Image(getClass().getResourceAsStream(lightPathTheme + "Save.png")));
                }
            } else if (debugMode) {
                if (isDarkTheme) {
                    Image burger = new Image(getClass().getResourceAsStream(darkPathTheme + "burger.png"));
                    burgerMenu.setGraphic(new ImageView(burger));

                    Image newFile = new Image(getClass().getResourceAsStream(darkPathTheme + "New file.png"));
                    btn_newfile.setGraphic(new ImageView(newFile));

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


                    smallNewFile.setImage(new Image(getClass().getResourceAsStream(darkPathTheme + "New file.png")));
                    smallSave.setImage(new Image(getClass().getResourceAsStream(darkPathTheme + "Save.png")));
                } else {
                    Image burger = new Image(getClass().getResourceAsStream(lightPathTheme + "Menu.png"));
                    burgerMenu.setGraphic(new ImageView(burger));

                    Image newFile = new Image(getClass().getResourceAsStream(lightPathTheme + "New file.png"));
                    btn_newfile.setGraphic(new ImageView(newFile));

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

                    smallNewFile.setImage(new Image(getClass().getResourceAsStream(lightPathTheme + "New file.png")));
                    smallSave.setImage(new Image(getClass().getResourceAsStream(lightPathTheme + "Save.png")));
                }
            } else if (!simulator.isRunning()) {
                if (isDarkTheme) {
                    Image burger = new Image(getClass().getResourceAsStream(darkPathTheme + "burger.png"));
                    burgerMenu.setGraphic(new ImageView(burger));

                    Image newFile = new Image(getClass().getResourceAsStream(darkPathTheme + "New file.png"));
                    btn_newfile.setGraphic(new ImageView(newFile));

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


                    smallNewFile.setImage(new Image(getClass().getResourceAsStream(darkPathTheme + "New file.png")));
                    smallSave.setImage(new Image(getClass().getResourceAsStream(darkPathTheme + "Save.png")));

                } else {
                    Image burger = new Image(getClass().getResourceAsStream(lightPathTheme + "Menu.png"));
                    burgerMenu.setGraphic(new ImageView(burger));

                    Image newFile = new Image(getClass().getResourceAsStream(lightPathTheme + "New file.png"));
                    btn_newfile.setGraphic(new ImageView(newFile));

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

                    smallNewFile.setImage(new Image(getClass().getResourceAsStream(lightPathTheme + "New file.png")));
                    smallSave.setImage(new Image(getClass().getResourceAsStream(lightPathTheme + "Save.png")));
                }
            }
        });
    }
}
