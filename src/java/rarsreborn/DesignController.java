package rarsreborn;

import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.net.URL;
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
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import rarsreborn.core.Presets;
import rarsreborn.core.core.environment.ITextInputDevice;
import rarsreborn.core.core.environment.events.*;
import rarsreborn.core.core.memory.IMemory;
import rarsreborn.core.core.memory.Memory32;
import rarsreborn.core.core.memory.MemoryChangeEvent;
import rarsreborn.core.core.register.Register32ChangeEvent;
import rarsreborn.core.core.register.Register32File;
import rarsreborn.core.core.register.Register32;
import rarsreborn.core.exceptions.execution.ExecutionException;
import rarsreborn.core.simulator.Simulator32;
import rarsreborn.core.simulator.StoppedEvent;
import rarsreborn.core.simulator.backstepper.BackStepFinishedEvent;


public class DesignController implements Initializable {
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
    private TableColumn<Register32, String> reg_table_name;
    @FXML
    private TableColumn<Register32, Integer> reg_table_num;
    @FXML
    private TableColumn<Register32, Integer> reg_table_value;

    @FXML
    private AnchorPane initial_table_controls;

    @FXML
    private ChoiceBox<String> address_choice;
    @FXML
    private ChoiceBox<String> memory_choice;
    @FXML
    private ChoiceBox<String> value_choice;


    private final Simulator32 simulator = Presets.getClassicalRiscVSimulator(new ITextInputDevice() {
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
            }
            catch (Exception e){
                console_box.appendText("\"" + s + "\" is not an Integer");
            }
            return 0;
        }

