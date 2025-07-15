# Project Documentation

# MitigationModelCalculator

## Overview

_Applies all combinations of provided uncertainties and DataFlowDiagrams and find solutions that do not violate the defined
constraints. It does so by iterating over all combinations of uncertainties, building the corresponding DFD, and analyzing for
constraint violations. If no violations are found the solution is then returned/stored._

## Methods

### `findMitigatingModel()`

Finds and returns a list of mitigating models based on the given uncertainty sources,
data flow diagrams, and constraints. The method cleans the output path, generates
mitigation candidates by considering uncertainty scenarios, and finally stores and processes
them while filtering irrelevant uncertainties.

    @return A list of mitigating models that meet the constraints and have been successfully processed.

### `createMitigationCandidates()`
Generates mitigation candidates by exploring combinations of uncertainty scenarios
and determining if they lead to valid mitigation models.

    @param index The current index of the uncertainty source being considered in the recursive process.
    @param relevantUncertainties A list of uncertainty sources relevant to the mitigation computation.
    @param diagramAndDict The current data flow diagram and its corresponding dictionary being analyzed.
    @param candidates The list where discovered mitigation models are stored as candidates.
    @param chosenScenarios A list of scenario identifiers representing the current combination of uncertainties being processed.


### `storeMitigationCandidates()`
Stores and processes mitigation candidates by filtering uncertainty sources and saving the relevant mitigation models.

    @param candidates A list of mitigation models to be stored after filtering relevant uncertainties.
    @param uncertaintiesToKeep A list of uncertainty sources that should be retained during filtering.
    @param mitigationURIs URIs representing file paths or memory locations used for storing 
    uncertainty models and their mitigated versions.
    @return A list of mitigation models that have been successfully stored and processed.

---

# UncertaintyRanker

## Overview

_Ranks uncertainties based on their importance for mitigating the violating DFD. To do so, the ranker has 
multiple strategies that can be executed in the python script. It returns ranked uncertainty list, with the goal 
of optimizing repair time._

## Methods

### `rankUncertaintiesBasedOnTrainData()`
Executes the Python script for ranking uncertainties based on training data and returns the ranked results.
This method interacts with the script by invoking it with the given parameters
and processes the output to return a list of ranked uncertainties.

     @param pythonPath Path to the Python interpreter to execute the script.
     @param scriptPath Path to the Python script that performs the ranking.
     @param pathToTrainDataFolder Path to the folder containing the training data used for ranking.
     @param rankingLength The desired number of top-ranked uncertainties to output.
     @param rankerType The type of ranking method or model to be used for ranking (e.g., PCA, LDA).
     @param aggregationMethod The method used to aggregate rankings (e.g., SUM, EXPONENTIAL_RANKS).
     @param mitigationStrategy The strategy for mitigating uncertainty during the ranking process (e.g., CLUSTER).
     @return A list of ranked uncertainties as strings, or null if an error occurs during execution.
---

# MitigationTestBase

## Overview

_General Test, that provides important interfaces for later test._

## Methods

### `storeMeassurementResults(List<Float> meassurements, String rankerType, String aggregationMethod)`

Stores the measurement results by appending them to a file named "meassurement_results.txt".
Depending on the ranker type, the method writes runtime results or detailed increasing, quarter,
and half-measurements to the file.

      @param meassurements a list of float values representing measurement results
      @param rankerType a string indicating the type of ranker (e.g., "BRUTE FORCE")
      @param aggregationMethod a string indicating the aggregation method applied

### `storeMeassurementResult(float meassurement, String tag)`

Stores a measurement result by appending it to a file named "meassurement_results.txt".
The result is written in the format: "{tag}: {meassurement}".
If the file does not exist, it is created. If it exists, the new measurement is appended.

      @param meassurement the measurement value to be stored
      @param tag a string label associated with the measurement

### `storeTrainingDataResults(List<Float> meassurements, String rankerType, String aggregationMethod)`

