package rarsreborn;

import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.ArrayList;

public class TextAreaScanner{
    private final TextArea text;
    private final ArrayList<String> inputQueue = new ArrayList<>();
    private int charPtr = 0;

    TextAreaScanner(TextArea area, StringBuilder uneditable){
        text = area;
        text.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                event.consume();
                text.appendText("\n");
                int start = charPtr;
                while (text.getText().charAt(charPtr) != '\n') {
                    charPtr++;
                }
                inputQueue.add(text.getText().substring(start, charPtr++));
                uneditable.append(text.getText().substring(start));
            }
        });
    }
    public String readLine() {
        while (inputQueue.isEmpty()){
            try {
                Thread.sleep(1);
            } catch (Exception e){
                System.out.println(e.getMessage());
            }

        }
        return inputQueue.removeFirst().split("\n")[0];
    }

    public void update(){
        inputQueue.clear();
        charPtr = 0;
    }

    public void terminalMessage(String s){
        charPtr += s.length();
    }
}