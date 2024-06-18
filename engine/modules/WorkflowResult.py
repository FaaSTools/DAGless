import os
import json
import logging

from modules.FunctionResult import FunctionResult

import pandas as pd

logging.basicConfig(level=logging.INFO, format='%(name)s - %(levelname)s - %(message)s')


class WorkflowResult:
    """The WorkflowResult class represents the result of a workflow invocation - it's intended to flatten the nested
    structure of the return values of the functions and to provide a convenient way to access the results of the
    workflow invocation"""

    def __init__(self, path_to_workflow_config: str) -> None:
        self.path_to_workflow_config = path_to_workflow_config
        self.monitor_results = []
        self.workflow_results = {
            "order": [],
            "provider": [],
            "region": [],
            "memory": [],
            "memory_unit": [],
            "code_exec_time_ns": [],
            "function_exec_time_ns": [],
            "total_exec_time_ns": [],
            "download_time_ns": [],
            "upload_time_ns": [],
            "invocation_latency_ns": [],
            "storage_name": [],
            "storage_provider": [],
            "storage_region": [],
            "download_bandwidth_mbs": [],
            "upload_bandwidth_mbs": [],
            "statement_range": [],
            "parallel": [],
            "parallel_factor": [],
        }
        self.file_transfers = {
            "order": [],
            "function_provider": [],
            "function_region": [],
            "function_memory": [],
            "function_memory_unit": [],
            "storage_region": [],
            "storage_provider": [],
            "transfer_type": [],
            "file_path": [],
            "file_size_mb": [],
            "transfer_time_ns": []
        }

    def __str__(self) -> str:
        return str(self.workflow_results)

    def __repr__(self) -> str:
        return str(self.workflow_results)

    def __get_path_to_transformed_functions(self):
        """Returns the path to the transformed functions of the workflow"""
        with open(self.path_to_workflow_config, "r") as config_file:
            split_config = json.load(config_file)

        key = "pathToSplitFunctions"
        if key not in split_config:
            raise RuntimeError(f"{key} not found in the workflow config file!")

        return split_config[key]

    def add_function(self, function: FunctionResult) -> None:
        """Adds a function to the workflow result - it adds the """
        logging.info(f"Adding function {function.get_name()} to workflow result")
        self.monitor_results.append(function.get_monitor_result())

        self.workflow_results["order"].append(function.get_function_order())
        self.workflow_results["provider"].append(function.get_provider())
        self.workflow_results["region"].append(function.get_region())
        self.workflow_results["memory"].append(function.get_memory())
        self.workflow_results["memory_unit"].append(function.get_memory_unit())
        self.workflow_results["code_exec_time_ns"].append(function.get_code_exec_time_ns())
        self.workflow_results["function_exec_time_ns"].append(function.get_function_exec_time_ns())
        self.workflow_results["total_exec_time_ns"].append(function.get_total_exec_time_ns())
        self.workflow_results["download_time_ns"].append(function.get_file_transfers().get_total_download_time_ns())
        self.workflow_results["upload_time_ns"].append(function.get_file_transfers().get_total_upload_time_ns())
        self.workflow_results["invocation_latency_ns"].append(function.get_invocation_latency_ns())
        self.workflow_results["storage_name"].append(function.get_storage())
        self.workflow_results["storage_provider"].append(function.get_storage_provider())
        self.workflow_results["storage_region"].append(function.get_storage_region())
        self.workflow_results["download_bandwidth_mbs"].append(
            function.get_file_transfers().get_download_bandwidth_mbs())
        self.workflow_results["upload_bandwidth_mbs"].append(function.get_file_transfers().get_upload_bandwidth())
        self.workflow_results["statement_range"].append(function.get_statement_range())
        self.workflow_results["parallel"].append(function.get_parallel())
        self.workflow_results["parallel_factor"].append(function.get_parallel_factor())

        number_of_files = function.get_file_transfers().get_number_of_files()
        logging.info(f"{number_of_files} files were transferred in function {function.get_name()}")
        self.file_transfers["order"].extend([function.get_function_order()] * number_of_files)
        self.file_transfers["function_provider"].extend([function.get_provider()] * number_of_files)
        self.file_transfers["function_region"].extend([function.get_region()] * number_of_files)
        self.file_transfers["function_memory"].extend([function.get_memory()] * number_of_files)
        self.file_transfers["function_memory_unit"].extend([function.get_memory_unit()] * number_of_files)
        self.file_transfers["storage_region"].extend([function.get_storage_region()] * number_of_files)

        self.file_transfers["storage_provider"].extend(function.get_file_transfers().file_transfers["storage_provider"])
        self.file_transfers["transfer_type"].extend(function.get_file_transfers().file_transfers["transfer_type"])
        self.file_transfers["file_path"].extend(function.get_file_transfers().file_transfers["file_path"])
        self.file_transfers["file_size_mb"].extend(function.get_file_transfers().file_transfers["file_size_mb"])
        self.file_transfers["transfer_time_ns"].extend(function.get_file_transfers().file_transfers["transfer_time_ns"])

        logging.info(f"Added function {function.get_name()} to workflow result")

    def workflow_to_dataframe(self) -> pd.DataFrame:
        df = pd.DataFrame(self.workflow_results)
        return df

    def write_workflow_to_csv(self, path: str) -> None:
        df = self.workflow_to_dataframe()
        df.to_csv(path, index=False)

    def file_transfer_to_dataframe(self) -> pd.DataFrame:
        df = pd.DataFrame(self.file_transfers)
        return df

    def file_transfer_to_csv(self, path: str) -> None:
        df = self.file_transfer_to_dataframe()
        df.to_csv(path, index=False)

    def accessed_files_as_dataframe(self) -> pd.DataFrame:
        """Returns a dataframe containing the order of the function and all accessed files"""
        df_dict = {
            "order": [],  # order of the function
            "accessed_files": []  # accessed files in the function
        }

        # adding all monitor results in the dict
        # adding the order of the function for each created file
        for order, result in enumerate(self.monitor_results):
            df_dict["order"].extend([order] * len(result.get_accessed_files()))
            df_dict["accessed_files"].extend(result.get_accessed_files())

        df = pd.DataFrame(df_dict)
        return df

    def created_files_as_dataframe(self) -> pd.DataFrame:
        """Returns a dataframe containing the order of the function and all created files"""
        df_dict = {
            "order": [],  # order of the function
            "created_file": []  # created files in the function
        }

        # adding all monitor results in the dict
        # adding the order of the function for each created file
        for order, result in enumerate(self.monitor_results):
            df_dict["order"].extend([order] * len(result.get_created_files()))
            df_dict["created_file"].extend(result.get_created_files())

        df = pd.DataFrame(df_dict)
        return df

    def get_workflow_provider(self) -> str:
        """Returns the provider of the workflow"""
        return next(iter(self.workflow_results.get("provider")), "None")

    def get_workflow_region(self) -> str:
        """Returns the region of the workflow"""
        return next(iter(self.workflow_results.get("region")), "None")

    def get_workflow_memory(self) -> str:
        """Returns the memory of the workflow"""
        return next(iter(self.workflow_results.get("memory")), "None")

    def get_workflow_storage_region(self) -> str:
        """Returns the storage region of the workflow"""
        return next(iter(self.workflow_results.get("storage_region")), "None")

    def update_file_io_in_function_config(self) -> None:
        """Updates the workflow config with the accessed files, created files and iteration items of each function"""
        logging.info("Updating workflow config with file I/O information")

        file_name = "workflow_config.json"
        path_to_transformed_functions = self.__get_path_to_transformed_functions()
        workflow_config_path = os.path.join(path_to_transformed_functions, file_name)

        # Use 'with' statement to ensure file closure
        with open(workflow_config_path, "r") as file:
            workflow_config = json.load(file)

        for order, monitor_result in enumerate(self.monitor_results):
            workflow_config[order]["downloadFilePaths"] = monitor_result.get_accessed_files()
            workflow_config[order]["uploadFilePaths"] = monitor_result.get_created_files()

            if workflow_config[order].get("parallel", False):
                workflow_config[order]["forEachIterations"] = monitor_result.get_for_each_iterations()

        # Use 'with' statement to ensure file closure and handle file writing
        with open(workflow_config_path, "w") as file:
            logging.info(f"Writing updated workflow config to {workflow_config_path}")
            json.dump(workflow_config, file, indent=4)

        logging.info("Updating workflow config with file I/O information finished")

    def set_workflow_results(self, workflow_results):
        """Sets the workflow results - this method is intended to be used in the benchmarking execution of the workflow
        invoker to set the aggregated workflow results"""
        self.workflow_results = workflow_results

    def set_file_transfers(self, file_transfers):
        """Sets the file transfers - this method is intended to be used in the benchmarking execution of the workflow
        invoker to set the aggregated file transfers"""
        self.file_transfers = file_transfers
