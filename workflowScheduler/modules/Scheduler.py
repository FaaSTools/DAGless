import copy
import functools
import math
import os
import json
import logging
from itertools import groupby
from statistics import median

import numpy as np

from modules.SplitFunction import SplitFunction
from modules.FunctionDeployment import FunctionDeployment
from modules.File import File

import pandas as pd

logging.basicConfig(level=logging.INFO, format='%(name)s - %(levelname)s - %(message)s')


class Scheduler:
    def __init__(self, path_to_function_csv, path_to_file_transfer_csv, path_to_workflow_config, theta) -> None:
        # Data frame containing the functions
        self.functions_df = pd.read_csv(path_to_function_csv)
        # Data frame containing the file transfers
        self.file_transfers_df = pd.read_csv(path_to_file_transfer_csv)
        # Load the workflow config file
        self.workflow_config = json.load(open(path_to_workflow_config, "r"))
        # Load the split function config file
        self.split_function_config = json.load(
            open(
                os.path.join(
                    self.workflow_config.get("pathToSplitFunctions"),
                    "workflow_config.json")
                , "r"
            )
        )
        self.split_function_list = self.__create_split_function_list
        self.__maximum_exhaustive_search = theta

    @property
    def __create_split_function_list(self) -> list:
        split_function_list = []

        split_functions_grouped = self.functions_df.groupby("order")
        for order, group in split_functions_grouped:
            parallel = group["parallel"].iloc[0]
            split_function = SplitFunction(
                order=group["order"].iloc[0],
                statement_range=group["statement_range"].iloc[0],
                function_deployments=[
                    FunctionDeployment(
                        function_provider=row["provider"],
                        function_region=row["region"],
                        function_memory_mb=row["memory"],
                        storage_provider=row["storage_provider"],
                        storage_region=row["storage_region"],
                        code_exec_time_ns=row["code_exec_time_ns"],
                        invocation_latency_ns=row["invocation_latency_ns"],
                        download_files=self.__get_download_file_transfers(order, row["memory"], parallel),
                        upload_files=self.__get_upload_file_transfers(order, row["memory"], parallel),
                        download_bandwidth_mbs=row["download_bandwidth_mbs"] if not np.nan else 0,
                        upload_bandwidth_mbs=row["upload_bandwidth_mbs"] if not np.nan else 0
                    ) for _, row in group.iterrows()
                ],
                parallel=parallel,
                parallel_factor=group["parallel_factor"].iloc[0]
            )
            split_function_list.append(split_function)
        return split_function_list

    def __get_download_file_transfers(self, order: int, function_memory: int, parallel: bool, function_region: str, storage_region: str) -> set:
        """Returns the file transfers which are downloaded in the function as a set of File objects"""

        download_transfers = self.file_transfers_df[
            (self.file_transfers_df["order"] == order) &
            (self.file_transfers_df["transfer_type"] == "download") &
            (self.file_transfers_df["function_memory"] == function_memory) &
            (self.file_transfers_df["function_region"] == function_region) &
            (self.file_transfers_df["storage_region"] == storage_region)
            ]

        download_file_paths = download_transfers["file_path"].tolist()
        file_sizes_mb = download_transfers["file_size_mb"].tolist()
        transfer_time_ns = download_transfers["transfer_time_ns"].tolist()

        files_set = {File(path, size_mb, transfer_time_ns) for path, size_mb, transfer_time_ns in
                        zip(download_file_paths, file_sizes_mb, transfer_time_ns)}

        if parallel:
            files_set = self.__get_parallel_files(order, files_set, "download")
            return files_set
        return files_set

    def __get_upload_file_transfers(self, order, function_memory, parallel: bool, function_region: str, storage_region) -> set:
        """Returns the file transfers which are uploaded in the function as a set of File objects"""
        upload_transfers = self.file_transfers_df[
            (self.file_transfers_df["order"] == order) &
            (self.file_transfers_df["transfer_type"] == "upload") &
            (self.file_transfers_df["function_memory"] == function_memory) &
            (self.file_transfers_df["function_region"] == function_region) &
            (self.file_transfers_df["storage_region"] == storage_region)
            ]

        upload_file_path = upload_transfers["file_path"].tolist()
        file_sizes_mb = upload_transfers["file_size_mb"].tolist()
        transfer_time_ns = upload_transfers["transfer_time_ns"].tolist()

        files_set = {File(path, size_mb, transfer_time_ns) for path, size_mb, transfer_time_ns in
                     zip(upload_file_path, file_sizes_mb, transfer_time_ns)}

        if parallel:
            files_set = self.__get_parallel_files(order, files_set, "upload")
            return files_set
        return files_set

    def __get_parallel_files(self, order: int, files_set: set, transfer_type: str) -> set:
        """Returns the file transfers for parallel functions as a set of File objects"""
        function = next((function for function in self.split_function_config if function.get("order") == order), None)
        if function is None:
            raise RuntimeError(f"Function with order {order} not found in the workflow config file!")
        if not function.get("parallel"):
            raise RuntimeError(f"Function with order {order} is not parallel in the workflow config file!")

        for_each_iterations = function.get("forEachIterations")
        transfer_time_per_instance = []
        transfer_size_per_instance = []
        file_count = 0
        for iteration in for_each_iterations:
            iteration_paths = iteration.get("downloadFilePaths") if transfer_type == "download" else iteration.get(
                "uploadFilePaths")
            file_transfer_times = []
            file_transfer_sizes = []
            for file in iteration_paths:
                file_in_files_set = next((f for f in files_set if f.get_path() == file), None)
                if file_in_files_set is None:
                    raise RuntimeError(f"File {file} not found in the file transfers dataframe!")
                file_transfer_times.append(file_in_files_set.transfer_time_ns)
                file_transfer_sizes.append(file_in_files_set.size_mb)
                file_count += 1
            transfer_time_per_instance.append(sum(file_transfer_times))
            transfer_size_per_instance.append(sum(file_transfer_sizes))

        files_per_instance = int(file_count / len(for_each_iterations))
        median_transfer_time = median(transfer_time_per_instance)
        median_transfer_size = median(transfer_size_per_instance)

        return {File(
            path=f"parallel_{transfer_type}_{i}",
            size_mb=median_transfer_size / files_per_instance,
            transfer_time_ns=median_transfer_time / files_per_instance
        ) for i in range(0, files_per_instance)}

    def __create_new_config_dict(self) -> dict:
        """Returns a dictionary containing the new config for the SplitFaaSifier"""
        config = {"configSplitFunctions": []}

        for split_function in self.split_function_list:
            minimal_cost_deployment = split_function.get_minimal_cost_deployment()
            function_config = {
                "order": int(split_function.order),
                "statementRange": {
                    "start": int(split_function.statement_range[0]),
                    "end": int(split_function.statement_range[1])
                },
                "provider": minimal_cost_deployment.function_provider,
                "region": minimal_cost_deployment.function_region,
                "memory": int(minimal_cost_deployment.function_memory_mb),
                "parallel": bool(split_function.parallel),
                "downloadFilePaths": [file.path for file in minimal_cost_deployment.get_download_files()] if not split_function.parallel else [],
                "uploadFilePaths": [file.path for file in minimal_cost_deployment.get_upload_files()] if not split_function.parallel else [],
                "iterationDependencies": self.__resolve_iteration_dependencies(
                    split_function.order) if split_function.parallel else [],
                "monitorDirectory": False,
                "measureExecutionTime": True,
            }
            config["configSplitFunctions"].append(function_config)

        # reset the order from 0 to n
        config_split_functions = config["configSplitFunctions"]
        for i, function_config in enumerate(config_split_functions):
            function_config["order"] = i
        config["configSplitFunctions"] = config_split_functions

        return config

    def update_split_function_config(self, output_path: str) -> None:
        # Load the existing configuration
        config = copy.deepcopy(self.workflow_config)

        # Update the config with new values
        new_config = self.__create_new_config_dict()
        config["configSplitFunctions"] = new_config["configSplitFunctions"]

        # Update the path to the split functions
        config["pathToSplitFunctions"] = config["pathToSplitFunctions"].replace("transformed", "optimized")

        # Set the data transfer mode to NONE
        config["dataTransferMode"] = "MANUAL"

        # Set the traceFileTransfer to False
        config["traceFileTransfer"] = True

        # Remove unnecessary keys
        keys_to_remove = ["functionDeployment", "splitRules", "directoryMonitoring", "measureExecutionTime"]
        for key in keys_to_remove:
            config.pop(key, None)

        # Save the updated configuration
        with open(output_path, "w") as file:
            json.dump(config, file, indent=4)

    def print_minimal_cost_info(self):
        overall_cost = sum([function.get_minimal_cost() for function in self.split_function_list])
        print(f"Overall cost: {overall_cost}")
        runtime_s = sum([function.get_minimal_cost_deployment().get_runtime_s() for function in self.split_function_list])
        print(f"Overall runtime: {runtime_s}")

    def print_minimal_memory_info(self):
        overall_cost = sum([function.get_minimal_memory_cost() for function in self.split_function_list])
        print(f"Overall cost: {overall_cost}")
        runtime_s = sum([function.get_minimal_memory_deployment().get_runtime_s() for function in self.split_function_list])
        print(f"Overall runtime: {runtime_s}")

    def print_minimal_runtime_info(self):
        overall_cost = sum([function.get_minimal_runtime_deployment().get_cost() * function.parallel_factor for function in self.split_function_list])
        print(f"Overall cost: {overall_cost}")
        runtime_s = sum(
            [function.get_minimal_runtime_deployment().get_runtime_s() for function in self.split_function_list])
        print(f"Overall runtime: {runtime_s}")


    def __resolve_iteration_dependencies(self, order: int) -> dict:
        """Returns the iteration dependencies of a split function from the workflow config file"""
        # get the path to the transformed functions
        workflow_config = copy.deepcopy(self.split_function_config)

        # Find the split function with the given order or raise an error if not found
        split_function = next((sf for sf in workflow_config if sf.get("order") == order), None)
        if split_function is None:
            raise RuntimeError(f"Split function with order {order} not found in the workflow config file!")

        # Get the iteration dependencies from the split function or raise an error if not found
        iteration_dependencies = split_function.get("forEachIterations")
        if iteration_dependencies is None:
            raise RuntimeError(f"forEachIterations not contained in the split function with order {order}!")

        # Return the iteration dependencies
        return iteration_dependencies

    def optimize_cost_performance(self):
        grouped_functions = [list(group) for _, group in groupby(self.split_function_list, key=lambda x: x.parallel)]

        merged_functions = []
        for index, group in enumerate(grouped_functions):
            # if all are parallel then skip
            if all([function.parallel for function in group]):
                merged_functions.append([group])
                continue
            # Perform a greedy optimization for groups of more than 15 functions until the group's size is equal than 15
            if len(group) > self.__maximum_exhaustive_search:
                group = self.__optimize_dynamic_programming(group)

            # Perform an exhaustive search for groups of less than or equal to 15 functions
            if len(group) <= self.__maximum_exhaustive_search:
                merged_functions.append(self.__optimize_exhaustive_search(group))

        most_cost_effective = []
        for group in merged_functions:
            min_price = math.inf
            min_index = None
            for partition_index, partition in enumerate(group):
                price = sum([function.get_minimal_cost() for function in partition])
                if price < min_price:
                    min_price = price
                    min_index = partition_index
            most_cost_effective.append(group[min_index])

        overall_cost = sum([function.get_minimal_cost() for functions in most_cost_effective for function in functions])
        logging.info(f"Overall cost: {overall_cost}")
        runtime_s = sum([sum([function.get_minimal_cost_deployment().get_runtime_s() for function in functions]) for functions in most_cost_effective])
        logging.info(f"Overall runtime: {runtime_s}")

        self.split_function_list = [function for functions in most_cost_effective for function in functions]

    def __optimize_dynamic_programming(self, group):
        group = copy.deepcopy(group)
        while len(group) > self.__maximum_exhaustive_search:
            # Generate all possible pairs
            pairs = list(self.__pair_generator(group))
            # Compute the cost before merging
            best_cost_before_merge = [sum([function.get_minimal_cost() for function in pair]) for pair in pairs]
            # Merge all pairs
            merged_function_pairs = [functools.reduce(lambda x, y: x.merge(y), pair) for pair in pairs]
            # Compute the cost after merging
            best_cost_after_merge = [function.get_minimal_cost() for function in merged_function_pairs]
            # Compute the difference between the costs
            best_cost_benefit = [before - after for before, after in zip(best_cost_before_merge, best_cost_after_merge)]
            # Get the index of the best cost benefit
            best_index = best_cost_benefit.index(max(best_cost_benefit))
            # Create a pair by index to be merged
            group = self.__create_pair_by_index(group, best_index)
            # Merge the pair
            group[best_index] = functools.reduce(lambda x, y: x.merge(y), group[best_index])

        # reset group order ascending
        starting_order = group[0].order
        for index, function in enumerate(group):
            function.order = starting_order + index
        return group

    def __optimize_exhaustive_search(self, group):
        partitions = self.__partition_generator(group)
        reduced_partitions = []
        for partition in partitions:
            reduced_functions = [functools.reduce(lambda x, y: x.merge(y), sublist) for sublist in partition]
            reduced_partitions.append(reduced_functions)
        return reduced_partitions

    def __pair_generator(self, collection):
        for i in range(len(collection) - 1):
            yield collection[i], collection[i + 1]

    def __create_pair_by_index(self, collection, index):
        collection[index] = [collection[index], collection[index + 1]]
        del collection[index + 1]
        return collection

    def __partitions(self, collection) -> list:
        if len(collection) == 1:
            yield [collection]
            return
        first = collection[0]
        for smaller in self.__partitions(collection[1:]):
            for n, subset in enumerate(smaller):
                yield [[first] + subset] + smaller[n + 1:]
            yield [[first]] + smaller

    def __is_ascending(self, sublist):
        return all(sublist[i].order == sublist[i + 1].order - 1 for i in range(len(sublist) - 1))

    def __partition_generator(self, collection):
        unsorted_partitions = list(self.__partitions(collection))
        return [lst for lst in unsorted_partitions if all(self.__is_ascending(sub) for sub in lst)]
