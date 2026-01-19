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

    public List<Mitigation> solve(List<List<Mitigation>> mitigations, Set<Mitigation> allMitigations, List<List<Mitigation>> contradictions) {
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
        
        for (var contradiction : contradictions) {
            // if a contradicting pair is selected it implies that the other is not selected
            MPConstraint conflict  = solver.makeConstraint(Double.NEGATIVE_INFINITY, 1.0, counter.toString());
            counter++;
            for (var mitigation : contradiction) {
                conflict.setCoefficient(mitigationMap.getKey(mitigation), 1.0);
            }
        }
        //required implementation
        for (Mitigation mitigation : allMitigations) {
            
            var mitigationValue = mitigationMap.getKey(mitigation);
            
            List<MPVariable> requiredAlternatives = new ArrayList<>();
            int clauseIdx = 0;
            
            //each clause is a list of alternative literals
            for(List<Mitigation> clause : mitigation.required()) {
                
                MPVariable parentElement = solver.makeIntVar(0, 1, "required_" + safeName(mitigation.mitigation().toString()) + "_" + clauseIdx);
                requiredAlternatives.add(parentElement);
                
                //if the parent element is chosen all Children need to be chosen as well
                for (Mitigation m : clause) {
                    MPVariable childrenVariable = mitigationMap.getKey(m);
                    MPConstraint childEnforcement = solver.makeConstraint(Double.NEGATIVE_INFINITY, 0.0,
                            "required_child_" + counter);
                    counter++;
                    childEnforcement.setCoefficient(parentElement, 1.0);
                    childEnforcement.setCoefficient(childrenVariable, -1.0);
                }
                
                // if all children are satisfied, parent is satisfied as well:
                // clauseSatisfied >= sum(literals) - (k-1)
                int k = clause.size();
                MPConstraint c2 = solver.makeConstraint(-(k - 1), Double.POSITIVE_INFINITY,
                        "required_parent_" + counter);
                counter++;
                c2.setCoefficient(parentElement, 1.0);
                
                for (Mitigation m : clause) {
                    MPVariable v = mitigationMap.getKey(m);
                    c2.setCoefficient(v, -1.0);
                }

                clauseIdx++;
                
            }
            //if the mitigationValue is chosen, one of the alternatives needs to be chosen
            if (!requiredAlternatives.isEmpty()) {
                MPConstraint required = solver.makeConstraint(0.0, Double.POSITIVE_INFINITY,
                        "required_cover_" + counter);
                counter++;
                
                for (MPVariable y : requiredAlternatives) {
                    required.setCoefficient(y, 1.0);
                }
                
                required.setCoefficient(mitigationValue, -1.0);
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
                    // skip auxiliary variables introduced by required semantic
                    if (var.name().startsWith("required_") || var.name().startsWith("y_")) {
                        continue;
                    }
                    
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
    /***
     * Returns a string that is valid for LP/MPS export by:
     * - replacing all whitespace and illegal characters with _
     * - if only a number or an empty string is used it is set to variable x_(number)
     * @param s any arbitrary string
     * @return s
     * 
     */
    private static String safeName(String s) {
        s = s.trim().replaceAll("\\s+", "_").replaceAll("[^A-Za-z0-9_]", "_");
        if (s.isEmpty() || Character.isDigit(s.charAt(0))) s = "x_" + s;
        return s;
    }

}
