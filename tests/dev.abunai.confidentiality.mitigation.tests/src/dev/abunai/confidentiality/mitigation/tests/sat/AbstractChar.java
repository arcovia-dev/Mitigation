package dev.abunai.confidentiality.mitigation.tests.sat;

public abstract class AbstractChar {
    private final String what;
    private final String type;
    private final String value;

    public AbstractChar(String what, String type, String value) {
        this.what = what;
        this.type = type;
        this.value = value;
    }

    public String what() {
        return what;
    }

    public String type() {
        return type;
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        AbstractChar that = (AbstractChar) o;

        return what.equals(that.what()) && type.equals(that.type()) && value.equals(that.value());
    }

    @Override
    public int hashCode() {
        int result = what.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "AbstractChar[" + "what=" + what + ", type=" + type + ", value=" + value + ']';
    }
}
