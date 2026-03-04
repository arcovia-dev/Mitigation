package dev.arcovia.mitigation.smt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.apache.log4j.Logger;
import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.AND;
import org.dataflowanalysis.dfd.datadictionary.AbstractAssignment;
import org.dataflowanalysis.dfd.datadictionary.Assignment;
import org.dataflowanalysis.dfd.datadictionary.DataDictionary;
import org.dataflowanalysis.dfd.datadictionary.ForwardingAssignment;
import org.dataflowanalysis.dfd.datadictionary.Label;
import org.dataflowanalysis.dfd.datadictionary.LabelReference;
import org.dataflowanalysis.dfd.datadictionary.NOT;
import org.dataflowanalysis.dfd.datadictionary.OR;
import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.datadictionary.SetAssignment;
import org.dataflowanalysis.dfd.datadictionary.TRUE;
import org.dataflowanalysis.dfd.datadictionary.Term;
import org.dataflowanalysis.dfd.datadictionary.UnsetAssignment;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Global;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.Model;
import com.microsoft.z3.Optimize;
import com.microsoft.z3.Status;

import dev.arcovia.mitigation.smt.config.Config;
import dev.arcovia.mitigation.smt.config.CostConfig;
import dev.arcovia.mitigation.smt.constraints.ConstraintTranslator;
import dev.arcovia.mitigation.smt.cost.CostFunction;
import dev.arcovia.mitigation.smt.operations.NodeLabelAddOperation;
import dev.arcovia.mitigation.smt.operations.NodeLabelRemoveOperation;
import dev.arcovia.mitigation.smt.operations.Operation;
import dev.arcovia.mitigation.smt.operations.SetAssignmentOperation;
import dev.arcovia.mitigation.smt.operations.UnsetAssignmentOperation;
import dev.arcovia.mitigation.smt.preprocess.PreprocessingResult;
import dev.arcovia.mitigation.smt.util.SMTUtil;
import dev.arcovia.mitigation.smt.util.Util;
import dev.arcovia.mitigation.smt.util.Z3NativeLoader;

/**
 * Central orchestrating class for interfacing with the Z3 Prover
 */
public class SMT {
    private final Logger logger = Logger.getLogger(getClass());
    private Context ctx;

    /**
     * Returns the context that all encoding is done
     * @return Context
     */
    public Context getCtx() {
        return ctx;
    }

    /**
     * Returns a map of all DFDVertices to their respective incoming flows
     * @return Incoming Flows Map
     */
    public Map<DFDVertex, List<TFGFlow>> getVertexIncomingFlows() {
        return pre.vertexIncomingFlows();
    }

    /**
     * Returns a Map of mappings of TFG Flow Labels to their respective label expressions
     * @return Flow Label Map
     */
    public Map<TFGFlow, Map<Label, BoolExpr>> getFlowLabels() {
        return flowLabels;
    }

    /**
     * Returns the Datadictionary of the DFD
     * @return Datadictionary
     */
    public DataDictionary getDD() {
        return pre.dfd()
                .dataDictionary();
    }

    /**
     * Returns a map of mappings of Node Labels to their respective Label Expressions
     * @return Node Label Map
     */
    public Map<Node, Map<Label, BoolExpr>> getNodeLabels() {
        return nodeLabels;
    }

    private Optimize opt;
    private PreprocessingResult pre;
    private IntExpr costFunction;
    private Config config;
    private List<AnalysisConstraint> constraints;
    // Contains the Node Labels for each relevant Label in the input DFD
    private Map<Node, Map<Label, BoolExpr>> nodeLabelRef;
    // Contains the variable Node Labels that the solver modifies to find a solution
    private Map<Node, Map<Label, BoolExpr>> nodeLabels;
    // Contains the Labels that are propagated along TFG Flows
    private Map<TFGFlow, Map<Label, BoolExpr>> flowLabels;
    // Contains the Set Assignments for each relevant pin x label combination that
    // the solver modifies to find a solution
    private Map<Pin, Map<Label, BoolExpr>> pinSet;
    // Contains the Unset Assignments for each relevant pin x label combination that
    // the solver modifies to find a solution
    private Map<Pin, Map<Label, BoolExpr>> pinUnset;

    // This flag can be set to true so the solver prints statistics to stdout during
    // solving
    private static final boolean verboseSolver = false;

