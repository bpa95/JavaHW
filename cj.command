#!/bin/bash
javac -d . -cp ImplementorTest.jar -sourcepath src src/ru/ifmo/ctddev/berdnikov/implementor/Implementor.java
jar cmfv Manifest.txt Impl.jar ru/ifmo/ctddev/berdnikov/implementor/Implementor.class
