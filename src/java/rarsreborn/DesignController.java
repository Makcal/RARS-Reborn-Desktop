package rarsreborn;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import rarsreborn.core.Presets;
import rarsreborn.core.core.register.Register32;
import rarsreborn.core.exceptions.compilation.CompilationException;
import rarsreborn.core.exceptions.linking.LinkingException;

import java.util.Iterator;


public class DesignController {
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
    void OnBtnRunAction(ActionEvent event)  {
        try {
            Presets.classical.compile(code_edit_box.getText());
        }catch (Exception e) {
            System.out.println(e.getMessage());
        }
        Presets.classical.run();
        Iterator<Register32> iterator = Presets.classical.getRegisterFile().getAllRegisters().iterator();
        System.out.println("===================================");
        while (iterator.hasNext()){
            Register32 current = iterator.next();
            System.out.println("Register: " + current.getName() + ", value: " + current.getValue());
            console_box.appendText("Register: " + current.getName() + ", value: " + current.getValue() + "\n");
        }
        System.out.println("===================================");
    }
}