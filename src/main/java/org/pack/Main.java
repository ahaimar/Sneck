package org.pack;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {

        Window window = Window.getWindow();

        // Create a window with the default
        Thread thread = new Thread(window);
        thread.start();
    }
}