Stores the training data results by appending them to a file named "meassurement_results.txt".
The results include the average duration for increasing, quarter, and half training phases
based on measured values while using the specified ranker type and aggregation method.

      @param meassurements a list of float values representing the measurement results
      @param rankerType a string indicating the ranker type used (e.g., "BRUTE FORCE")
      @param aggregationMethod a string describing the aggregation method applied

### `loadSolutionRanking()`

Loads a solution ranking from a file specified by the path stored in the class field
'pathToRankingSolution'. If the file does not exist or an error occurs during file reading,
an empty list is returned. Otherwise, the method returns the lines of the file as a list
of strings.

      @return a list of strings representing the lines in the solution ranking file, or an empty list
              if the file does not exist or an error occurs.

### `seeAverageRuntime()`

Computes and returns the average runtime measurement based on values stored in a file.
The method ignores initial "warmup" runs and calculates the average only on the relevant data.
Logs the computed average runtime for reference.

      If the measurements file does not exist or an error occurs during file reading,
      a default value of 0.0f is returned.
     
      @return the average runtime as a float, or 0.0f if the file is invalid or an error occurs

### `printMetricies()`

Logs precision and mean average precision metrics for given rankings.
This method computes precision at K (P@K) and mean average precision at K (MAP@K)
for a set of solution rankings and program rankings. The values are logged using
the class logger. Additionally, the method calculates the metrics based on R,
which is determined by the rank of the last relevant element in the program ranking.

      The following calculations and logs are performed:
      - P@K: Precision of the top K elements in the program ranking compared to the solution ranking.
      - MAP@K: Mean average precision of the top K elements in the program ranking.
      - P@R: Precision of the top R elements, where R is the rank of the last relevant element.
      - MAP@R: Mean average precision of the top R elements.
     
      Preconditions:
      - The solution ranking is loaded via the `loadSolutionRanking` method.
      - The program ranking is derived from the class field `rankedUncertaintyEntityNames`.

### `createUncertaintyRanking()`
Generates a ranking of uncertainties within a model, based on violations of constraints applied
to uncertain data flows. This method performs the following steps:
 
1. Retrieves the analysis object and the list of constraints to be applied.
2. Generates uncertain flow graphs from the existing flow graph contained in the analysis.
3. Evaluates the generated uncertain flow graphs to determine their validity.
4. Converts the uncertain flow graphs into a collection of uncertain transpose flow graphs.
5. For each constraint:
   - Identifies violations by querying uncertain data flow using the constraint.
   - If constraint violations are found, generates training data and writes it to a CSV file
     specific to the constraint.
6. Uses the generated training data to rank uncertainties in the model. The ranking is computed
   based on a specified ranker type, aggregation method, and mitigation strategy by the python script.

### `mitigateWithIncreasingAmountOfUncertainties()`
Executes a mitigation process on a fixed number of uncertainties ranked by their entity names,
and returns the corresponding mitigation models. The method identifies "n" top-ranked uncertainties,
calculates the mitigations based on the provided analysis and data flow diagram, and returns any
mitigation models that have been computed.     
     
      @param rankedUncertaintyEntityName a list of entity names ranked by their significance or priority.
      @param analysis an object that provides information about uncertainty sources and supports analysis of confidentiality.
      @param dfdAnddd a data structure that combines a data flow diagram and its associated dictionary, used for mitigation computation.
      @return a list of MitigationModel objects representing the mitigation results for the chosen uncertainties.

### `mitigateWithFixAmountOfUncertaintie()`
Executes a mitigation process on a fixed number of top-ranked uncertainties and returns 
the corresponding mitigation models. The method selects the highest-ranked uncertainties
based on the provided list, calculates mitigations using the specified analysis and 
data flow diagrams, and returns generated mitigation models.

    @param rankedUncertaintyEntityName a list of entity names ranked by their significance or priority.
    @param n the maximum number of top-ranked uncertainties to consider for mitigation.
    @param analysis an object that provides information about uncertainty sources and supports analysis of confidentiality.
    @param dfdAnddd a data structure that combines a data flow diagram and its associated dictionary, used for mitigation computation.
    @return a list of MitigationModel objects representing the mitigation results for the chosen uncertainties.

### `createMitigationCandidatesAutomatically()`

