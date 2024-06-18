import copy
import functools
import logging

from modules.File import File
from modules.FunctionDeployment import FunctionDeployment

logging.basicConfig(level=logging.INFO, format='%(name)s - %(levelname)s - %(message)s')


class SplitFunction:
    def __init__(
            self,
            order: int,
            statement_range: str,
            function_deployments: list,
            parallel: bool,
            parallel_factor: int
    ):
        self.order = order
        self.statement_range = self.__get_tuple_from_statement_range(statement_range)
        self.function_deployments = function_deployments
        self.parallel = parallel
        self.parallel_factor = parallel_factor

    @staticmethod
    def __get_tuple_from_statement_range(statement_range) -> tuple:
        """Returns a tuple from the statement range"""
        return tuple(map(int, statement_range.split("-")))

    def merge(self, split_function):
        """Reduces the current split function by the given split function"""
        if self.parallel or split_function.parallel:
            raise Exception("Cannot reduce parallel functions")

        # Create a deep copy of the current split function
        deepcopy = copy.deepcopy(self)

        # Merge the statement ranges
        deepcopy.statement_range = (deepcopy.statement_range[0], split_function.statement_range[1])

        # Find the intersecting memories
        deepcopy_memories = {deployment.function_memory_mb for deployment in deepcopy.function_deployments}
        split_function_memories = {deployment.function_memory_mb for deployment in split_function.function_deployments}
        intersecting_memories = list(deepcopy_memories.intersection(split_function_memories))

        # Create deployments that are mergeable with the other split function
        deepcopy.function_deployments = [deployment for deployment in deepcopy.function_deployments if deployment.function_memory_mb in intersecting_memories]

        # Merge the deployments
        for index, deployment in enumerate(deepcopy.function_deployments):
            other = next((d for d in split_function.function_deployments if d.mergeable(deployment)), None)
            if other:
                deployment.merge(other)
            else:
                logging.warning(f"Could not find mergeable deployment for {deployment} in {split_function}")

        return deepcopy

    def get_minimal_cost(self) -> float:
        # print([deployment.get_cost() for deployment in self.function_deployments])
        return min([deployment.get_cost() for deployment in self.function_deployments]) * self.parallel_factor

    def get_minimal_cost_deployment(self) -> FunctionDeployment:
        return min(self.function_deployments, key=lambda deployment: deployment.get_cost())

    def get_minimal_memory_cost(self) -> float:
        min_memory_deployment = self.get_minimal_memory_deployment()
        return min_memory_deployment.get_cost() * self.parallel_factor

    def get_minimal_memory_deployment(self) -> FunctionDeployment:
        return min(self.function_deployments, key=lambda deployment: deployment.function_memory_mb)

    def get_minimal_runtime_cost(self) -> float:
        return min([deployment.get_runtime_cost() for deployment in self.function_deployments]) * self.parallel_factor

    def get_minimal_runtime_deployment(self) -> FunctionDeployment:
        return min(self.function_deployments, key=lambda deployment: deployment.get_runtime_s())

    def __str__(self):
        return (f"order={self.order}, best_memory={self.get_minimal_cost_deployment().function_memory_mb}, "
                f"download_files={len(self.get_minimal_cost_deployment().download_files)}, "
                f"upload_files={len(self.get_minimal_cost_deployment().upload_files)}, minimal_cost={self.get_minimal_cost()}, runtime_s={self.get_minimal_cost_deployment().get_runtime_s()}")

    def __repr__(self):
        return self.__str__()