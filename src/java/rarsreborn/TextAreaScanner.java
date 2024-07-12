package rarsreborn;

import javafx.scene.control.TextArea;

public class TextAreaScanner{
    private TextArea textbox;
    private int textPtr = 0;
    private final StringBuilder readLine = new StringBuilder();

    TextAreaScanner(TextArea textArea){
        textbox = textArea;

    }

    public String readLine() {
        readLine.setLength(0);
        while (true){
            synchronized (this){
                if (textbox.getText().length() > textPtr){
                    readLine.append(textbox.getText().charAt(textPtr));
                    if (textbox.getText().charAt(textPtr) == '\n'){
                        break;
                    }
                    textPtr++;
                }
            }
        }
        return readLine.toString();
    }

    public void update(){
        textPtr = 0;
    }
}
