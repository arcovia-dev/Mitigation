package dev.arcovia.mitigation.ilp;

import java.util.List;

public record Mitigation(ActionTerm mitigation, double cost, List<List<Mitigation>> required) {
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Mitigation))
            return false;

        Mitigation m = (Mitigation) o;

        return this.mitigation()
                .equals(m.mitigation());
    }
}