    /**
     * The constructor encodes the problem into the Z3 Solver.
     * @param pre The preprocessing result that is used as a basis for encoding
     * @param constraints The user-supplied neverFlows constraints
     * @param config The solving configuration
     */
    public SMT(PreprocessingResult pre, List<AnalysisConstraint> constraints, Config config) {
        this.config = config;
        this.constraints = constraints;
        Z3NativeLoader.ensureLoaded();
        if (verboseSolver) {
            Global.setParameter("verbose", "2");
        }
        this.ctx = new Context();
        this.opt = ctx.mkOptimize();
        this.pre = pre;
        nodeLabelRef = new HashMap<>();
        nodeLabels = new HashMap<>();
        flowLabels = new HashMap<>();
        pinSet = new HashMap<>();
        pinUnset = new HashMap<>();
        // Initializes Nodes and Pins
        initializeStructure();

        CostConfig costConfig = config.getCostConfig();
        // Custom node and pin factors (or default) are overwritten if this is true
        if (costConfig.isWeighTFGs()) {
            costConfig = weighTFGs(costConfig);
        }

        // Label Costs are parsed from Strings to the concrete Label obejcts
        Map<Label, Integer> addLabelCost = Util.transformLabelCosts(pre.dfd()
                .dataDictionary(), costConfig.getAddLabelCost());
        Map<Label, Integer> removeLabelCost = Util.transformLabelCosts(pre.dfd()
                .dataDictionary(), costConfig.getRemoveLabelCost());

        CostFunction costFunctionBuilder = CostFunction.create(ctx);
        costFunctionBuilder = addNodeLabelCosts(costFunctionBuilder, costConfig, addLabelCost, removeLabelCost);
        costFunctionBuilder = addPinSetCosts(costFunctionBuilder, costConfig, addLabelCost);
        costFunctionBuilder = addPinUnsetCosts(costFunctionBuilder, costConfig, removeLabelCost);

        // Z3 Expression that represents the cost function
        costFunction = costFunctionBuilder.build();
        // Encodes propagated labels for TFG Flows into Z3 Expressions
        createDataFlowExpressions();
        // Asserts the user supplied Confidentiality constraints
        createUserConstraints(constraints);
        // Instructs the solver to minimize the costs function
        opt.MkMinimize(costFunction);
    }

    /**
     * Appends node label costs to the supplied cost function
     * @param cost The input cost function
     * @param costConfig Configuration for creating the cost function
     * @param addLabelCost Costs for adding a label to a node
     * @param removeLabelCost Costs removing a label from a node
     * @return Partial cost function for node labels
     */
    private CostFunction addNodeLabelCosts(CostFunction cost, CostConfig costConfig, Map<Label, Integer> addLabelCost,
            Map<Label, Integer> removeLabelCost) {
        // For all nodes that have modifiable labels
        for (Entry<Node, Map<Label, BoolExpr>> map : nodeLabelRef.entrySet()) {
            // Find the weight of this modifying labels on this node. Defaults to 1
            int nodeCost = costConfig.getNodeFactor()
                    .getOrDefault(map.getKey(), 1);
            // For all modifiable labels
            for (Entry<Label, BoolExpr> ref : map.getValue()
                    .entrySet()) {
                // Node Label Addition if the node does not have the label
                if (!map.getKey()
                        .getProperties()
                        .contains(ref.getKey())) {
                    // Add the cost. If label cost for addition is not defined, default to 1
                    cost.add(nodeLabels.get(map.getKey())
                            .get(ref.getKey()), ref.getValue(), addLabelCost.getOrDefault(ref.getKey(), 1) * nodeCost);
                    // Node Label Removal if the node already has the label
                } else {
                    // If label cost for removal is not defined default to 1
                    cost.add(nodeLabels.get(map.getKey())
                            .get(ref.getKey()), ref.getValue(), removeLabelCost.getOrDefault(ref.getKey(), 1) * nodeCost);
                }
            }
        }
        return cost;
    }

