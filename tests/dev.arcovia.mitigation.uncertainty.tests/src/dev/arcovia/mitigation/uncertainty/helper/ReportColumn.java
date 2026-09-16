package dev.arcovia.mitigation.uncertainty.helper;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Locale;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.RECORD_COMPONENT)
public @interface ReportColumn {

    String value();

    Format format() default Format.DEFAULT;

    Alignment alignment() default Alignment.RIGHT;

    enum Format {
        DEFAULT {
            @Override
            String format(Object value) {
                return String.valueOf(value);
            }
        },

        MICROSECONDS_AS_MS {
            @Override
            String format(Object value) {
                return String.format(Locale.ROOT, "%.3f",
                        ((Number) value).doubleValue() / 1_000.0
                );
            }
        },

        BYTES_AS_MIB {
            @Override
            String format(Object value) {
                return String.format(Locale.ROOT, "%.2f",
                        ((Number) value).doubleValue() / (1024.0 * 1024.0)
                );
            }
        };

        abstract String format(Object value);
    }

    enum Alignment {
        LEFT,
        RIGHT
    }
}
