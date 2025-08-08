# Project Documentation

# Mechanic

## Overview

_The Mechanic class is designed to operate on a Data Flow Diagram (DFD) and its associated dictionary, 
applying constraints and repairing violations found within the DFD. It provides the ability to assess
whether a DFD and its constraints are consistent and suggests corrections to resolve violations._

## Methods

### Constructor

_The Mechanic class has four different constructors allowing multiple options in providing the DFD._
    
    Options:
    - DFD Format:
        - Location of the web json DFD or
        - DataFlowDiagramAndDictionary object from xDecaf
    - LabelCostMap:
        - no Costs
        - CostMap {Label : Value}

### `repair()`

Repairs the given Data Flow Diagram and Dictionary (DFD) by analyzing violations
against constraints, identifying necessary corrective actions, and applying those
corrections to provide an updated and valid DFD.

The method first determines the violating Transpose Flow Graphs (TFGs) in the DFD
and analyzes the nodes and flows to be adjusted. If no violations are found, the original DFD is returned.
Otherwise, it solves for feasible corrective actions using the SAT class, chooses the best solution, and applies
the appropriate changes to the DFD.

    @return the updated Data Flow Diagram and Dictionary (DFD) that adheres to the specified constraints
    @throws ContradictionException if the constraint solving process encounters a contradiction
    @throws TimeoutException if the constraint solving process exceeds the predefined time limit
    @throws IOException if there is an error reading or writing the DFD data during the repair
---

# SAT

## Overview
_The Sat class provides a framework for solving satisfiability problems.
It models and solves a set of constraints related to nodes, flows, and terms
in a data flow diagram (DFD)._

## Methods

### `solve()`
The method builds the necessary clauses, writes related output files for debugging or analysis
if a DFD name is provided, and then solves the problem using a SAT solver.

    @param nodes the list of nodes involved in the system.
    @param flows the list of flows between nodes in the system.
    @param constraints the list of constraints that must be satisfied.
    @param dfdName the optional name for the data flow diagram; used for output file naming.

    @return a list of solutions, where each solution is represented as a list of terms.

### `buildClauses()`
In this methode, the minimal critical DFD resulting from Mechanic is transformed into semantically equivalent CNF clauses.
By enforcing existing nodes, flows, and labels, we are able to preserve existing functionality while 
preventing degenerated solutions. Next, we enforce the confidential constraints and allow only those changes which may 
potentially satisfy all the given constraints. As a result, we prohibit ineffective changes and further shrink the 
solution space.

The building process can be split into three distinct steps:
1. Transformation of the extracted DFD subgraph into a formal representation comprising predicates, clauses, and terms
2. Encoding of DFD semantics within the context of propositional logic
3. Enforcement of predefined confidentiality constraints on all nodes and data flows


### `solveClauses`
Solves a system of logical clauses using a SAT solver and returns a list of unique solutions.

The method iteratively finds solutions to the given SAT problem, extracts relevant terms
from each solution while excluding terms associated with incoming data, and ensures each
solution is unique. It also prohibits previously found solutions to ensure the discovery of
distinct results. The solver halts if more than 10,000 solutions are found, throwing a
{@link TimeoutException}. Additional constraints are dynamically added to avoid revisiting
the same solutions.

---
# ModelCostCalculator

## Overview
_ModelCostCalculator is responsible for computing the total cost of a data flow diagram (DFD)
based on predefined constraints and label costs. The calculator traverses the DFD, identifies
relevant labels, and evaluates their occurrences in various nodes and connections within the diagram._

## Methods

### `calculateCost()`
Calculates the cost of evaluating a data flow model by computing the combined
cost of all relevant labels and their associated operations within the model.
It identifies relevant labels based on constraints, evaluates vertices, and
processes outgoing pins linked to those labels. The cost for a label is determined
by multiplying the number of related operations by the predefined cost of the label.
    
    @return the total calculated cost of the model evaluation as an integer

---
# Label

## Overview
_Represents a composite label that combines a LabelCategory with a Label.
This is an abstract base class, intended to be extended by specific implementations
of composite labels with predefined categories.
The primary purpose of this class is to encapsulate the association between a category
and a label, as well as to provide standard implementations for equality, hashing, and
string representation of composite labels.
The class ensures immutability by using final fields for its components: category
and label. Subclasses provide specific categorizations by invoking the constructor
with a predefined category and a Label.
Equality and hash code calculations are based on the category, label type, and label value.
The string representation summarizes the composite label with its category, type, and value._

## Types

- NodeLabel
- IncomingDataLabel
- OutgoingDataLabel

---
# Vertex

## Overview
_The Vertex class represents an entity in a data flow analysis graph.
It encapsulates properties, behaviors, and relationships with other entities,
such as incoming and outgoing pins and assignments._

## Methods

### `getOutPinsWithLabel()`
Retrieves a list of outgoing pins associated with a specific label.

This method iterates through the set of outgoing labels and selects pins
that contain the given label in their corresponding list of labels.

    @param label the label used to filter the outgoing pins; must not be null
    @return a list of pins associated with the specified label; 
    returns an empty list if no pins match the given label

### `hasVertexLabel()`
Checks if the specified label exists in the collection of vertex labels.
    
    @param label the label to check for existence; must not be null
    @return Boolean

### `getForwardingOutPins()`
Retrieves a list of output pins that are forward-connected to the specified input pin.

This method iterates through the forwarding assignments and identifies output pins
connected to the provided input pin through forwarding logic.

    @param pin the input pin for which forwarding output pins are to be retrieved; must not be null
    @return a list of output pins that are forward-connected to the specified input pin;
    returns an empty list if no forward connections exist for the input pin

---
# Records
_Records are used to handle simple data structures of our approach._

## Label
Represents a label with a type and value. 
The class provides a customized implementation of the toString method
to display the label in a formatted string representation.

## Node
Represents a Node entity that models a connection point with a set of input pins,
output pins, and associated labels containing characteristics or properties.

## InPin
Represents an input pin with a unique identifier.
This record is designed to hold the unique ID of the input pin,
which can be used to represent and manage the state or connection
of the pin.

## OutPin
Represents an output pin with a unique identifier.
This record is designed to hold the unique ID of the output pin,
which can be used to represent and manage the state or connection
of the pin.

## Flow
Represents a unidirectional flow from a source OutPin to a sink InPin.
A Flow object establishes a connection between the output pin and input pin,
providing a way to model system design or data flow connections.


## FlowDataLabel
The FlowDataLabel record encapsulates a Flow and its associated IncomingDataLabel.
It is used to represent the pairing of a unidirectional data Flow with a specific label
that provides additional context or metadata for the incoming data in the flow.


## Literal
Represents a literal that consists of a polarity (positive or negative) and a composite label.

## Term
Represents a term consisting of a specific domain and an associated composite label.