    /**
     * Appends the cost of modifying pin label additions to the supplied cost function
     * @param cost The input cost function
     * @param costConfig Cost configuration
     * @param addLabelCost The cost for adding labels
     * @return Partial cost function for label addition at pins
     */
    private CostFunction addPinSetCosts(CostFunction cost, CostConfig costConfig, Map<Label, Integer> addLabelCost) {
        // For all pins that could add labels
        for (Entry<Pin, Map<Label, BoolExpr>> map : pinSet.entrySet()) {
            // If pin factor is defined fetch it here, default 1
            int pinCost = costConfig.getPinFactor()
                    .getOrDefault(map.getKey(), 1);
            // For all labels that this pin could set
            for (Entry<Label, BoolExpr> set : map.getValue()
                    .entrySet()) {
                // Add cost for adding label, label cost dfaults to 1
                cost.add(set.getValue(), ctx.mkFalse(), addLabelCost.getOrDefault(set.getKey(), 1) * pinCost);
            }
        }
        return cost;
    }

    /**
     * Appends the cost of modifying pin label removal to the supplied cost function
     * @param cost The input cost function
     * @param costConfig Cost configuration
     * @param removeLabelCosts The cost for removing labels
     * @return Partial Cost Function for label removal at pins
     */
    private CostFunction addPinUnsetCosts(CostFunction cost, CostConfig costConfig, Map<Label, Integer> removeLabelCosts) {
        // For all pins that could remove labels
        for (Entry<Pin, Map<Label, BoolExpr>> map : pinUnset.entrySet()) {
            // If pin factor is defined fetch it here, default 1
            int pinCost = costConfig.getPinFactor()
                    .getOrDefault(map.getKey(), 1);
            // For all labels that this pin could remove
            for (Entry<Label, BoolExpr> unset : map.getValue()
                    .entrySet()) {
                // Add cost. If removal cost is undefined default to 1
                cost.add(unset.getValue(), ctx.mkFalse(), removeLabelCosts.getOrDefault(unset.getKey(), 1) * pinCost);
            }
        }
        return cost;
    }

    /**
     * Modifies the cost configuration by adding TFG-based weights for nodes and pins
     * @param costConfig The input cost configuration
     * @return the modified cost configuration
     */
    private CostConfig weighTFGs(CostConfig costConfig) {
        HashMap<Node, Integer> nodeWeights = new HashMap<>();
        HashMap<Pin, Integer> pinWeights = new HashMap<>();
        // Count each occurrence of a node in a TFG.
        for (DFDVertex vertex : pre.vertices()) {
            // Increment its weight
            nodeWeights.put(vertex.getReferencedElement(), nodeWeights.getOrDefault(vertex.getReferencedElement(), 0) + 1);
            // Increment the factor for all output pins that lead to this node.
            for (Pin pin : vertex.getPinFlowMap()
                    .keySet()) {
                pinWeights.put(pin, pinWeights.getOrDefault(pin, 0) + 1);
            }
        }
        costConfig.setNodeFactor(nodeWeights);
        costConfig.setPinFactor(pinWeights);
        return costConfig;
    }

    /**
     * Repairs the DFD
     * @return Record type that contains solution and additional information
     */
    public SolvingResult repair() {
        long before = System.currentTimeMillis();
        // Find solution
        Status st = opt.Check();
        long after = System.currentTimeMillis();
        long solveTime = after - before;
        // If no solution was found
        if (st != Status.SATISFIABLE) {
            logger.warn("UNSAT");
            ctx.close();
            return new SolvingResult(false, null, null, Integer.MAX_VALUE, Optional.empty(), Optional.empty(), solveTime, pre.findTFGsTime());
        } else {
            // Fetch variable assignment
            Model m = opt.getModel();
            Optional<Long> expressionTreeSize;
            // If expression tree size is requested, calculate it here
            if (config.isFindExpressionTreeSize()) {
                BoolExpr[] assertions = opt.getAssertions();
                long astNodes = SMTUtil.countAstNodes(assertions);
                expressionTreeSize = Optional.of(astNodes);
            } else {
                expressionTreeSize = Optional.empty();
            }
            // Find cost
            IntExpr costValExpr = (IntExpr) m.eval(costFunction, true);
            // Find repair operations
            List<Operation> parseActions = parseActions(m);
            DataFlowDiagramAndDictionary dfd = pre.dfd();
            // Apply them
            for (int i = 0; i < parseActions.size(); i++) {
                dfd = parseActions.get(i)
                        .doOperation(dfd);
            }
            int cost = Integer.parseInt(costValExpr.toString());
            // If configured, use DFA to check for violations after
            Optional<Integer> violationsAfter;
            if (config.isCheckForViolationsAfter()) {
                violationsAfter = Optional.of(Util.countViolations(dfd, constraints));
            } else {
                violationsAfter = Optional.empty();
            }
            ctx.close();
            return new SolvingResult(true, dfd, parseActions, cost, expressionTreeSize, violationsAfter, solveTime, pre.findTFGsTime());
        }
    }

