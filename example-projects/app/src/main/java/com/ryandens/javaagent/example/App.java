package com.ryandens.javaagent.example;

public class App {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Hello World" + new Fibonacci().iterative(5000));
        Thread.sleep(3000);
    }
}
