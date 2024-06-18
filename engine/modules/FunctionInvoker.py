import concurrent.futures
import subprocess
from time import perf_counter_ns
import re
import json
import logging
import requests

import numpy as np
import boto3
from botocore.config import Config

READ_TIMEOUT = 900  # 15 minutes

BOTO3_CONFIG = Config(
    connect_timeout=30,
    read_timeout=READ_TIMEOUT
)

logging.basicConfig(level=logging.INFO, format='%(name)s - %(filename)s - %(funcName)s - %(levelname)s - %(message)s')


def __invoke_lambda(function_name: str, payload: dict, retry_attempts=5) -> dict:
    regex = r"[a-z]{2}-[a-z]+-\d{1}"
    region = re.search(regex, function_name).group()
    client = boto3.client('lambda', region_name=region, config=BOTO3_CONFIG)
    attempt = 1
    while attempt <= retry_attempts:
        start_time_ns = perf_counter_ns()
        response = client.invoke(
                FunctionName=function_name,
                InvocationType='RequestResponse',
                Payload=json.dumps(payload)
            )
        result = json.loads(response.get("Payload").read().decode("utf-8"))
        if "errorMessage" in result:
            logging.warning(f"Request failed at attempt {attempt}, trying again...")
            if "Error: Runtime exited with error: signal: killed" in result.get('errorMessage'):
                logging.warning(f"Lambda function {function_name} was killed due to insufficient memory!")
                logging.warning(f"Aborting retry attempts!")
                break
            attempt += 1
        else:
            result["total_exec_time_ns"] = perf_counter_ns() - start_time_ns
            logging.info(f"Lambda function {function_name} invoked successfully!")
            return result

    raise Exception(f"Failed to invoke lambda function: {function_name}!")


def __invoke_cloud_function_http(function_name: str, payload: dict, project_name="masterthesis-394010",
                                 retry_attempts=3) -> dict:
    """Invokes google cloud function per HTTP and implements basic retry logic in case of a failed request"""
    identity_token = __retrieve_token_for_gcp()
    regex = r"[a-z]+-[a-z]+\d{1}"
    region = re.search(regex, function_name).group()

    url = f"https://{region}-{project_name}.cloudfunctions.net/{function_name}"
    headers = {
        "Authorization": f"Bearer {identity_token}",
        "Content-Type": "application/json"
    }

    attempt = 0
    while attempt < retry_attempts:
        try:
            start_time_ns = perf_counter_ns()
            response = requests.post(url=url, headers=headers, json=payload, timeout=READ_TIMEOUT)
            response.raise_for_status()
            result = json.loads(response.text.strip())
            result["total_exec_time_ns"] = perf_counter_ns() - start_time_ns
            logging.info(f"Cloud function {function_name} invoked successfully!")
            return result
        except:
            logging.warning(f"Request failed at attempt {attempt}, trying again...")
            attempt += 1

    raise Exception(f"Failed to invoke cloud function: {function_name}!")


def __retrieve_token_for_gcp() -> str:
    """Retrieves the identity token for the current user"""
    try:
        return subprocess.check_output(
            ["gcloud", "auth", "print-identity-token"],
            stderr=subprocess.STDOUT
        ).decode("utf-8").strip()
    except:
        logging.error(f"Failed to retrieve identity token!")
        print(f"Failed to retrieve identity token!")
        return __retrieve_token_for_gcp()


def invoke_serverless_function(function: str, payload: dict, provider: str, retry_attempts=4) -> dict:
    """Invokes the function with the given payload"""
    if provider.lower() == "aws":
        return __invoke_lambda(
            function_name=function,
            payload=payload,
            retry_attempts=retry_attempts
        )
    elif provider.lower() == "gcp":
        return __invoke_cloud_function_http(
            function_name=function,
            payload=payload,
            retry_attempts=retry_attempts
        )
    else:
        raise Exception("Unknown provider")


def warm_up_function(function: str, payload: list, provider: str, max_workers: int) -> None:
    """Warms up the function by invoking it once"""
    # only warm up as many instances as there are payloads
    if len(payload) < max_workers:
        max_workers = len(payload)
    logging.info(f"Warming up {max_workers} instances of function {function}.")
    with concurrent.futures.ThreadPoolExecutor(max_workers=max_workers) as executor:
        futures = list(
            executor.submit(
                invoke_serverless_function,
                function=function,
                payload={},
                provider=provider,
                retry_attempts=1
            ) for _ in range(max_workers)
        )
        for future in concurrent.futures.as_completed(futures):
            try:
                result = future.result()
                print(result)
            except Exception as _:
                # The function invocation should fail because the payload is empty
                pass

    logging.info(f"Function {function} with {max_workers} instances warmed up!")


def invoke_function(function: str, payload: list, provider: str, for_each_collection: str | None, max_workers: int,
                    retry_attempts=3) -> dict:
    """
    Invokes a serverless function concurrently with a given payload. The function gets invoked concurrently for each
    item in the payload list.

    Args:
        function (str): The name of the serverless function to invoke.
        payload (list): A list of payloads to be processed by the function.
        provider (str): The provider of the serverless function.
        for_each_collection (str|None): The name of the for each collection iterable in parallel.
        max_workers (int): The maximum number of workers to use for invoking the function.
        retry_attempts (int): The maximum number of retry attempts for function invocation (default is 4).

    Returns:
        dict: A dictionary containing the merged results of invoking the function on each payload.

    """
    results = []
    logging.info(f"Invoking function {function}..")
    # adjust the maximum number of workers based on the concurrency limit of the provider
    with concurrent.futures.ThreadPoolExecutor(max_workers=max_workers) as executor:
        # Use list() to convert the generator to a list of futures
        futures = list(
            executor.submit(
                invoke_serverless_function,
                provider=provider,
                function=function,
                payload=item,
                retry_attempts=retry_attempts
            ) for item in payload
        )

        # Collect results as they complete
        for future in concurrent.futures.as_completed(futures):
            result = future.result()
            results.append(result)

    return reduce_function_results(results, for_each_collection)


def reduce_function_results(results: list, parallel_collection: str) -> dict:
    if len(results) == 1:
        return results[0]

    if parallel_collection is None:
        raise Exception("Function should be parallel")

    # keys which should be aggregated
    aggregation_keys = ["total_exec_time_ns", "codeExecutionTimeNs", "functionExecutionTimeNs"]
    # keys which should be extended
    extension_keys = ["fileTransfers", parallel_collection]

    final_dict = results.pop()

    for result in results:
        for key, value in result.items():
            if key in extension_keys:
                final_dict[key].extend(value)
            elif key in aggregation_keys:
                if isinstance(final_dict[key], list):
                    final_dict[key].append(value)
                else:
                    final_dict[key] = [final_dict[key], value]
            else:
                final_dict[key] = value

    # compute median for aggregation keys
    for key in aggregation_keys:
        if isinstance(final_dict[key], list):
            final_dict[key] = np.median(final_dict[key])

    return final_dict