    /**
     * Assert never flows constraints
     * @param constraints never flows constraints
     */
    private void createUserConstraints(List<AnalysisConstraint> constraints) {
        ConstraintTranslator constraintTranslator = new ConstraintTranslator(this);

        // No vertex should satisfy any constraint
        for (AnalysisConstraint constr : constraints) {
            for (DFDVertex vertex : pre.vertices()) {
                // Assert that the constarint is not satisfied for this vertex
                opt.Assert(new BoolExpr[] {constraintTranslator.translateConstraint(constr, vertex)});
            }
        }
    }

    /**
     * Creates label expressions for a TFG Flow
     * @param flow The flow that expressions will be created for
     * @param outPinToAss Mapping of Output pins to their respective assignments
     */
    private void createDataFlowExpression(TFGFlow flow, Map<Pin, List<AbstractAssignment>> outPinToAss) {
        // If the expression for this flow has already been created just return. May
        // happen because of recursion
        if (flowLabels.get(flow) != null) {
            return;
        } else {
            // Ensure that the expressions for flows that this one depends on have already
            // been created.
            // For forwarding assignments
            flow.getThisFlowForwards()
                    .values()
                    .forEach(x -> x.forEach(y -> createDataFlowExpression(y, outPinToAss)));
            // For Assign Assignments
            flow.getThisFlowEvaluatesOn()
                    .values()
                    .forEach(x -> x.forEach(y -> createDataFlowExpression(y, outPinToAss)));
            flowLabels.put(flow, new HashMap<>());
            Pin pin = flow.getSrcPin();
            List<AbstractAssignment> assignments = outPinToAss.get(pin);
            // The expression have to be defined for all relevant labels, even if not all
            // can be added or removed.
            // This is important so the constraints can later be created on these
            // expressions
            Set<Label> allDataLabels = new HashSet<>();
            allDataLabels.addAll(pre.relevantDataLabelsAdd());
            allDataLabels.addAll(pre.relevantDataLabelsRemove());
            // Distinct expression for each label
            for (Label label : allDataLabels) {
                // Initially the label is not propagated
                BoolExpr labelExpr = ctx.mkFalse();
                // As the propagated labels are dependent on the ORDER of the assignments we
                // iterate in that order.
                // Later Assignments may override earlier ones
                for (int i = 0; i < assignments.size(); i++) {
                    AbstractAssignment assignment = assignments.get(i);
                    // If a set assignment adds this label, any earlier state can be overwritten as
                    // it will definitely be propagated
                    if (assignment instanceof SetAssignment cast && cast.getOutputLabels()
                            .contains(label)) {
                        labelExpr = ctx.mkTrue();
                        // Analogously if a unset assignment removes this label, any earlier state can
                        // be overwritten
                    } else if (assignment instanceof UnsetAssignment cast && cast.getOutputLabels()
                            .contains(label)) {
                        labelExpr = ctx.mkFalse();
                    } else if (assignment instanceof ForwardingAssignment cast) {
                        // Forward assignments are evaluated using the preceeding flows (may be emptry).
                        List<TFGFlow> forward = flow.getThisFlowForwards()
                                .getOrDefault(cast, new ArrayList<>());
                        for (TFGFlow pre : forward) {
                            // Fetch label expression for previous flow
                            BoolExpr preLabel = flowLabels.get(pre)
                                    .get(label);
                            // Label is propagated if the current flow already propagated it or a preceeding
                            // flow has it.
                            labelExpr = ctx.mkOr(labelExpr, preLabel);
                        }
                        // If an Assignment could influence this label
                    } else if (assignment instanceof Assignment cast && cast.getOutputLabels()
                            .contains(label)) {
                        // Find relevant flows that the term needs to be evaluated on.
                        List<TFGFlow> evaluateOn = flow.getThisFlowEvaluatesOn()
                                .getOrDefault(cast, new ArrayList<>());
                        // The label is propagated if the term evaluates to true, else it is not
                        // propagated, therefore earlier
                        // definitions can be overwritten.
                        labelExpr = createTerm(cast.getTerm(), evaluateOn);
                    }
                }
                // After the existing Assignments are evaluated, the modifiable assignments are
                // evaluated.
                // If the pin can potentially add this label
                BoolExpr pinNewSet = pinSet.get(pin)
                        .get(label);
                if (pinNewSet != null) {
                    // Propagate it if it already gets propagated, or the set assignment is added.
                    // We can not set this to a constant true in Java Code because the value is a
                    // variable
                    labelExpr = ctx.mkOr(labelExpr, pinNewSet);
                }
                // If the pin can potentially remove this label
                BoolExpr pinNewUnset = pinUnset.get(pin)
                        .get(label);
                if (pinNewUnset != null) {
                    // Propagate it if it already gets propagated AND this pin does not remove it.
                    // Once again earlier state can not be overwritten in Java because the unset is
                    // a variable
                    labelExpr = ctx.mkAnd(labelExpr, ctx.mkNot(pinNewUnset));
                }
                flowLabels.get(flow)
                        .put(label, labelExpr);
            }
        }
    }

