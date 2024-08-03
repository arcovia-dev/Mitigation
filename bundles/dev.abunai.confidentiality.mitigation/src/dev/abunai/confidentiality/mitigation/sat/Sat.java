package dev.abunai.confidentiality.mitigation.sat;

import java.util.List;
import java.util.ArrayList;
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
    private List<Node> nodes;
    private List<Edge> edges;
    private List<Constraint> constraints;
    
    public List<List<Delta>> solve(List<Node> nodes,List<Edge> edges,List<Constraint> constraints) throws ContradictionException, TimeoutException {
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
        for (var node : nodes) {
            for(var inPin : node.inPins()) {
                var clause = new VecInt();
                for (var constraint : constraints) {
                    var type=constraint.label().type();
                    var value =constraint.label().value();
                    var sign = constraint.positive() ? 1 : -1;
                    if(constraint.what().equals("Node")) {
                        clause.push(sign * delta(node.name(), new NodeChar(type,value)));
                    }
                    else if(constraint.what().equals("Data")) {
                        clause.push(sign * delta(inPin.id(), new InDataChar(type,value)));
                    }
                }
                solver.addClause(clause);
            }
            
        }

        // Require node and outgoing data chars
        for (var node : nodes) {
            for(var property: node.nodeChars()) {
                solver.addClause(clause(delta(node.name(), new NodeChar(property.type(),property.value()))));
            }
            for(var outPin : node.outPins().keySet()) {
                for(var outData : node.outPins().get(outPin)) {
                    solver.addClause(clause(delta(outPin.id(), new OutDataChar(outData.type(),outData.value()))));
                }
            }
        }

        // Prohibit creation of new edges
        for (var fromNode : nodes) {
            for(var fromPin: fromNode.outPins().keySet()) {
                for (var toNode : nodes) {
                    for(var toPin : toNode.inPins()) {
                        var sign = edges.contains(new Edge(fromPin, toPin)) ? 1 : -1;
                        solver.addClause(clause(sign * edge(fromPin, toPin)));
                    }  
                }
            }
        }
        
        // Make clauses for label propagation
        for (var fromNode : nodes) {
            for(var fromPin: fromNode.outPins().keySet()) {
                for (var toNode : nodes) {
                    for(var toPin : toNode.inPins()) {
                        for (var label : labels) {
                            var edgeDataLit = edgeData(new Edge(fromPin, toPin), new InDataChar(label.type(), label.value()));
                            var outLit = delta(fromPin.id(), new OutDataChar(label.type(), label.value()));
                            // (From.OutIn AND Edge(From,To)) <=> To.EdgeInPin
                            solver.addClause(clause(-outLit, -edge(fromPin, toPin), edgeDataLit));
                            solver.addClause(clause(-edgeDataLit, outLit));
                            solver.addClause(clause(-edgeDataLit, edge(fromPin, toPin)));
                        }
                    }  
                }
            }
        }

        
        // Node has incoming data at inpin iff it receives it at least once
        for (var label : labels) {
            for (var toNode : nodes) {
                for(var toPin : toNode.inPins()) {
                    var inLit = delta(toPin.id(), new InDataChar(label.type(), label.value()));
                    var clause = new VecInt();
                    clause.push(-inLit);
                    for (var fromNode : nodes) {
                        for(var fromPin : fromNode.outPins().keySet()) {
                            var edgeDataLit = edgeData(new Edge(fromPin, toPin), new InDataChar(label.type(), label.value()));
                            solver.addClause(clause(-edgeDataLit, inLit));
                            clause.push(edgeDataLit);  
                        } 
                    }
                    solver.addClause(clause);
                } 
            }
        }
    }

    private void extractUniqueLabels() {
        labels = new HashSet<>();
        for (var constraint : constraints) {
            labels.add(constraint.label());
        }
    }

    private VecInt clause(int... literals) {
        return new VecInt(literals);
    }

    private int edge(OutPin from, InPin to) {
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

    private int delta(String where, AbstractChar characteristic) {
        var delta = new Delta(where, characteristic);
        if (!deltaToLit.containsKey(delta)) {
            deltaToLit.put(delta, solver.nextFreeVarId(true));
        }
        return deltaToLit.getValue(delta);
    }
}