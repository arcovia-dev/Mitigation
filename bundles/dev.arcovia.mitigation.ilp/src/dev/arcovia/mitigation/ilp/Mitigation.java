package dev.arcovia.mitigation.ilp;


import dev.arcovia.mitigation.sat.Term;

public record Mitigation(Term mitigation, double cost) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Mitigation other)) return false;
        
        Mitigation m = (Mitigation) o;
        
        return this.mitigation().toString() == m.mitigation().toString();
    }
}