    /**
     * Creates Dataflow expressions for all flows
     */
    private void createDataFlowExpressions() {
        Set<TFGFlow> allFlows = pre.flows();
        Map<Pin, List<AbstractAssignment>> outPinToAss = Util.outPinToAss(pre.dfd()
                .dataFlowDiagram()
                .getNodes());
        // Dataflow Expressions only have to be created if data labels are relevant.
        // E.g. for Constraints that have no data label selectors
        // flow labels are irrelevant.
        if ((!pre.relevantDataLabelsAdd()
                .isEmpty()
                || !pre.relevantDataLabelsRemove()
                        .isEmpty())) {
            for (TFGFlow flow : allFlows) {
                createDataFlowExpression(flow, outPinToAss);
            }
        }
    }

    /**
     * Creates a Boolean Expression that represents the Logical Term of an Assign Statement.
     * @param term Assignment Term
     * @param evaluateOn Incoming Flows that the Label References of this Term will be evaluated on
     * @return Expression that encodes the term
     */
    private BoolExpr createTerm(Term term, List<TFGFlow> evaluateOn) {
        // Base case
        if (term instanceof TRUE) {
            return ctx.mkTrue();
            // Recursive not
        } else if (term instanceof NOT cast) {
            return ctx.mkNot(createTerm(cast.getNegatedTerm(), evaluateOn));
            // Recursive AND
        } else if (term instanceof AND cast) {
            List<Term> subTerms = cast.getTerms();
            List<BoolExpr> subExprs = subTerms.stream()
                    .map(x -> createTerm(x, evaluateOn))
                    .toList();
            return ctx.mkAnd(subExprs.toArray(new BoolExpr[0]));
            // Recursive OR
        } else if (term instanceof OR cast) {
            List<Term> subTerms = cast.getTerms();
            List<BoolExpr> subExprs = subTerms.stream()
                    .map(x -> createTerm(x, evaluateOn))
                    .toList();
            return ctx.mkOr(subExprs.toArray(new BoolExpr[0]));
            // Label References evaluate to true, if any of the evaluated flows propagate
            // the label.
        } else if (term instanceof LabelReference cast) {
            Label label = cast.getLabel();
            List<BoolExpr> incomingMatches = new ArrayList<>();
            for (TFGFlow f : evaluateOn) {
                BoolExpr evaluateLabel = flowLabels.get(f)
                        .get(label);
                incomingMatches.add(evaluateLabel);
            }
            return ctx.mkOr(incomingMatches.toArray(new BoolExpr[0]));
        } else {
            throw new IllegalArgumentException("Unknown term: " + term);
        }
    }

    /**
     * Initializes Labels for pins and nodes
     */
    private void initializeStructure() {
        initializePins();
        initializeNodes();
    }