        @Override
        public byte requestChar() {
            String s = consoleScanner.readLine();
            try {
                return Byte.parseByte(s);
            }
            catch (Exception e){
                console_box.appendText("\"" + s + "\" is not an Character");
            }
            return 0;
        }
    });
    private final Register32File registers = simulator.getRegisterFile();
    private final IMemory memory = simulator.getMemory();

    private final ObservableList<Register32> registersList = FXCollections.observableArrayList(registers.getAllRegisters());
    private final ObservableList<Integer> memoryAddresses = FXCollections.observableArrayList();

    private final TextAreaScanner consoleScanner = new TextAreaScanner();
    private final StringBuilder consoleUneditableText = new StringBuilder();

    private boolean debugMode = false;

    private final HashMap<Tab, URI> filesNamesLinker = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        registersList.add(simulator.getProgramCounter());
        reg_table_name.setCellValueFactory(new PropertyValueFactory<>("name"));
        reg_table_num.setCellValueFactory(new PropertyValueFactory<>("number"));
        reg_table_value.setCellValueFactory(new PropertyValueFactory<>("value"));
        reg_table.setItems(registersList);
        for (Register32 r : registersList) {
            r.addObserver(Register32ChangeEvent.class, (register32ChangeEvent) -> updateRegistersTable());
        }

        for (int i = 0; i < 8; i++){
            memoryAddresses.add(Memory32.DATA_SECTION_START + (i * 32));
        }
        for (int i = 1; i < 9; i++){
            int finalI = i;
            //noinspection unchecked
            ((TableColumn<Integer, Long>) memory_table.getColumns().get(i)).setCellValueFactory(integerCellDataFeatures -> {
                try {
                    return new ReadOnlyObjectWrapper<>(memory.getMultiple(integerCellDataFeatures.getValue() + ((finalI - 1) * 4), 4));
                } catch (Exception e) {
                    throw new RuntimeException();
                }
            });
        }
        memory_choice.setItems(FXCollections.observableArrayList("0x00400000 (.text)", "0x10000000 (.extern)", "0x10010000 (.data)", "0x10040000 (.heap)", "0xffff0000 (MMIO)", "current gp", "current sp"));
        address_choice.setItems(FXCollections.observableArrayList("Decimal addresses", "Hexadecimal addresses"));
        value_choice.setItems(FXCollections.observableArrayList("Decimal values", "Hexadecimal values", "ASCII"));
        memory_choice.getSelectionModel().select("0x10010000 (.data)");
        address_choice.getSelectionModel().select("Decimal addresses");
        value_choice.getSelectionModel().select("Decimal addresses");

        memory_table.setItems(memoryAddresses);

        simulator.getExecutionEnvironment().addObserver(ConsolePrintStringEvent.class, (event) -> {
            console_box.appendText(event.text());
            consoleUneditableText.append(event.text());
        });
        simulator.getExecutionEnvironment().addObserver(ConsolePrintCharEvent.class, (event) -> {
            console_box.appendText(String.valueOf((char) event.character()));
            consoleUneditableText.append((char) event.character());
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
        memory.addObserver(MemoryChangeEvent.class, (event) -> {
            memory_table.refresh();
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
        console_box.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                event.consume();
                console_box.appendText("\n");
                int charPtr = console_box.getText().length() - 2;
                while (console_box.getText().charAt(charPtr) != '\n') {
                    charPtr--;
                    if (charPtr == -1) {
                        charPtr = 0;
                        break;
                    }
                }
                consoleUneditableText.append(console_box.getText().substring(charPtr));
                consoleScanner.addInput(console_box.getText().substring(charPtr));
            }
        });

        file_tab.getTabs().remove(initial_file_tab);
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
                fileName[0] = ((FileCreationDesignController) fxmlLoader.getController()).getName();
                stage.close();
            });
            stage.showAndWait();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        if (!fileName[0].isEmpty()) {
            createNewTab(fileName[0]);
        }
    }

    @FXML
    private void OnRunBtnAction() {
        preStartActions();
        try {
            Tab curTab = file_tab.getSelectionModel().getSelectedItem();
            if (Objects.equals(curTab.getText(), "EXECUTE")){
                return;
            }
            String content = ((TextArea) ((AnchorPane) ((Parent) curTab.getContent()).getChildrenUnmodifiable().get(0)).getChildren().get(0)).getText();
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
    private void onStopBtnAction() {
        simulator.stop();
    }

    @FXML
    private void onDebugBtnAction() {
        preStartActions();
        debugMode = true;
        try {
            Tab curTab = file_tab.getSelectionModel().getSelectedItem();
            if (Objects.equals(curTab.getText(), "EXECUTE")){
                return;
            }
            String content = ((TextArea) ((AnchorPane) ((Parent) curTab.getContent()).getChildrenUnmodifiable().get(0)).getChildren().get(0)).getText();
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
        if (debugMode) {
            (new Thread(() -> {
                try {
                    if (simulator.isRunning()) {
                        simulator.stepBack();
                    } else {
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
        if (debugMode) {
            (new Thread(() -> {
                try {
                    if (simulator.isRunning()) {
                        simulator.runSteps(1);
                    } else {
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
    private void closeCurrentFile() {
        if (!file_tab.getTabs().isEmpty()) {
            if (Objects.equals(file_tab.getSelectionModel().getSelectedItem().getText(), "EXECUTE")){
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
            if (Objects.equals(tab.getText(), "EXECUTE")){
                return;
            }
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save File");
            fileChooser.setInitialFileName(tab.getText());
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ASM files (*.asm)", "*.asm"));

            File newFile = fileChooser.showSaveDialog(Window.getWindows().get(0));
            if (newFile != null) {
                FileWriter currentFile = new FileWriter(newFile);
                currentFile.write(((TextArea) ((Parent) ((TabPane) ((Parent) tab.getContent()).getChildrenUnmodifiable().get(0)).getTabs().get(0).getContent()).getChildrenUnmodifiable().get(0)).getText());
                currentFile.close();
                filesNamesLinker.put(tab, newFile.toURI());
                tab.setText(newFile.getName().split("\\.")[0]);
            }
        } catch (Exception e) {
            console_box.appendText(e.getMessage());
        }
    }
    @FXML
    private void saveFile() {
        Tab tab = file_tab.getSelectionModel().getSelectedItem();
        if (Objects.equals(tab.getText(), "EXECUTE")){
            return;
        }
        if (filesNamesLinker.get(tab) == null){
            saveFileAs();
        }
        else if (!Files.exists(Path.of(filesNamesLinker.get(tab)))) {
            saveFileAs();
        }
        else {
            try {
                File newFile = new File(filesNamesLinker.get(tab));
                FileWriter currentFile = new FileWriter(newFile);
                currentFile.write(((TextArea) ((Parent) ((TabPane) ((Parent) tab.getContent()).getChildrenUnmodifiable().get(0)).getTabs().get(0).getContent()).getChildrenUnmodifiable().get(0)).getText());
                currentFile.close();
            } catch (Exception e) {
                console_box.appendText(e.getMessage());
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
            console_box.appendText(e.getMessage());
        }
    }

    @FXML
    void onMemoryLeftAction() {

    }

    @FXML
    void onMemoryRightAction() {

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
    }

    private void setDebugControlsVisible(boolean visible) {
        btn_step_back.setVisible(visible);
        btn_step_over.setVisible(visible);
        btn_pause.setVisible(visible);
        btn_resume.setVisible(visible);
    }

    private void setControlsDisable(boolean enabled) {
        btn_run.setDisable(enabled);
        btn_debug.setDisable(enabled);
    }

    private void updateRegistersTable() {
        reg_table.refresh();
    }

    private void updateMemoryTable(TableView<IMemory> table){
        table.refresh();
    }

    private void preStartActions() {
        consoleUneditableText.setLength(0);
        console_box.setText("");
        consoleScanner.update();
        console_box.setEditable(true);
        simulator.stop();
    }


}