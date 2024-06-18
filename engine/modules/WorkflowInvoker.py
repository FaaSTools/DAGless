import copy
import os.path
from typing import Callable
import logging
import json

import numpy as np
import pandas as pd

from modules import FunctionNameUtils, FunctionInvoker
from modules.FileTransfers import FileTransfers
from modules.FunctionResult import FunctionResult
from modules.InvokerConfiguration import InvokerConfiguration
from modules.MonitorResult import MonitorResult
from modules.WorkflowConfig import WorkflowConfig
from modules.WorkflowResult import WorkflowResult

logging.basicConfig(level=logging.INFO, format='%(name)s - %(filename)s - %(funcName)s - %(levelname)s - %(message)s')


class WorkflowInvoker:
    def __init__(self, path_to_invocation_config: str, path_to_split_faasfifcation_config: str, path_to_results,
                 worker=10, block_size=1, warm_up=True) -> None:
        self.invoker_config = InvokerConfiguration(path_to_invocation_config)
        self.path_to_split_faasification_config = path_to_split_faasfifcation_config
        self.path_to_results = path_to_results
        self.__path_to_workflow_config = self.__get_workflow_config_path(path_to_split_faasfifcation_config)
        self.__workflow_config = WorkflowConfig(self.__path_to_workflow_config)
        self.__workers = worker
        self.__block_size = block_size
        self.__warm_up = warm_up

    @staticmethod
    def __get_workflow_config_path(path_to_split_faasfifcation_config: str) -> str:
        """
        Retrieves the path to the workflow configuration file based on the provided path to the split faasification
        configuration.

        Args:
            path_to_split_faasification_config (str): The path to the split faasification configuration file.

        Returns:
            str: The path to the workflow configuration file.

        Raises:
            FileNotFoundError: If any of the required files (split faasification config, transformed functions, or
                               workflow config) is not found.
        """
        if not os.path.exists(path_to_split_faasfifcation_config):
            raise FileNotFoundError(f"File not found: {path_to_split_faasfifcation_config}")
        path_to_transformed_functions = json.load(open(path_to_split_faasfifcation_config)).get("pathToSplitFunctions")

        if not os.path.exists(path_to_transformed_functions):
            raise FileNotFoundError(f"File not found: {path_to_transformed_functions}")
        path_to_workflow_config = os.path.join(path_to_transformed_functions, "workflow_config.json")

        if not os.path.exists(path_to_workflow_config):
            raise FileNotFoundError(f"File not found: {path_to_workflow_config}")

        return path_to_workflow_config

    def __prepare_payload_for_learning(self, result: dict, bucket: str, order: int) -> list:
        """
        Prepares the payload for the learning invocation of the workflow. It adds the function directory to the download
        and upload URI, which is equivalent to the transfer mode "COMPLETE".

        Args:
            result (dict): The result dictionary containing information to be included in the payload.
            bucket (str): The bucket for file transfers.
            order (str): The order of the learning function.

        Returns:
            dict: The prepared payload for the learning invocation.
        """
        # Deep copy the result to avoid modifying the original dictionary
        payload = copy.deepcopy(result)

        # Update payload with download and upload URIs
        function_dir_uri = f"{bucket}function/"
        payload.update({
            "downloadUris": [function_dir_uri],
            "monitorUtilsBucketUri": bucket,
            "uploadUris": [function_dir_uri],
            "enableDirectoryMonitoring": True,
        })

        # Remove unnecessary keys from the payload
        keys_to_remove = ["fileTransfers", "monitorResult", "executionTimeNs", "total_exec_time_ns"]
        for key in keys_to_remove:
            payload.pop(key, None)

        # save values of variables for benchmark invocation where the previous execution failed
        for key, value in payload.items():
            self.__workflow_config.set_input_values_by_order(order, key, value)

        # Write the workflow config to file
        self.__workflow_config.write_config(self.__path_to_workflow_config)


        # Convert all dicts to JSON strings for serialization
        payload = {key: json.dumps(value) for key, value in payload.items()}

        return [payload]

    def __prepare_payload_for_benchmarking(self, result: dict, bucket: str, order: int) -> list:
        """
        Prepares the payload for benchmarking by removing unnecessary inputs and adding URIs for file transfers.

        Args:
            result (dict): The result dictionary containing information to be included in the payload.
            bucket (str): The bucket for file transfers.
            order (int): The order of the split function.

        Returns:
            list: The prepared payload for benchmarking.
        """
        payload = copy.deepcopy(result)

        if True: #not payload and order != 0:
            # Backup in case a function fails and no result is returned, the execution may continue
            logging.info(f"Function {order - 1} failed, collecting payload from workflow config...")
            payload = self.__workflow_config.get_input_variables_by_order(order)

        # Update payload with download and upload URIs
        payload.update({
            "downloadUris": self.__workflow_config.get_uris_from_split_function_config(
                bucket=bucket, function_order=order, transfer_type="download"
            ),
            "uploadUris":  self.__workflow_config.get_uris_from_split_function_config(
                bucket=bucket, function_order=order, transfer_type="upload"
            ),
            "enableDirectoryMonitoring": False,

        })

        # Remove unnecessary keys from the payload
        keys_to_remove = ["fileTransfers", "monitorResult", "executionTimeNs", "total_exec_time_ns"]
        for key in keys_to_remove:
            payload.pop(key, None)

        # Convert all dicts to JSON strings for serialization
        payload = {key: json.dumps(value) for key, value in payload.items()}

        return [payload]

    def __prepare_payload_for_parallel_execution(self, result: dict, bucket: str, order: int) -> list:
        """
        Prepare payload for parallel execution based on workflow configuration.

        Args:
            result (dict): The original result dictionary.
            bucket (str): The storage bucket.
            order (int): The order of the function in the workflow.

        Returns:
            list: A list of dictionaries representing payloads for parallel execution.

        Note:
            This method extracts information from the provided result and the workflow configuration
            to build payloads for parallel execution based on the specified order.

        """
        payload = copy.deepcopy(result)
        if not self.__workflow_config.get_parallel_by_order(order):
            logging.info(f"Function {order} is not parallel, returning payload for benchmarking...")
            return self.__prepare_payload_for_benchmarking(result, bucket, order)

        if not payload and order != 0:
            # Backup in case a function fails and no result is returned, the execution may continue
            logging.info(f"Function {order-1} failed, collecting payload from workflow config...")
            payload = self.__workflow_config.get_input_variables_by_order(order)

        logging.info(f"Function {order} is parallel, preparing payload for parallel execution...")
        # Get the for each iteration from the workflow config
        for_each_iterations = self.__workflow_config.get_for_each_iterations_by_order(order)
        # Get the collection that is iterated over
        for_each_collection = self.__workflow_config.get_for_each_collection_by_order(order)

        # Since we do block size one each function just has one iteration
        # Build the payload for each iteration
        bucket = bucket.rstrip('/')
        # Split the for each iteration into chunks of size block_size
        for_each_chunks = [for_each_iterations[i:i + self.__block_size] for i in
                           range(0, len(for_each_iterations), self.__block_size)]
        temp_payload = [
            {
                "downloadUris": list({f"{bucket}/{file_path.lstrip('/')}"
                                      for for_each_it in for_each_chunk
                                      for file_path in for_each_it.get("downloadFilePaths", [])}),
                "uploadUris": list({f"{bucket}/{file_path.lstrip('/')}"
                                    for for_each_it in for_each_chunk
                                    for file_path in for_each_it.get("uploadFilePaths", [])}),
                for_each_collection: [for_each_it.get('iterationObject', {}) for for_each_it in for_each_chunk],
                "enableDirectoryMonitoring": False,
            }
            for for_each_chunk in for_each_chunks
        ]

        logging.info(f"Number of parallel executions for function {order}: {len(temp_payload)}")

        # Remove unnecessary keys from the original payload
        keys_to_remove = ["fileTransfers", "monitorResult", "executionTimeNs", "total_exec_time_ns",
                          for_each_collection]
        for key in keys_to_remove:
            payload.pop(key, None)

        # Merge all necessary fields in each new payload
        merged_payload_list = [{**p, **payload} for p in temp_payload if p]

        # Convert all dicts to JSON strings for serialization
        dumped_payload = [
            {
                key: json.dumps(value)
                for key, value in merged_payload.items()
            }
            for merged_payload in merged_payload_list
        ]
        return dumped_payload

    def invoke_workflow_for_learning(self) -> None:
        """
        Executes the workflow with the most resources allocated and writes the learned files
        to the function config files.

        This method identifies the workflow with the maximum allocated resources based on
        memory configuration and invokes the workflow. After the execution, it updates
        file input/output in the function configuration.

        """
        logging.info("Invoking workflow for learning...")
        workflows = self.invoker_config.get_workflows()

        maximum_configuration = max(
            workflows,
            key=lambda workflow: FunctionNameUtils.get_memory_from_function_name(workflow.get("functions")[0])
        )

        workflow_result = self.__invoke_workflow(maximum_configuration, self.__prepare_payload_for_learning)

        workflow_result.update_file_io_in_function_config()
        logging.info("Finished invoking workflow for learning!")

    def invoke_workflow_for_benchmarking(self, parallel: bool) -> list:
        """
        Executes all workflows in the invocation configuration for benchmarking.

        This method iterates through all workflows in the invocation configuration and
        executes each one using the __invoke_single_workflow method with the payload
        prepared for benchmarking. It returns a list of WorkflowResults from each workflow execution.

        Returns:
            list: A list of results from each workflow execution.

        """
        logging.info("Invoking workflow for benchmarking...")
        workflows = self.invoker_config.get_workflows()

        if parallel:
            prepare_payload_func = self.__prepare_payload_for_parallel_execution
        else:
            prepare_payload_func = self.__prepare_payload_for_benchmarking

        results = [
            self.__invoke_single_workflow(workflow, prepare_payload_func)
            for workflow in workflows
        ]

        logging.info("Finished invoking workflow for benchmarking!")
        return results

    def invoke_workflow_in_parallel(self) -> WorkflowResult:
        """
        Executes the first workflow in the invocation configuration in parallel.

        This method retrieves the first workflow from the invocation configuration and
        executes it in parallel using the __invoke_single_workflow method with the payload
        prepared for parallel execution. It returns the result of the parallel workflow execution.

        Returns:
            WorkflowResult: The result of the parallel workflow execution.

        Raises:
            Exception: If no workflow is found in the invocation configuration.

        """
        logging.info("Invoking workflow parallel...")
        workflow = next(iter(self.invoker_config.get_workflows()), None)
        if not workflow:
            raise Exception("No workflow found!")

        result = self.__invoke_single_workflow(workflow, self.__prepare_payload_for_parallel_execution)

        logging.info("Finished invoking workflow for benchmarking!")
        return result

    def __invoke_single_workflow(self, workflow: dict, prepare_payload: Callable) -> WorkflowResult:
        """
        Invokes a single workflow multiple times and returns the aggregated result.

        Args:
            workflow (dict): The workflow configuration to be invoked.
            prepare_payload (Callable): The function to prepare the payload for the workflow.

        Returns:
            WorkflowResult: The aggregated result of invoking the workflow multiple times.

        """
        invocation_count = workflow.get("invocation_count") if workflow.get("invocation_count") else 1

        logging.info(f"Executing workflow {workflow} {invocation_count} times")

        invocation_count_results = [self.__invoke_workflow(workflow, prepare_payload)
                                    for _ in range(invocation_count)]

        workflow_df = pd.concat([x.workflow_to_dataframe() for x in invocation_count_results], axis=0)
        file_transfer_df = pd.concat([x.file_transfer_to_dataframe() for x in invocation_count_results], axis=0)

        workflow_columns = ['order', 'provider', 'region', 'memory', 'memory_unit', 'storage_name',
                            'storage_provider', 'storage_region', 'statement_range', 'parallel', 'parallel_factor']
        file_transfer_columns = ['order', 'function_provider', 'function_region', 'function_memory',
                                 'function_memory_unit', 'storage_region', 'storage_provider', 'transfer_type',
                                 'file_path']

        workflow_df = workflow_df.groupby(workflow_columns, as_index=False).median()
        file_transfer_df = file_transfer_df.groupby(file_transfer_columns, as_index=False).median()

        result = WorkflowResult(self.path_to_split_faasification_config)
        result.set_workflow_results(workflow_df.to_dict(orient="list"))
        result.set_file_transfers(file_transfer_df.to_dict(orient="list"))

        self.__write_workflow_results_to_file([result])

        return result

    def __write_workflow_results_to_file(self, result_list) -> None:
        """
        Writes the workflow results to a file.

        Args:
            result_list: List of workflow results.

        Returns:
            None

        """
        for workflow_result in result_list:
            folder = os.path.join(self.path_to_results,
                                  (f"{workflow_result.get_workflow_provider()}-"
                                   f"{workflow_result.get_workflow_region()}-"
                                   f"{workflow_result.get_workflow_memory()}-"
                                   f"{workflow_result.get_workflow_storage_region()}"
                                   ))

            if not os.path.exists(folder):
                os.makedirs(folder)

            workflow_result.write_workflow_to_csv(os.path.join(folder, "workflow_result.csv"))
            workflow_result.file_transfer_to_csv(os.path.join(folder, "file_transfers.csv"))

    def __invoke_workflow(self, workflow: dict, prepare_payload: Callable) -> WorkflowResult:
        """
        Execute a workflow, invoking each function and collecting results.

        Args:
            workflow (dict): The workflow configuration.
            prepare_payload (Callable): A function to prepare the payload for each function.

        Returns:
            WorkflowResult: The result object containing function execution information.

        """
        functions = workflow.get("functions")
        bucket = workflow.get("bucket")

        workflow_result = WorkflowResult(self.path_to_split_faasification_config)
        result = {}

        for function in functions:
            order = FunctionNameUtils.get_order_from_function_name(function)
            # prepare payload for function
            payload = prepare_payload(result, bucket, order)
            # parallel factor which is used to determine the number of invocations
            parallel_factor = len(payload)
            # get provider from function name
            provider = FunctionNameUtils.get_provider_from_function_name(function)
            # warm up function
            if self.__warm_up:
                FunctionInvoker.warm_up_function(
                    function=function,
                    payload=payload,
                    provider=provider,
                    max_workers=self.__workers
                )
            try:
                result = FunctionInvoker.invoke_function(
                    function=function,
                    payload=payload,
                    provider=provider,
                    for_each_collection=self.__workflow_config.get_for_each_collection_by_order(order),
                    max_workers=self.__workers
                )
            except Exception as e:
                logging.error(e)
                logging.error(f"Function {function} failed, continuing with next function...")
                # setting result empty so that the prepare function reads the values from the workflow config
                result = {}
                continue

            workflow_result.add_function(
                FunctionResult(
                    name=function,
                    provider=provider,
                    region=FunctionNameUtils.get_region_from_function_name(function, provider),
                    memory=FunctionNameUtils.get_memory_from_function_name(function),
                    memory_unit=FunctionNameUtils.get_memory_unit_from_function_name(function),
                    code_exec_time_ns=result.get("codeExecutionTimeNs") if result.get(
                        "codeExecutionTimeNs") else np.NaN,
                    function_exec_time_ns=result.get("functionExecutionTimeNs") if result.get(
                        "functionExecutionTimeNs") else np.NaN,
                    total_exec_time_ns=result.get("total_exec_time_ns") if result.get("total_exec_time_ns") else np.NaN,
                    storage_name=bucket,
                    file_transfers=FileTransfers(result.get("fileTransfers")) if result.get(
                        "fileTransfers") else FileTransfers([]),
                    monitor_result=MonitorResult(result.get("monitorResult")) if result.get(
                        "monitorResult") else MonitorResult([]),
                    statement_range=self.__workflow_config.get_statement_range_by_order(order),
                    parallel=self.__workflow_config.get_parallel_by_order(order),
                    parallel_factor=parallel_factor
                )
            )

        return workflow_result