    /**
     * Initializes Label Modification variables for Pins
     */
    private void initializePins() {
        List<Pin> allOutPins = pre.dfd()
                .dataDictionary()
                .getBehavior()
                .stream()
                .flatMap(x -> x.getOutPin()
                        .stream())
                .toList();

        // Depending on configuration, data labels may not be added
        Set<Label> dataLabelsAdd = config.isAddDataLabels() ? pre.relevantDataLabelsAdd() : new HashSet<>();

        // Depending on configuration, data labels may not be removed
        Set<Label> dataLabelsRemove = config.isRemoveDataLabels() ? pre.relevantDataLabelsRemove() : new HashSet<>();

        // This case only creates decision variables for assignments that can repair
        // constraint violations.
        // This is the default case. The else branch creates assignments for all
        // constraint-relevant labels.
        if (config.isOnlyRelevantModifications()) {
            for (Pin pin : allOutPins) {
                Map<Label, BoolExpr> set = new HashMap<>();

                for (Label label : dataLabelsAdd) {
                    // Creates variable, that encodes, whether this output pin adds the specified
                    // label
                    set.put(label, ctx.mkBoolConst("Pin_" + pin.getId() + "_set_" + label.getEntityName()));
                }
                pinSet.put(pin, set);
            }
            for (Pin pin : allOutPins) {
                Map<Label, BoolExpr> unset = new HashMap<>();

                for (Label label : dataLabelsRemove) {
                    // Creates variable, that encodes, whether this output pin removes the specified
                    // label
                    unset.put(label, ctx.mkBoolConst("Pin_" + pin.getId() + "_unset_" + label.getEntityName()));
                }
                pinUnset.put(pin, unset);
            }
        } else {
            // Older case. This creates useless decision variables as certain Set and Unset
            // Assignments
            // do not have to be explored to find a minimal solution
            List<Label> allDataLabels = new ArrayList<>();
            allDataLabels.addAll(dataLabelsRemove);
            allDataLabels.addAll(dataLabelsAdd);
            for (Pin pin : allOutPins) {
                Map<Label, BoolExpr> set = new HashMap<>();
                Map<Label, BoolExpr> unset = new HashMap<>();

                for (Label label : allDataLabels) {
                    set.put(label, ctx.mkBoolConst("Pin_" + pin.getId() + "_set_" + label.getEntityName()));
                    unset.put(label, ctx.mkBoolConst("Pin_" + pin.getId() + "_unset_" + label.getEntityName()));
                }
                pinSet.put(pin, set);
                pinUnset.put(pin, unset);
            }
        }
    }

    /**
     * Initializes Node Label Modification variables. Also intializes Constants that represent node labels in the input DFD.
     */
    private void initializeNodes() {
        // Depending on configuration node labels may not be added
        Set<Label> nodeLabelsAdd = config.isAddNodeLabels() ? pre.relevantNodeLabelsAdd() : new HashSet<>();
        // Depending on configuration node labels may not be removed
        Set<Label> nodeLabelsRemove = config.isRemoveNodeLabels() ? pre.relevantNodeLabelsRemove() : new HashSet<>();
        // Still, all relevant labels have to be encoded even if they are not modifiable
        Set<Label> allNodeLabels = new HashSet<>();
        allNodeLabels.addAll(pre.relevantNodeLabelsAdd());
        allNodeLabels.addAll(pre.relevantNodeLabelsRemove());
        // Analogously to pins, this only creates decision variables for modifications
        // that could repair constraint violation
        if (config.isOnlyRelevantModifications()) {
            for (Node node : pre.dfd()
                    .dataFlowDiagram()
                    .getNodes()) {
                Set<Label> thisNodeLabels = new HashSet<>(node.getProperties());
                Map<Label, BoolExpr> thisNodeLabelRef = new HashMap<>();
                Map<Label, BoolExpr> thisNodeLabelVar = new HashMap<>();
                // For all relevant labels
                for (Label label : allNodeLabels) {
                    // If label can be added or removed. This means it appears in negated and
                    // non-negated selectors.
                    if (nodeLabelsAdd.contains(label) && nodeLabelsRemove.contains(label)) {
                        // The ref encodes the presence of the label in the input DFD
                        thisNodeLabelRef.put(label, thisNodeLabels.contains(label) ? ctx.mkTrue() : ctx.mkFalse());
                        // Modifiable var
                        thisNodeLabelVar.put(label, ctx.mkBoolConst(node.getEntityName() + "_label_" + label.getEntityName()));
                    }
                    // If label can only be added, only create it for nodes that do not posses the
                    // label. This is the case if it only appears in a negated selector.
                    else if (nodeLabelsAdd.contains(label) && !thisNodeLabels.contains(label)) {
                        thisNodeLabelRef.put(label, thisNodeLabels.contains(label) ? ctx.mkTrue() : ctx.mkFalse());

                        thisNodeLabelVar.put(label, ctx.mkBoolConst(node.getEntityName() + "_label_" + label.getEntityName()));
                    }
                    // If label can only be removed, only create it for nodes that possess the
                    // label.
                    // This is the if it only appears in non-negated selector.
                    else if (nodeLabelsRemove.contains(label) && thisNodeLabels.contains(label)) {
                        thisNodeLabelRef.put(label, thisNodeLabels.contains(label) ? ctx.mkTrue() : ctx.mkFalse());
                        thisNodeLabelVar.put(label, ctx.mkBoolConst(node.getEntityName() + "_label_" + label.getEntityName()));
                    } else {
                        // If label can neither be added or removed, make it static. No reference is
                        // needed here because it is static.
                        thisNodeLabelVar.put(label, thisNodeLabels.contains(label) ? ctx.mkTrue() : ctx.mkFalse());
                    }
                }
                if (!allNodeLabels.isEmpty()) {
                    nodeLabelRef.put(node, thisNodeLabelRef);
                    nodeLabels.put(node, thisNodeLabelVar);
                }
            }
            // Older case. Creates modification variables for all relevant node labels. Some
            // can not satisfy constraints and are therefore useless
        } else {
            if (!allNodeLabels.isEmpty()) {
                for (Node node : pre.dfd()
                        .dataFlowDiagram()
                        .getNodes()) {
                    Set<Label> thisNodeLabels = new HashSet<>(node.getProperties());

                    Map<Label, BoolExpr> thisNodeLabelRef = new HashMap<>();
                    Map<Label, BoolExpr> thisNodeLabelVar = new HashMap<>();

                    for (Label label : allNodeLabels) {
                        thisNodeLabelRef.put(label, thisNodeLabels.contains(label) ? ctx.mkTrue() : ctx.mkFalse());

                        thisNodeLabelVar.put(label, ctx.mkBoolConst(node.getEntityName() + "_label_" + label.getEntityName()));
                    }

                    nodeLabelRef.put(node, thisNodeLabelRef);
                    nodeLabels.put(node, thisNodeLabelVar);
                }
            }

        }
    }

