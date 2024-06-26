from abc import ABC, abstractmethod

class UncertaintyRanker(ABC):
    
    @abstractmethod
    def __init__(self,X, X_train, X_test, y, y_train, y_test):
        pass

    @abstractmethod
    def evaluate(self):
        pass

    @abstractmethod
    def show_ranking_with_correctness_score(self)-> list[(str,float)]:
        pass
