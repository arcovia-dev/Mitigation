package dev.abunai.confidentiality.mitigation.tests.sat;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.IntStream;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IProblem;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

public class Sat {

    private BiMap<Delta, Integer> deltaToLit;
    private BiMap<Edge, Integer> edgeToLit;
    private BiMap<EdgeDataChar, Integer> edgeDataToLit;
    private ISolver solver;
    private Set<Label> labels;
    private Map<String, List<AbstractChar>> nodes;
    private List<Edge> edges;
    private List<Constraint> constraints;
    
    public List<List<Delta>> solve(Map<String, List<AbstractChar>> nodes,List<Edge> edges,List<Constraint> constraints) throws ContradictionException, TimeoutException {
        this.nodes=nodes;
        this.edges=edges;
        this.constraints=constraints;
        
        deltaToLit = new BiMap<>();
        edgeToLit = new BiMap<>();
        edgeDataToLit = new BiMap<>();
        solver = SolverFactory.newDefault();
        
        extractUniqueLabels();

        buildClauses();

        return solveClauses();
    }

    private List<List<Delta>> solveClauses() throws TimeoutException, ContradictionException {
        IProblem problem = solver;
        List<List<Delta>> solutions = new ArrayList<>();
        
        while (problem.isSatisfiable()) {
            int[] model = problem.model();
            
            //Map literals to relevant Deltas
            var deltas = IntStream.of(model)
                    .filter(lit -> lit > 0)
                    .filter(lit -> deltaToLit.containsValue(lit))
                    .mapToObj(lit -> deltaToLit.getKey(lit))
                    .filter(delta -> !nodes.get(delta.node())
                            .contains(delta.characteristic()))
                    .filter(delta -> !delta.characteristic()
                            .what()
                            .equals("InData"))
                    .toList();
            
            //Store unique solutions
            if (!solutions.contains(deltas)) {
                solutions.add(deltas);
            }
            
            //Prohibit current solution
            var negated = new VecInt();
            for (var literal : model) {
                negated.push(-literal);
            }
            solver.addClause(negated);
        }
        return solutions;
    }

    private void buildClauses() throws ContradictionException {
        // Apply constraints
        for (var node : nodes.keySet()) {
            var clause = new VecInt();
            for (var constraint : constraints) {
                clause.push((constraint.positive() ? 1 : -1) * delta(node, constraint.characteristic()));
            }
            solver.addClause(clause);
        }

        // Require node and outgoing data chars
        for (var node : nodes.keySet()) {
            for (var characteristic : nodes.get(node)) {
                if (characteristic instanceof InDataChar cast) {
                    solver.addClause(clause(delta(node, cast.toOut())));
                } else {
                    solver.addClause(clause(delta(node, characteristic)));
                }
            }
        }

        // Prohibit creation of new edges
        for (var from : nodes.keySet()) {
            for (var to : nodes.keySet()) {
                var sign = edges.contains(new Edge(from, to)) ? 1 : -1;
                solver.addClause(clause(sign * edge(from, to)));
            }
        }

        // Make clauses for label propagation
        for (var from : nodes.keySet()) {
            for (var to : nodes.keySet()) {
                for (var label : labels) {
                    var edgeDataLit = edgeData(new Edge(from, to), new InDataChar(label.type(), label.value()));
                    var outLit = delta(from, new OutDataChar(label.type(), label.value()));
                    // (From.Outgoing AND Edge(From,To)) <=> To.EdgeIngoing
                    solver.addClause(clause(-outLit, -edge(from, to), edgeDataLit));
                    solver.addClause(clause(-edgeDataLit, outLit));
                    solver.addClause(clause(-edgeDataLit, edge(from, to)));
                }
            }
        }

        // Node has incoming data iff it receives it at least once
        for (var label : labels) {
            for (var to : nodes.keySet()) {
                var inLit = delta(to, new InDataChar(label.type(), label.value()));
                var clause = new VecInt();
                clause.push(-inLit);
                for (var from : nodes.keySet()) {
                    if (!from.equals(to)) {
                        var edgeDataLit = edgeData(new Edge(from, to), new InDataChar(label.type(), label.value()));
                        solver.addClause(clause(-edgeDataLit, inLit));
                        clause.push(edgeDataLit);
                    }
                }
                solver.addClause(clause);
            }
        }
    }

    private void extractUniqueLabels() {
        labels = new HashSet<>();
        for (var constraint : constraints) {
            labels.add(new Label(constraint.characteristic()
                    .type(),
                    constraint.characteristic()
                            .value()));
        }
    }

    private VecInt clause(int... literals) {
        return new VecInt(literals);
    }

    private int edge(String from, String to) {
        var edge = new Edge(from, to);
        if (!edgeToLit.containsKey(edge)) {
            edgeToLit.put(edge, solver.nextFreeVarId(true));
        }
        return edgeToLit.getValue(edge);
    }

    private int edgeData(Edge edge, InDataChar inDataChar) {
        var edgeData = new EdgeDataChar(edge, inDataChar);
        if (!edgeDataToLit.containsKey(edgeData)) {
            edgeDataToLit.put(edgeData, solver.nextFreeVarId(true));
        }
        return edgeDataToLit.getValue(edgeData);
    }

    private int delta(String node, AbstractChar characteristic) {
        var delta = new Delta(node, characteristic);
        if (!deltaToLit.containsKey(delta)) {
            deltaToLit.put(delta, solver.nextFreeVarId(true));
        }
        return deltaToLit.getValue(delta);
    }
}