    /**
     * Construct a list of repair operations for the input DFD
     * @param m contains the variable assignment of the solution that the Z3 Prover found
     * @return List of repair operations
     */
    private List<Operation> parseActions(Model m) {
        List<Operation> changes = new ArrayList<>();

        // For every modifiable node label
        for (Node n : nodeLabelRef.keySet()) {
            Map<Label, BoolExpr> beforeMap = nodeLabelRef.get(n);
            Map<Label, BoolExpr> afterMap = nodeLabels.get(n);

            for (Label lbl : beforeMap.keySet()) {
                BoolExpr beforeExpr = beforeMap.get(lbl);
                BoolExpr afterExpr = afterMap.get(lbl);
                // Parse to java
                boolean beforeVal = ((BoolExpr) m.evaluate(beforeExpr, true)).isTrue();
                boolean afterVal = ((BoolExpr) m.evaluate(afterExpr, true)).isTrue();

                // If it didn't exist in the input, but exists now, create a Add Operation
                if (!beforeVal && afterVal) {
                    changes.add(new NodeLabelAddOperation(n, lbl));
                    // On the other hand create a Remove Operation
                } else if (beforeVal && !afterVal) {
                    changes.add(new NodeLabelRemoveOperation(n, lbl));
                }
                // If it did not change create no operation
            }
        }
        // Evaluate Set Assignments
        for (Pin p : pinSet.keySet()) {
            Map<Label, BoolExpr> setMap = pinSet.get(p);

            for (Label label : setMap.keySet()) {
                BoolExpr setExpr = setMap != null ? setMap.get(label) : null;
                // If the pin could set the label and it actually did, create the operation
                if (setExpr != null && ((BoolExpr) m.evaluate(setExpr, true)).isTrue()) {
                    changes.add(new SetAssignmentOperation(p, label));
                }
            }
        }
        // Evaluate Unset Assignments
        for (Pin p : pinUnset.keySet()) {
            Map<Label, BoolExpr> unsetMap = pinUnset.get(p);
            for (Label label : unsetMap.keySet()) {
                BoolExpr unsetExpr = unsetMap != null ? unsetMap.get(label) : null;

                // If the pin could unset the label and it actually did, create the operation
                if (unsetExpr != null && ((BoolExpr) m.evaluate(unsetExpr, true)).isTrue()) {
                    changes.add(new UnsetAssignmentOperation(p, label));
                }
            }
        }

        return changes;
    }
}
