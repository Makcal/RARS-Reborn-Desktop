package rarsreborn;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class FileCreationDesignController {
    @FXML
    private TextField file_name_text_box;

    @FXML
    private void OnConfirmBtnAction(){
        if (!file_name_text_box.getText().isEmpty()){
            file_name_text_box.getScene().getWindow().hide();
        }
    }

    public String getName(){
        return file_name_text_box.getText();
    }
}
