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
import dev.arcovia.mitigation.smt.utils.ParsingUtils;
import dev.arcovia.mitigation.smt.utils.Z3CountingUtils;
import dev.arcovia.mitigation.smt.utils.Z3NativeLoader;

/**
 * Central orchestrating class for interfacing with the Z3 Prover
 */
public class SMT {
    private final Logger logger = Logger.getLogger(getClass());
    private Context context;
    private Optimize optimize;
    private PreprocessingResult preprocesingResult;
    private IntExpr costFunction;
    private Config config;
    private List<AnalysisConstraint> constraints;
    // Contains the Node Labels for each relevant Label in the input DFD
    private Map<Node, Map<Label, BoolExpr>> nodeLabelInput;
    // Contains the variable Node Labels that the solver modifies to find a solution
    private Map<Node, Map<Label, BoolExpr>> nodeLabels;
    // Contains the Labels that are propagated along Flow instances
    private Map<FlowInstance, Map<Label, BoolExpr>> flowLabels;
    // Contains the Set Assignments for each relevant pin x label combination that
    // the solver modifies to find a solution
    private Map<Pin, Map<Label, BoolExpr>> pinNewSetAssignments;
    // Contains the Unset Assignments for each relevant pin x label combination that
    // the solver modifies to find a solution
    private Map<Pin, Map<Label, BoolExpr>> pinNewUnsetAssignments;

    // This flag can be set to true so the solver prints statistics to stdout during
    // solving
    private static final boolean verboseSolver = false;

