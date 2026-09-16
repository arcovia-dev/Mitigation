package dev.arcovia.mitigation.uncertainty.helper;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RecordTableRendererTest {

    public record Row(
            @ReportColumn(value = "driver", alignment = ReportColumn.Alignment.LEFT) String driver,
            @ReportColumn("value") int value,
            @ReportColumn(value = "median ms", format = ReportColumn.Format.MICROSECONDS_AS_MS) long medianUs,
            @ReportColumn(value = "retained MiB", format = ReportColumn.Format.BYTES_AS_MIB) long retainedBytes,
            String unannotated) {
    }

    private record Hidden(@ReportColumn("value") int value) {
    }

    @Test
    void widensEveryColumnToItsWidestCellAndAlignsAsDeclared() {
        String rendered = RecordTableRenderer.render(
                List.of(new Row("sourceCount", 17, 49_497_100L, 4326L * 1024 * 1024, "skipped"),
                        new Row("modelSize", 16384, 1_200L, 1024L * 1024, "skipped")),
                Row.class);

        assertEquals("  driver      | value | median ms | retained MiB\n"
                     + "  ------------+-------+-----------+-------------\n"
                     + "  sourceCount |    17 | 49497.100 |      4326.00\n"
                     + "  modelSize   | 16384 |     1.200 |         1.00\n",
                rendered);
    }

    @Test
    void rendersOnlyTheHeaderWhenThereAreNoRows() {
        assertEquals("  driver | value | median ms | retained MiB\n",
                RecordTableRenderer.render(List.of(), Row.class));
    }

    @Test
    void rejectsATypeThatIsNotARecord() {
        assertThrows(IllegalArgumentException.class,
                () -> RecordTableRenderer.render(List.of(), Record.class));
    }

    @Test
    void readsAccessorsOfANonPublicRecord() {
        assertEquals("  value\n  -----\n      7\n",
                RecordTableRenderer.render(List.of(new Hidden(7)), Hidden.class));
    }
}