Automatically generates mitigation candidates for handling uncertainties within a given analysis based on a selected mitigation strategy.
This method determines the appropriate approach for mitigating uncertainties by evaluating entity names, uncertainty data,
and the chosen strategy, and then applies the corresponding mitigation logic to generate the most suitable mitigation candidates.

      The mitigation strategies supported include:
      - INCREASING: Gradually mitigates an increasing amount of uncertainty sources.
      - QUATER: Mitigates progressively with fixed fractions (quarters) of uncertainty sources.
      - HALF: Starts with half the uncertainty sources, and expands if necessary.
      - CLUSTER: Applies mitigation based on clustering analysis of uncertainty data.
      - FAST_START: Optimizes mitigation by quickly identifying the smallest number of uncertainty sources to resolve the issue.
      - BRUTE_FORCE (default): Mitigates all uncertainty sources in one attempt.
     
      The method ensures there is at least one valid mitigation result generated, as indicated by the final assertion.

---

# Uncertainty_Ranking.py

## Overview

_Python script that does get called by the java UncertaintyRanker class. The communication between python and java is done via the 
console stdout. It performs the ranking using the provided strategy. Possible options are:_

## Ranking Algorithms (`RANKER_TYPE`):

- **`LDA`**: Linear Discriminant Analysis Ranker
- **`P`**: Principal Component Uncertainty Ranker
- **`IP`**: Inverse Principal Component Uncertainty Ranker
- **`F`**: FAMD Uncertainty Ranker
- **`IF`**: Inverse FAMD Uncertainty Ranker
- **`RF`**: Random Forest Uncertainty Ranker
- **`LR`**: Linear Regression Uncertainty Ranker
- **`LGR`**: Logistic Regression Uncertainty Ranker
- **Default**: Principal Component Uncertainty Ranker (`P`)

## Aggregation Methods (`AGGREGATION_TYPE`):

- **`L`**: Summing rankings linearly
- **`E`**: Summing rankings exponentially
- **Default**: Taking top 3 rankings

## Batch Size Optimization:

- **`BATCH_SIZE_OPTIMIZATION`**: Enable (`Y`) or disable batch size optimization clustering.

## Output Settings:

- **`RELEVANT_UNCERTAINTIES_LENGTH`**: Number of uncertainties to output/display


---

# Batch_size_identifier.py

## Overview

_Generates uncertainty clusters based on violations and optimal combinations. To do so it uses a 
k means classifier with k being either eight or the number of available uncertainties._


---
# Design Decisions: Replacing Recursive Approach with Iterative Evaluation

Context:
Previously, the process of identifying viable uncertainty configurations was implemented using recursion. 
This approach stored each evaluated configuration in memory, causing RAM overflows and significant performance d
egradation when handling large datasets or numerous configurations.

Decision:
Replaced the recursive approach with an iterative method. Each uncertainty configuration is now evaluated immediately
upon generation, with its viability determined on-the-fly. Non-viable configurations are discarded immediately,
preventing unnecessary memory usage.

Rationale:

Performance Improvement: Immediate evaluation reduces computational overhead and enhances runtime efficiency.

Memory Efficiency: By discarding configurations immediately after evaluation rather than storing them, 
the approach significantly reduces RAM usage, eliminating previous memory overflow issues.

Impact:
This iterative method enables the application to handle larger datasets efficiently and improves overall system stability 
and responsiveness.

# Recent Changes

- Logging Improvements: Replaced generic System.out.println statements with meaningful logger messages to enhance debugging and monitoring.

- Test Enhancements: Implemented proper test evaluation methods, enabling tests to genuinely fail and report accurately, replacing previous behavior of merely printing failed test messages.

- Bug Fixes: Addressed issues in Clustering, MitigationModelCalculator, and Ranker modules, enhancing the overall reliability and accuracy of the application.

- Performance Improvements: Removed excessive print statements and optimized the analysis process by implementing a streamlined, minimally sufficient analysis rather than the previously complex, overly engineered version.

- SOLID Principles: Eliminated duplicate code by extracting common functionalities, simplified naming conventions, and restructured complex conditional statements and loops for better readability and maintainability.

