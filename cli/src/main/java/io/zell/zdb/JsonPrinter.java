package io.zell.zdb;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.function.Consumer;

/**
 * Utility class used by several commands to print valid json.
 * It will surround  json object, with c
 */
public class JsonPrinter {

    private boolean moreThanOneElement = false;

    private final PrintStream stream;


    public JsonPrinter() {
        this(System.out);
    }

    public JsonPrinter(PrintStream stream) {
        this.stream = stream;
    }

    private void printHeading() {
        stream.print("{\"data\":[");
    }

    private void printSeparator() {
        stream.print(",");
    }

    public void printElement(String element) {
        if (moreThanOneElement) {
            printSeparator();
        }
        stream.print(element);
        moreThanOneElement = true;
    }

    private void printEnd() {
        stream.print("]}");
    }

    public void surround(Consumer<Consumer<String>> toBeSurrounded) {
        printHeading();
        toBeSurrounded.accept(this::printElement);
        printEnd();
    }

    public void reset() {
        moreThanOneElement = false;
    }
}
