package com.ryandens.javaagent.example;

public final class Fibonacci {

    public long iterative(final int index) {
        if (index  <= 1) return index;
        long prevFib = 1;
        long fib = 1;
        for (int i = 2; i < index; i++) {
            long temp = fib;
            fib += prevFib;
            prevFib = temp;
        }
        return fib;
    }
}