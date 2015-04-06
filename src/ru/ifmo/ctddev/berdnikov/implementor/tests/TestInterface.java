package ru.ifmo.ctddev.berdnikov.implementor.tests;

public interface TestInterface {
    int c = 0;
    public void execute(String arg);

    @Deprecated
    int returnInt(String... a);

    public String returnString();
}
