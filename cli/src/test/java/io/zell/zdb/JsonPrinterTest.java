package io.zell.zdb;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class JsonPrinterTest {


    private ByteArrayOutputStream out;
    private JsonPrinter jsonPrinter;

    @BeforeEach
    private void setup() {
        out = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(out);
        jsonPrinter = new JsonPrinter(printStream);
    }

    @Test
    public void shouldPrintEmptyDataObject() {
        // given

        // when
        jsonPrinter.surround((printer) -> {});

        // then
        Assertions.assertThat(out.toString()).isEqualTo("{\"data\":[]}");
    }

    @Test
    public void shouldPrintDataObjectWithOnElement() {
        // given

        // when
        jsonPrinter.surround((printer) -> {printer.accept("{\"foo\":1}");});

        // then
        Assertions.assertThat(out.toString()).isEqualTo("{\"data\":[{\"foo\":1}]}");
    }

    @Test
    public void shouldPrintDataObjectWithMoreElements() {
        // given

        // when
        jsonPrinter.surround((printer) -> {
            printer.accept("{\"foo\":1}");
            printer.accept("{\"foo\":1}");
            printer.accept("{\"foo\":1}");
        });

        // then
        Assertions.assertThat(out.toString()).isEqualTo("{\"data\":[{\"foo\":1},{\"foo\":1},{\"foo\":1}]}");
    }
}