    /**
     * The constructor encodes the problem into the Z3 Solver.
     * @param preprocessingResult The preprocessing result that is used as a basis for encoding
     * @param constraints The user-supplied neverFlows constraints
     * @param config The solving configuration
     */
    public SMT(PreprocessingResult preprocessingResult, List<AnalysisConstraint> constraints, Config config) {
        this.config = config;
        this.constraints = constraints;
        Z3NativeLoader.ensureLoaded();
        if (verboseSolver) {
            Global.setParameter("verbose", "2");
        }
        this.context = new Context();
        this.optimize = context.mkOptimize();
        this.preprocesingResult = preprocessingResult;
        nodeLabelInput = new HashMap<>();
        nodeLabels = new HashMap<>();
        flowLabels = new HashMap<>();
        pinNewSetAssignments = new HashMap<>();
        pinNewUnsetAssignments = new HashMap<>();
        // Initializes Nodes and Pins
        initializeStructure();

        CostConfig costConfig = config.costConfig();
        // Custom node and pin factors (or default) are overwritten if this is true
        if (costConfig.weighTFGs()) {
            costConfig = weighTFGs(costConfig);
        }

        // Label Costs are parsed from Strings to the concrete Label obejcts
        Map<Label, Integer> addLabelCost = ParsingUtils.transformLabelCosts(preprocessingResult.dfd()
                .dataDictionary(), costConfig.addLabelCost());
        Map<Label, Integer> removeLabelCost = ParsingUtils.transformLabelCosts(preprocessingResult.dfd()
                .dataDictionary(), costConfig.removeLabelCost());

        CostFunction costFunctionBuilder = CostFunction.create(context);
        costFunctionBuilder = addNodeLabelCosts(costFunctionBuilder, costConfig, addLabelCost, removeLabelCost);
        costFunctionBuilder = addPinSetCosts(costFunctionBuilder, costConfig, addLabelCost);
        costFunctionBuilder = addPinUnsetCosts(costFunctionBuilder, costConfig, removeLabelCost);

        // Z3 Expression that represents the cost function
        costFunction = costFunctionBuilder.build();
        // Encodes propagated labels for Flow instances into Z3 Expressions
        createDataFlowExpressions();
        // Asserts the user supplied Confidentiality constraints
        createUserConstraints(constraints);
        // Instructs the solver to minimize the costs function
        optimize.MkMinimize(costFunction);
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
        for (Entry<Node, Map<Label, BoolExpr>> thisNodeModifiableLabels : nodeLabelInput.entrySet()) {
            // Find the weight of this modifying labels on this node. Defaults to 1
            int nodeCost = costConfig.nodeFactor()
                    .getOrDefault(thisNodeModifiableLabels.getKey(), 1);
            // For all modifiable labels
            for (Entry<Label, BoolExpr> modifiableLabel : thisNodeModifiableLabels.getValue()
                    .entrySet()) {
                // Node Label Addition if the node does not have the label
                if (!thisNodeModifiableLabels.getKey()
                        .getProperties()
                        .contains(modifiableLabel.getKey())) {
                    // Add the cost. If label cost for addition is not defined, default to 1
                    cost.add(nodeLabels.get(thisNodeModifiableLabels.getKey())
                            .get(modifiableLabel.getKey()), modifiableLabel.getValue(), addLabelCost.getOrDefault(modifiableLabel.getKey(), 1) * nodeCost);
                    // Node Label Removal if the node already has the label
                } else {
                    // If label cost for removal is not defined default to 1
                    cost.add(nodeLabels.get(thisNodeModifiableLabels.getKey())
                            .get(modifiableLabel.getKey()), modifiableLabel.getValue(), removeLabelCost.getOrDefault(modifiableLabel.getKey(), 1) * nodeCost);
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
        for (Entry<Pin, Map<Label, BoolExpr>> thisPinSetLabelAssignments : pinNewSetAssignments.entrySet()) {
            // If pin factor is defined fetch it here, default 1
            int pinCost = costConfig.pinFactor()
                    .getOrDefault(thisPinSetLabelAssignments.getKey(), 1);
            // For all labels that this pin could set
            for (Entry<Label, BoolExpr> setLabelAssignment : thisPinSetLabelAssignments.getValue()
                    .entrySet()) {
                // Add cost for adding label, label cost dfaults to 1
                cost.add(setLabelAssignment.getValue(), context.mkFalse(), addLabelCost.getOrDefault(setLabelAssignment.getKey(), 1) * pinCost);
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
        for (Entry<Pin, Map<Label, BoolExpr>> thisPinUnsetLabelAssignments : pinNewUnsetAssignments.entrySet()) {
            // If pin factor is defined fetch it here, default 1
            int pinCost = costConfig.pinFactor()
                    .getOrDefault(thisPinUnsetLabelAssignments.getKey(), 1);
            // For all labels that this pin could remove
            for (Entry<Label, BoolExpr> unsetLabelAssignment : thisPinUnsetLabelAssignments.getValue()
                    .entrySet()) {
                // Add cost. If removal cost is undefined default to 1
                cost.add(unsetLabelAssignment.getValue(), context.mkFalse(), removeLabelCosts.getOrDefault(unsetLabelAssignment.getKey(), 1) * pinCost);
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
        // Clear maps to overwrite existing weights with TFG-weighted ones
        Map<Node, Integer> nodeFactor = costConfig.nodeFactor();
        nodeFactor.clear();
        Map<Pin, Integer> pinFactor = costConfig.pinFactor();
        pinFactor.clear();
        // Count each occurrence of a node in a TFG.
        for (DFDVertex vertex : preprocesingResult.vertices()) {
            // Increment its weight
            nodeFactor.put(vertex.getReferencedElement(), nodeFactor.getOrDefault(vertex.getReferencedElement(), 0) + 1);
            // Increment the factor for all output pins that lead to this node.
            for (Pin pin : vertex.getPinFlowMap()
                    .keySet()) {
                pinFactor.put(pin, pinFactor.getOrDefault(pin, 0) + 1);
            }
        }
        return costConfig;
    }

    /**
     * Repairs the DFD
     * @return Record type that contains solution and additional information
     */
    public SolvingResult repair() {
        long before = System.currentTimeMillis();
        // Find solution
        Status status = optimize.Check();
        long after = System.currentTimeMillis();
        long solveTime = after - before;
        // If no solution was found
        if (status != Status.SATISFIABLE) {
            logger.warn("UNSAT");
            context.close();
            return new SolvingResult(false, null, null, Integer.MAX_VALUE, Optional.empty(), Optional.empty(), solveTime,
                    preprocesingResult.findTFGsTime());
        } else {
            // Fetch variable assignment
            Model model = optimize.getModel();
            Optional<Long> expressionTreeSizeOptional;
            // If expression tree size is requested, calculate it here
            if (config.findExpressionTreeSize()) {
                BoolExpr[] assertions = optimize.getAssertions();
                long expressionTreeSize = Z3CountingUtils.countExpressionTreesSize(assertions);
                expressionTreeSizeOptional = Optional.of(expressionTreeSize);
            } else {
                expressionTreeSizeOptional = Optional.empty();
            }
            // Find cost
            IntExpr costValExpr = (IntExpr) model.eval(costFunction, true);
            // Find repair operations
            List<Operation> parseActions = parseActions(model);
            DataFlowDiagramAndDictionary dfd = preprocesingResult.dfd();
            // Apply them
            for (int i = 0; i < parseActions.size(); i++) {
                dfd = parseActions.get(i)
                        .doOperation(dfd);
            }
            int cost = Integer.parseInt(costValExpr.toString());
            // If configured, use DFA to check for violations after
            Optional<Integer> violationsAfter;
            if (config.checkForViolationsAfter()) {
                violationsAfter = Optional.of(ParsingUtils.countViolations(dfd, constraints));
            } else {
                violationsAfter = Optional.empty();
            }
            context.close();
            return new SolvingResult(true, dfd, parseActions, cost, expressionTreeSizeOptional, violationsAfter, solveTime,
                    preprocesingResult.findTFGsTime());
        }
    }

    /**
     * Assert never flows constraints
     * @param constraints never flows constraints
     */
    private void createUserConstraints(List<AnalysisConstraint> constraints) {
        ConstraintTranslator constraintTranslator = new ConstraintTranslator(this);

        // No vertex should satisfy any constraint
        for (AnalysisConstraint constraint : constraints) {
            for (DFDVertex vertex : preprocesingResult.vertices()) {
                // Assert that the constraint is not satisfied for this vertex
                optimize.Assert(new BoolExpr[] {constraintTranslator.translateConstraint(constraint, vertex)});
            }
        }
    }

    /**
     * Creates label expressions for a Flow instance
     * @param flow The flow instances that expressions will be created for
     * @param outPinToAssignments Mapping of Output pins to their respective assignments
     */
    private void createDataFlowExpression(FlowInstance flow, Map<Pin, List<AbstractAssignment>> outPinToAssignments) {
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
                    .forEach(x -> x.forEach(y -> createDataFlowExpression(y, outPinToAssignments)));
            // For Assign Assignments
            flow.getThisFlowEvaluatesOn()
                    .values()
                    .forEach(x -> x.forEach(y -> createDataFlowExpression(y, outPinToAssignments)));
            flowLabels.put(flow, new HashMap<>());
            Pin pin = flow.getSourcePin();
            List<AbstractAssignment> assignments = outPinToAssignments.get(pin);
            // The expression have to be defined for all relevant labels, even if not all
            // can be added or removed.
            // This is important so the constraints can later be created on these
            // expressions
            Set<Label> allDataLabels = new HashSet<>();
            allDataLabels.addAll(preprocesingResult.relevantDataLabelsAdd());
            allDataLabels.addAll(preprocesingResult.relevantDataLabelsRemove());
            // Distinct expression for each label
            for (Label label : allDataLabels) {
                // Initially the label is not propagated
                BoolExpr labelExpression = context.mkFalse();
                // As the propagated labels are dependent on the ORDER of the assignments we
                // iterate in that order.
                // Later Assignments may override earlier ones
                for (int i = 0; i < assignments.size(); i++) {
                    AbstractAssignment assignment = assignments.get(i);
                    // If a set assignment adds this label, any earlier state can be overwritten as
                    // it will definitely be propagated
                    if (assignment instanceof SetAssignment cast && cast.getOutputLabels()
                            .contains(label)) {
                        labelExpression = context.mkTrue();
                        // Analogously if a unset assignment removes this label, any earlier state can
                        // be overwritten
                    } else if (assignment instanceof UnsetAssignment cast && cast.getOutputLabels()
                            .contains(label)) {
                        labelExpression = context.mkFalse();
                    } else if (assignment instanceof ForwardingAssignment cast) {
                        // Forward assignments are evaluated using the preceeding flows (may be emptry).
                        List<FlowInstance> forward = flow.getThisFlowForwards()
                                .getOrDefault(cast, new ArrayList<>());
                        for (FlowInstance preceedingFlow : forward) {
                            // Fetch label expression for previous flow
                            BoolExpr preceedingFlowLabel = flowLabels.get(preceedingFlow)
                                    .get(label);
                            // Label is propagated if the current flow already propagated it or a preceeding
                            // flow has it.
                            labelExpression = context.mkOr(labelExpression, preceedingFlowLabel);
                        }
                        // If an Assignment could influence this label
                    } else if (assignment instanceof Assignment cast && cast.getOutputLabels()
                            .contains(label)) {
                        // Find relevant flows that the term needs to be evaluated on.
                        List<FlowInstance> preceedingFlowsToEvaluate = flow.getThisFlowEvaluatesOn()
                                .getOrDefault(cast, new ArrayList<>());
                        // The label is propagated if the term evaluates to true, else it is not
                        // propagated, therefore earlier
                        // definitions can be overwritten.
                        labelExpression = createTerm(cast.getTerm(), preceedingFlowsToEvaluate);
                    }
                }
                // After the existing Assignments are evaluated, the modifiable assignments are
                // evaluated.
                // If the pin can potentially add this label
                BoolExpr pinNewSetAssignment = pinNewSetAssignments.get(pin)
                        .get(label);
                if (pinNewSetAssignment != null) {
                    // Propagate it if it already gets propagated, or the set assignment is added.
                    // We can not set this to a constant true in Java Code because the value is a
                    // variable
                    labelExpression = context.mkOr(labelExpression, pinNewSetAssignment);
                }
                // If the pin can potentially remove this label
                BoolExpr pinNewUnsetAssignment = pinNewUnsetAssignments.get(pin)
                        .get(label);
                if (pinNewUnsetAssignment != null) {
                    // Propagate it if it already gets propagated AND this pin does not remove it.
                    // Once again earlier state can not be overwritten in Java because the unset is
                    // a variable
                    labelExpression = context.mkAnd(labelExpression, context.mkNot(pinNewUnsetAssignment));
                }
                flowLabels.get(flow)
                        .put(label, labelExpression);
            }
        }
    }

    /**
     * Creates Dataflow expressions for all flows
     */
    private void createDataFlowExpressions() {
        Set<FlowInstance> allFlows = preprocesingResult.flows();
        Map<Pin, List<AbstractAssignment>> outPinToAssignments = ParsingUtils.outPinToAssignments(preprocesingResult.dfd()
                .dataFlowDiagram()
                .getNodes());
        // Dataflow Expressions only have to be created if data labels are relevant.
        // E.g. for Constraints that have no data label selectors
        // flow labels are irrelevant.
        if ((!preprocesingResult.relevantDataLabelsAdd()
                .isEmpty()
                || !preprocesingResult.relevantDataLabelsRemove()
                        .isEmpty())) {
            for (FlowInstance flow : allFlows) {
                createDataFlowExpression(flow, outPinToAssignments);
            }
        }
    }

    /**
     * Creates a Boolean Expression that represents the Logical Term of an Assign Statement.
     * @param term Assignment Term
     * @param flowsToEvaluate Incoming Flows that the Label References of this Term will be evaluated on
     * @return Expression that encodes the term
     */
    private BoolExpr createTerm(Term term, List<FlowInstance> flowsToEvaluate) {
        // Base case
        if (term instanceof TRUE) {
            return context.mkTrue();
            // Recursive not
        } else if (term instanceof NOT cast) {
            return context.mkNot(createTerm(cast.getNegatedTerm(), flowsToEvaluate));
            // Recursive AND
        } else if (term instanceof AND cast) {
            List<Term> subTerms = cast.getTerms();
            List<BoolExpr> subExprs = subTerms.stream()
                    .map(x -> createTerm(x, flowsToEvaluate))
                    .toList();
            return context.mkAnd(subExprs.toArray(new BoolExpr[0]));
            // Recursive OR
        } else if (term instanceof OR cast) {
            List<Term> subTerms = cast.getTerms();
            List<BoolExpr> subExprs = subTerms.stream()
                    .map(x -> createTerm(x, flowsToEvaluate))
                    .toList();
            return context.mkOr(subExprs.toArray(new BoolExpr[0]));
            // Label References evaluate to true, if any of the evaluated flows propagate
            // the label.
        } else if (term instanceof LabelReference cast) {
            Label label = cast.getLabel();
            List<BoolExpr> incomingMatches = new ArrayList<>();
            for (FlowInstance f : flowsToEvaluate) {
                BoolExpr evaluateLabel = flowLabels.get(f)
                        .get(label);
                incomingMatches.add(evaluateLabel);
            }
            return context.mkOr(incomingMatches.toArray(new BoolExpr[0]));
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
        List<Pin> allOutPins = preprocesingResult.dfd()
                .dataDictionary()
                .getBehavior()
                .stream()
                .flatMap(x -> x.getOutPin()
                        .stream())
                .toList();

        // Depending on configuration, data labels may not be added
        Set<Label> dataLabelsAdd = config.addDataLabels() ? preprocesingResult.relevantDataLabelsAdd() : new HashSet<>();

        // Depending on configuration, data labels may not be removed
        Set<Label> dataLabelsRemove = config.removeDataLabels() ? preprocesingResult.relevantDataLabelsRemove() : new HashSet<>();

        // This case only creates decision variables for assignments that can repair
        // constraint violations.
        // This is the default case. The else branch creates assignments for all
        // constraint-relevant labels.
        if (config.onlyRelevantModifications()) {
            for (Pin pin : allOutPins) {
                Map<Label, BoolExpr> setAssignments = new HashMap<>();

                for (Label label : dataLabelsAdd) {
                    // Creates variable, that encodes, whether this output pin adds the specified
                    // label
                    setAssignments.put(label, context.mkBoolConst("Pin_" + pin.getId() + "_set_" + label.getEntityName()));
                }
                pinNewSetAssignments.put(pin, setAssignments);
            }
            for (Pin pin : allOutPins) {
                Map<Label, BoolExpr> unsetAssignments = new HashMap<>();

                for (Label label : dataLabelsRemove) {
                    // Creates variable, that encodes, whether this output pin removes the specified
                    // label
                    unsetAssignments.put(label, context.mkBoolConst("Pin_" + pin.getId() + "_unset_" + label.getEntityName()));
                }
                pinNewUnsetAssignments.put(pin, unsetAssignments);
            }
        } else {
            // Older case. This creates useless decision variables as certain Set and Unset
            // Assignments
            // do not have to be explored to find a minimal solution
            List<Label> allDataLabels = new ArrayList<>();
            allDataLabels.addAll(dataLabelsRemove);
            allDataLabels.addAll(dataLabelsAdd);
            for (Pin pin : allOutPins) {
                Map<Label, BoolExpr> setAssignments = new HashMap<>();
                Map<Label, BoolExpr> unsetAssignments = new HashMap<>();

                for (Label label : allDataLabels) {
                    setAssignments.put(label, context.mkBoolConst("Pin_" + pin.getId() + "_set_" + label.getEntityName()));
                    unsetAssignments.put(label, context.mkBoolConst("Pin_" + pin.getId() + "_unset_" + label.getEntityName()));
                }
                pinNewSetAssignments.put(pin, setAssignments);
                pinNewUnsetAssignments.put(pin, unsetAssignments);
            }
        }
    }

    /**
     * Initializes Node Label Modification variables. Also intializes Constants that represent node labels in the input DFD.
     */
    private void initializeNodes() {
        // Depending on configuration node labels may not be added
        Set<Label> nodeLabelsAdd = config.addNodeLabels() ? preprocesingResult.relevantNodeLabelsAdd() : new HashSet<>();
        // Depending on configuration node labels may not be removed
        Set<Label> nodeLabelsRemove = config.removeNodeLabels() ? preprocesingResult.relevantNodeLabelsRemove() : new HashSet<>();
        // Still, all relevant labels have to be encoded even if they are not modifiable
        Set<Label> allNodeLabels = new HashSet<>();
        allNodeLabels.addAll(preprocesingResult.relevantNodeLabelsAdd());
        allNodeLabels.addAll(preprocesingResult.relevantNodeLabelsRemove());
        // Analogously to pins, this only creates decision variables for modifications
        // that could repair constraint violation
        if (config.onlyRelevantModifications()) {
            for (Node node : preprocesingResult.dfd()
                    .dataFlowDiagram()
                    .getNodes()) {
                Set<Label> thisNodeLabels = new HashSet<>(node.getProperties());
                Map<Label, BoolExpr> thisNodeLabelInput = new HashMap<>();
                Map<Label, BoolExpr> thisNodeLabelVar = new HashMap<>();
                // For all relevant labels
                for (Label label : allNodeLabels) {
                    // If label can be added or removed. This means it appears in negated and
                    // non-negated selectors.
                    if (nodeLabelsAdd.contains(label) && nodeLabelsRemove.contains(label)) {
                        // The input encodes the presence of the label in the input DFD
                        thisNodeLabelInput.put(label, thisNodeLabels.contains(label) ? context.mkTrue() : context.mkFalse());
                        // Modifiable var
                        thisNodeLabelVar.put(label, context.mkBoolConst(node.getEntityName() + "_label_" + label.getEntityName()));
                    }
                    // If label can only be added, only create it for nodes that do not posses the
                    // label. This is the case if it only appears in a negated selector.
                    else if (nodeLabelsAdd.contains(label) && !thisNodeLabels.contains(label)) {
                        thisNodeLabelInput.put(label, thisNodeLabels.contains(label) ? context.mkTrue() : context.mkFalse());

                        thisNodeLabelVar.put(label, context.mkBoolConst(node.getEntityName() + "_label_" + label.getEntityName()));
                    }
                    // If label can only be removed, only create it for nodes that possess the
                    // label.
                    // This is the if it only appears in non-negated selector.
                    else if (nodeLabelsRemove.contains(label) && thisNodeLabels.contains(label)) {
                        thisNodeLabelInput.put(label, thisNodeLabels.contains(label) ? context.mkTrue() : context.mkFalse());
                        thisNodeLabelVar.put(label, context.mkBoolConst(node.getEntityName() + "_label_" + label.getEntityName()));
                    } else {
                        // If label can neither be added or removed, make it static. No reference is
                        // needed here because it is static.
                        thisNodeLabelVar.put(label, thisNodeLabels.contains(label) ? context.mkTrue() : context.mkFalse());
                    }
                }
                if (!allNodeLabels.isEmpty()) {
                    nodeLabelInput.put(node, thisNodeLabelInput);
                    nodeLabels.put(node, thisNodeLabelVar);
                }
            }
            // Older case. Creates modification variables for all relevant node labels. Some
            // can not satisfy constraints and are therefore useless
        } else {
            if (!allNodeLabels.isEmpty()) {
                for (Node node : preprocesingResult.dfd()
                        .dataFlowDiagram()
                        .getNodes()) {
                    Set<Label> thisNodeLabels = new HashSet<>(node.getProperties());

                    Map<Label, BoolExpr> thisNodeLabelInput = new HashMap<>();
                    Map<Label, BoolExpr> thisNodeLabelVar = new HashMap<>();

                    for (Label label : allNodeLabels) {
                        thisNodeLabelInput.put(label, thisNodeLabels.contains(label) ? context.mkTrue() : context.mkFalse());

                        thisNodeLabelVar.put(label, context.mkBoolConst(node.getEntityName() + "_label_" + label.getEntityName()));
                    }

                    nodeLabelInput.put(node, thisNodeLabelInput);
                    nodeLabels.put(node, thisNodeLabelVar);
                }
            }

        }
    }

    /**
     * Construct a list of repair operations for the input DFD
     * @param model contains the variable assignment of the solution that the Z3 Prover found
     * @return List of repair operations
     */
    private List<Operation> parseActions(Model model) {
        List<Operation> changes = new ArrayList<>();

        // For every modifiable node label
        for (Node node : nodeLabelInput.keySet()) {
            Map<Label, BoolExpr> beforeMap = nodeLabelInput.get(node);
            Map<Label, BoolExpr> afterMap = nodeLabels.get(node);

            for (Label label : beforeMap.keySet()) {
                BoolExpr beforeExpression = beforeMap.get(label);
                BoolExpr afterExpression = afterMap.get(label);
                // Parse to java
                boolean beforeValue = ((BoolExpr) model.evaluate(beforeExpression, true)).isTrue();
                boolean afterValue = ((BoolExpr) model.evaluate(afterExpression, true)).isTrue();

                // If it didn't exist in the input, but exists now, create a Add Operation
                if (!beforeValue && afterValue) {
                    changes.add(new NodeLabelAddOperation(node, label));
                    // On the other hand create a Remove Operation
                } else if (beforeValue && !afterValue) {
                    changes.add(new NodeLabelRemoveOperation(node, label));
                }
                // If it did not change create no operation
            }
        }
        // Evaluate Set Assignments
        for (Pin pin : pinNewSetAssignments.keySet()) {
            Map<Label, BoolExpr> newSetAssignments = pinNewSetAssignments.get(pin);

            for (Label label : newSetAssignments.keySet()) {
                BoolExpr setExpression = newSetAssignments != null ? newSetAssignments.get(label) : null;
                // If the pin could set the label and it actually did, create the operation
                if (setExpression != null && ((BoolExpr) model.evaluate(setExpression, true)).isTrue()) {
                    changes.add(new SetAssignmentOperation(pin, label));
                }
            }
        }
        // Evaluate Unset Assignments
        for (Pin pin : pinNewUnsetAssignments.keySet()) {
            Map<Label, BoolExpr> newUnsetAssignments = pinNewUnsetAssignments.get(pin);
            for (Label label : newUnsetAssignments.keySet()) {
                BoolExpr unsetExpression = newUnsetAssignments != null ? newUnsetAssignments.get(label) : null;

                // If the pin could unset the label and it actually did, create the operation
                if (unsetExpression != null && ((BoolExpr) model.evaluate(unsetExpression, true)).isTrue()) {
                    changes.add(new UnsetAssignmentOperation(pin, label));
                }
            }
        }

        return changes;
    }

    /**
     * Returns the context that all encoding is done
     * @return Context
     */
    public Context getContext() {
        return context;
    }

    /**
     * Returns a map of all DFDVertices to their respective incoming flows
     * @return Incoming Flows Map
     */
    public Map<DFDVertex, List<FlowInstance>> getVertexIncomingFlows() {
        return preprocesingResult.vertexIncomingFlows();
    }

    /**
     * Returns a Map of mappings of Flow instance Labels to their respective label expressions
     * @return Flow Label Map
     */
    public Map<FlowInstance, Map<Label, BoolExpr>> getFlowLabels() {
        return flowLabels;
    }

    /**
     * Returns the Datadictionary of the DFD
     * @return Datadictionary
     */
    public DataDictionary getDataDictionary() {
        return preprocesingResult.dfd()
                .dataDictionary();
    }

    /**
     * Returns a map of mappings of Node Labels to their respective Label Expressions
     * @return Node Label Map
     */
    public Map<Node, Map<Label, BoolExpr>> getNodeLabels() {
        return nodeLabels;
    }

}
