package dev.arcovia.mitigation.uncertainty.helper;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecordTableRendererLocaleTest {

    private record Row(
            @ReportColumn(value = "ms", format = ReportColumn.Format.MICROSECONDS_AS_MS) long us,
            @ReportColumn(value = "MiB", format = ReportColumn.Format.BYTES_AS_MIB) long bytes
    ) {
    }

    @Test
    void writesADecimalPointOnAMachineSetToADecimalComma() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            String rendered = RecordTableRenderer.render(
                    List.of(new Row(1_500_000L, 1024L * 1024)), Row.class);
            assertTrue(rendered.contains("1500.000"),
                    "microseconds must render with a decimal point, got:\n" + rendered);
            assertTrue(rendered.contains("1.00"),
                    "bytes must render with a decimal point, got:\n" + rendered);
            assertFalse(rendered.contains(","),
                    "no cell may carry a decimal comma, got:\n" + rendered);
        } finally {
            Locale.setDefault(original);
        }
    }
}
