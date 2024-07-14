package rarsreborn;

import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class TextAreaScanner{
    private final TextArea textbox;
    private int textPtr = 0;
    private StringBuilder readLine = new StringBuilder();

    TextAreaScanner(TextArea textArea){
        textbox = textArea;
    }

    public String readLine() {
        textbox.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                event.consume();
                readLine = new StringBuilder(textbox.getText());
                textbox.appendText("\n");
            }
        });
        return readLine.toString();
    }

    public void update(){
        textPtr = 0;
    }
}