package dev.arcovia.mitigation.ilp;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.io.FileWriter;
import java.io.IOException;

import dev.arcovia.mitigation.sat.BiMap;

import java.util.ArrayList;

import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

public class ILPSolver {
    private BiMap<MPVariable, Mitigation> mitigationMap = new BiMap<>();

    public List<Mitigation> solve(List<List<Mitigation>> mitigations, Set<Mitigation> allMitigations) {
        Loader.loadNativeLibraries();
        MPSolver solver = MPSolver.createSolver("CBC_MIXED_INTEGER_PROGRAMMING");

        for (Mitigation mitigation : allMitigations) {
            MPVariable var = solver.makeIntVar(0, 1, mitigation.toString());
            mitigationMap.put(var, mitigation);
        }
        Integer counter = 0;
        for (var constraint : mitigations) {
            // Coverage constraint for the single violation: at least one mitigation active
            MPConstraint cover = solver.makeConstraint(1.0, Double.POSITIVE_INFINITY, counter.toString());
            counter++;
            for (var mitigation : constraint) {
                cover.setCoefficient(mitigationMap.getKey(mitigation), 1.0);
            }
        }

        MPObjective objective = solver.objective();

        for (var mitigation : allMitigations) {
        	objective.setCoefficient(mitigationMap.getKey(mitigation), mitigation.cost());
        }

        objective.setMinimization();

        String lpModel = solver.exportModelAsLpFormat(false);
        try (FileWriter writer = new FileWriter("model.lp")) {
            writer.write(lpModel);
        } catch (IOException e) {
            e.printStackTrace();
        }

        MPSolver.ResultStatus st = solver.solve();

        if (st == MPSolver.ResultStatus.OPTIMAL || st == MPSolver.ResultStatus.FEASIBLE) {
            List<Mitigation> chosen = new ArrayList<>();
            for (MPVariable var : solver.variables()) {
                if (var.solutionValue() > 0.5) {
                    Optional<Mitigation> mitigation = allMitigations.stream()
                            .filter(m -> var.name()
                                    .equals(m.toString()))
                            .findFirst();
                    if (mitigation.isPresent())
                        chosen.add(mitigation.get());
                }
            }
            return chosen;
        } else {
            System.out.println("No feasible solution: " + st);
            return null;
        }
    }

}
