package com.ryandens;


public final class HelloWorld {

    public static void main(final String[] args) {
        System.out.println("Hello World!");
        // Echo a system property so functional tests can verify that JVM args configured via
        // `applicationDefaultJvmArgs` are actually applied at runtime and not dropped/mangled by the plugin.
        final String greeting = System.getProperty("greeting");
        if (greeting != null) {
            System.out.println("greeting=[" + greeting + "]");
        }
    }
}
