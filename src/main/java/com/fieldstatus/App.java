package com.fieldstatus;

public class App {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Field Status Board - hello, world");
        new DittoService();
        Thread.sleep(3000);
    }
}