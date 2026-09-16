package dev.arcovia.mitigation.uncertainty.helper;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class RecordTableRenderer {

    private RecordTableRenderer() {
    }

    public static String render(List<? extends Record> rows, Class<? extends Record> type) {
        var columns = columns(type);

        if (rows.isEmpty()) {
            return renderHeader(columns);
        }

        var renderedRows = rows.stream()
                .map(row -> renderRow(row, columns))
                .toList();

        int[] widths = calculateWidths(columns, renderedRows);

        var result = new StringBuilder();

        appendRow(
                result,
                columns.stream()
                        .map(column -> column.annotation().value())
                        .toList(),
                columns,
                widths
        );

        appendSeparator(result, widths);

        renderedRows.forEach(row -> appendRow(result, row, columns, widths));

        return result.toString();
    }

    private static List<Column> columns(Class<? extends Record> type) {
        if (!type.isRecord()) {
            throw new IllegalArgumentException(
                    type.getName() + " is not a record"
            );
        }

        return Arrays.stream(type.getRecordComponents())
                .<Column>mapMulti((component, consumer) -> {
                    var annotation = component.getAnnotation(ReportColumn.class);
                    if (annotation != null) {
                        consumer.accept(new Column(component, annotation));
                    }
                })
                .toList();
    }

    private static List<String> renderRow(Record row, List<Column> columns) {
        return columns.stream()
                .map(column -> renderValue(row, column))
                .toList();
    }

    private static String renderValue(Record row, Column column) {
        try {
            var accessor = column.component().getAccessor();
            // A record's accessor is public while its declaring record often is not: the report
            // records this renders are private or package-private nested types in other packages,
            // and reflection reaches a public method of an inaccessible class only once it is
            // opened. Without this every caller outside this package fails with IllegalAccessException.
            accessor.setAccessible(true);
            var value = accessor.invoke(row);

            return column.annotation()
                    .format()
                    .format(value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not render " + column.component().getName(), e);
        }
    }

    private static int[] calculateWidths(
            List<Column> columns,
            List<List<String>> rows
    ) {
        int[] widths = new int[columns.size()];

        for (int column = 0; column < columns.size(); column++) {
            widths[column] =
                    columns.get(column).annotation().value().length();

            for (List<String> row : rows) {
                widths[column] = Math.max(
                        widths[column],
                        row.get(column).length()
                );
            }
        }

        return widths;
    }

    private static String renderHeader(List<Column> columns) {
        int[] widths = columns.stream()
                .mapToInt(column -> column.annotation().value().length())
                .toArray();

        StringBuilder result = new StringBuilder();

        appendRow(
                result,
                columns.stream()
                        .map(column -> column.annotation().value())
                        .toList(),
                columns,
                widths
        );

        return result.toString();
    }

    private static void appendRow(StringBuilder result, List<String> values, List<Column> columns, int[] widths) {
        result.append("  ");
        IntStream.range(0, values.size()).forEach(i -> {
            if (i > 0) {
                result.append(" | ");
            }
            result.append(pad(values.get(i), widths[i], columns.get(i).annotation().alignment()));
        });
        result.append('\n');
    }

    private static void appendSeparator(StringBuilder result, int[] widths) {
        result.append(Arrays.stream(widths).mapToObj("-"::repeat).collect(Collectors.joining("-+-", "  ", "\n")));
    }

    private static String pad(String value, int width, ReportColumn.Alignment alignment) {
        int padding = width - value.length();

        if (padding <= 0) {
            return value;
        }

        return switch (alignment) {
            case LEFT -> value + " ".repeat(padding);
            case RIGHT -> " ".repeat(padding) + value;
        };
    }

    private record Column(
            RecordComponent component,
            ReportColumn annotation
    ) {
    }
}
