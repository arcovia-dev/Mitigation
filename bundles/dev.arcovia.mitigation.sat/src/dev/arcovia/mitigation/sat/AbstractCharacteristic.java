package dev.arcovia.mitigation.sat;

public abstract class AbstractCharacteristic {
    private final CharacteristicCategory category;
    private final String type;
    private final String value;

    public AbstractCharacteristic(CharacteristicCategory category, String type, String value) {
        this.category = category;
        this.type = type;
        this.value = value;
    }

    public CharacteristicCategory category() {
        return category;
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

        AbstractCharacteristic that = (AbstractCharacteristic) o;

        return category.equals(that.category()) && type.equals(that.type()) && value.equals(that.value());
    }

    @Override
    public int hashCode() {
        int result = category.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "AbstractChar[" + "what=" + category + ", type=" + type + ", value=" + value + ']';
    }
}
