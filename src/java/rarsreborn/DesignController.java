package rarsreborn;

import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import rarsreborn.core.Presets;
import rarsreborn.core.core.register.Register32;
import rarsreborn.core.exceptions.compilation.CompilationException;
import rarsreborn.core.exceptions.linking.LinkingException;

import java.net.URL;
import java.util.Iterator;
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
    private TextArea console_box;

    @FXML
    private TableView<Register32> reg_table;

    @FXML
    private TableColumn<Register32, String> reg_table_name;
    @FXML
    private TableColumn<Register32, Integer> reg_table_num;


    @FXML
    private TableColumn<Register32, Integer> reg_table_value;

    ObservableList<Register32> registersList = FXCollections.observableArrayList(Presets.classical.getRegisterFile().getAllRegisters());

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        reg_table_name.setCellValueFactory(new PropertyValueFactory<Register32, String>("name"));
        reg_table_num.setCellValueFactory(new PropertyValueFactory<Register32, Integer>("number"));
        reg_table_value.setCellValueFactory(new PropertyValueFactory<Register32, Integer>("value"));
        reg_table.setItems(registersList);
    }

    private void updateRegistersTable() {
        reg_table.refresh();
    }

    @FXML
    void OnBtnRunAction(ActionEvent event) {
        console_box.setText("");
        try {
            Presets.classical.compile(code_edit_box.getText());
        } catch (Exception e) {
            console_box.appendText(e.getMessage());
        }
        Presets.classical.run();
//        Iterator<Register32> iterator = Presets.classical.getRegisterFile().getAllRegisters().iterator();
//        System.out.println("===================================");
//        while (iterator.hasNext()) {
//            Register32 current = iterator.next();
//            System.out.println("Register: " + current.getName() + ", value: " + current.getValue());
//            console_box.appendText("Register: " + current.getName() + ", value: " + current.getValue() + "\n");
//        }
//        System.out.println("===================================");
        updateRegistersTable();
    }

}