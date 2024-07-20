package rarsreborn;

import java.util.ArrayList;

public class TextAreaScanner{
    private final ArrayList<String> inputStack = new ArrayList<>();

    public String readLine() {
        while (inputStack.isEmpty()){
            try {
                Thread.sleep(1);
            } catch (Exception e){
                System.out.println(e.getMessage());
            }

        }
        String s = inputStack.getFirst();
        inputStack.removeFirst();
        return s;
    }

    public void addInput(String s){
        inputStack.add(s);
    }

    public void update(){
        inputStack.clear();
    }
}