import logging
import math

#from modules.FunctionDeployment import FunctionDeployment

logging.basicConfig(level=logging.INFO, format='%(name)s - %(levelname)s - %(message)s')

GCP_TIER_1_REGIONS = ["asia-east1", "asia-northeast1", "asia-northeast2", "europe-north1",
                      "europe-southwest1", "europe-west1", "europe-west4", "europe-west8",
                      "europe-west9", "me-west1", "us-central1", "us-east1", "us-east4", "us-east5", "us-south1",
                      "us-west1"]

GCP_TIER_2_REGIONS = ["africa_south1", "asia-east2", "asia-northeast3", "asia-southeast1", "asia-southeast2",
                      "asia-south1",
                      "asia-south2", "australia-southeast1", "australia-southeast2", "europe-central2", "europe-west10",
                      "europe-west12", "europe-west2",
                      "europe-west3", "europe-west6", "me-central1", "me-central2",
                      "northamerica-northeast1", "northamerica-northeast2", "southamerica-east1", "southamerica-west1",
                      "us-west2", "us-west3", "us-west4"]


def __gcp_get_vcpu_by_mebibyte(mib: float):
    if mib <= 128:
        return 0.083
    elif mib <= 256:
        return 0.167
    elif mib <= 512:
        return 0.333
    elif mib <= 1024:
        return 0.583
    elif mib <= 2048:
        return 1.000
    elif mib <= 8192:
        return 2.000
    elif mib <= 16384:
        return 4.000
    elif mib <= 32768:
        return 8.000
    else:
        raise Exception("Memory size not supported")


AWS_REGIONS = regions = [
    "us-east-2",
    "us-east-1",
    "us-west-1",
    "us-west-2",
    "af-south-1",
    "ap-east-1",
    "ap-south-2",
    "ap-southeast-3",
    "ap-southeast-4",
    "ap-south-1",
    "ap-northeast-3",
    "ap-northeast-2",
    "ap-southeast-1",
    "ap-southeast-2",
    "ap-northeast-1",
    "ca-central-1",
    "ca-west-1",
    "eu-central-1",
    "eu-west-1",
    "eu-west-2",
    "eu-south-1",
    "eu-west-3",
    "eu-south-2",
    "eu-north-1",
    "eu-central-2",
    "il-central-1",
    "me-south-1",
    "me-central-1",
    "sa-east-1",
]

AWS_EPHEMERAL_PRICING_GB = [
    0.0000000309,  # Ohio (us-east-2)
    0.0000000309,  # N. Virginia (us-east-1)
    0.000000037,  # N. California (us-west-1)
    0.0000000309,  # Oregon (us-west-2)
    0.0000000404,  # Cape Town (af-south-1)
    0.0000000407,  # Hong Kong (ap-east-1)
    0.0000000352,  # Hyderabad (ap-south-2)
    0.000000037,  # Jakarta (ap-southeast-3)
    0.000000037,  # Melbourne (ap-southeast-2)
    0.0000000352,  # Mumbai (ap-south-1)
    0.000000037,  # Osaka-Local (ap-northeast-3)
    0.0000000352,  # Seoul (ap-northeast-2)
    0.000000037,  # Singapore (ap-southeast-1)
    0.000000037,  # Sydney (ap-southeast-2)
    0.000000037,  # Tokyo (ap-northeast-1)
    0.000000034,  # Montreal (ca-central-1)
    0.000000034,  # Calgary (ca-west-1)
    0.0000000367,  # Frankfurt (eu-central-1)
    0.000000034,  # Ireland (eu-west-1)
    0.0000000358,  # London (eu-west-2)
    0.0000000357,  # Milan (eu-south-1)
    0.0000000358,  # Paris (eu-west-3)
    0.000000034,  # Spain (eu-south-2)
    0.0000000323,  # Stockholm (eu-north-1)
    0.0000000441,  # Zurich (eu-central-2)
    0.0000000408,  # Tel Aviv (il-central-1)
    0.0000000374,  # Bahrain (me-south-1)
    0.0000000374,  # Dubai (me-central-1)
    0.0000000586,  # Sao Paulo (sa-east-1)
]

lambda_one = {
    "regions": ["sa-east-1", "eu-north-1", "us-east-1", "us-east-2", "us-west-1", "us-west-2", "ap-south-2",
                "ap-southeast-3", "ap-southeast-4", "ap-south-1", "ap-northeast-3", "ap-northeast-2", "ap-southeast-1",
                "ap-southeast-2", "ap-northeast-1", "ca-central-1", "ca-west-1", "eu-central-1", "eu-west-1",
                "eu-west-2", "eu-west-3", "eu-south-2"],
    "cost_gb_s": 0.0000166667,
    "cost_m_r": 0.20,
}

lambda_two = {
    "regions": ["af_south-1", ],
    "cost_gb_s": 0.0000221,
    "cost_m_r": 0.27,
}

lambda_three = {
    "regions": ["ap-east-1", ],
    "cost_gb_s": 0.00002292,
    "cost_m_r": 0.28,
}

lambda_four = {
    "regions": ["eu-south-1"],
    "cost_gb_s": 0.0000195172,
    "cost_m_r": 0.23,
}

lambda_five = {
    "regions": ["eu-central-2"],
    "cost_gb_s": 0.0000183334,
    "cost_m_r": 0.22,
}

lambda_six = {
    "regions": ["il-central-1"],
    "cost_gb_s": 0.0000175,
    "cost_m_r": 0.21,
}

lambda_seven = {
    "regions": ["me-south-1", "me-central-1"],
    "cost_gb_s": 0.0000206667,
    "cost_m_r": 0.25,
}

lambda_tiers = [lambda_one, lambda_two, lambda_three, lambda_four, lambda_five, lambda_six, lambda_seven]


def compute_runtime_cost(function_deployment) -> float:
    """Returns the runtime cost of the function"""
    if function_deployment.function_provider.lower() == "aws":
        return __aws_compute_runtime_cost(function_deployment)
    elif function_deployment.function_provider == "gcp":
        return __gcp_compute_runtime_cost(function_deployment)
    else:
        raise Exception("Unknown provider")


def __aws_compute_runtime_cost(function_deployment) -> float:
    """Calculates the AWS runtime cost of the given function.

        The runtime cost is computed using the formula:
        runtime_cost = runtime_ms * price_per_memory_and_region

        Parameters:
        - function (SplitFunction): The function for which the runtime cost needs to be calculated.

        Returns:
        float: The calculated AWS runtime cost.

        Raises:
        Exception: If the provided region is unknown.
        """
    # extract the function's region, memory, runtime, and vCPU
    region = function_deployment.function_region
    memory_gb = function_deployment.function_memory_mb / 1024

    # runtime is the sum of code execution time, download time, and upload time
    runtime_ns = __get_runtime_from_function_deployment(function_deployment)

    runtime_s = runtime_ns / 1e9

    for tier in lambda_tiers:
        if region in tier["regions"]:
            execution_cost = tier["cost_gb_s"] * memory_gb * runtime_s
            invocation_cost = tier["cost_m_r"] / 1e6
            # Ephemeral storage cost is only charged for more than 512MB of memory
            # ephemeral_storage_cost = AWS_EPHEMERAL_PRICING_GB[AWS_REGIONS.index(region)] * 0.5 * runtime_s
            return execution_cost + invocation_cost
    else:
        raise Exception("Unknown region")


def __get_runtime_from_function_deployment(function_deployment) -> float:
    code_exec_time_ns = function_deployment.code_exec_time_ns
    download_time_ns = sum([file.transfer_time_ns for file in function_deployment.get_download_files()])
    upload_time_ns = sum([file.transfer_time_ns for file in function_deployment.get_upload_files()])
    runtime_ns = code_exec_time_ns + download_time_ns + upload_time_ns
    return runtime_ns


def __gcp_compute_runtime_cost(function_deployment) -> float:
    # extract the function's region, memory, runtime, and vCPU
    region = function_deployment.function_region
    memory_gibibyte = function_deployment.function_memory_mb / 1024 # TODO revert after experiments
    memory_mebibyte = function_deployment.function_memory_mb # TODO revert after experiments
    v_cpu = __gcp_get_vcpu_by_mebibyte(memory_mebibyte)

    # runtime is the sum of code execution time, download time, and upload time
    runtime_ns = __get_runtime_from_function_deployment(function_deployment)

    # compute the runtime rounded to 100ms increments and convert it to seconds
    runtime_rounded_s = __gcp_get_runtime_rounded_seconds(runtime_ns)
    # an invocation price of 0.40 USD per 1 million invocations
    invocation_price = 0.40 / 1e6
    if region in GCP_TIER_1_REGIONS:
        v_cpu_cost = 0.00002400 * v_cpu * runtime_rounded_s
        memory_cost = 0.00000250 * memory_gibibyte * runtime_rounded_s
        return v_cpu_cost + memory_cost + invocation_price
    elif region in GCP_TIER_2_REGIONS:
        v_cpu_cost = 0.00003360 * v_cpu * runtime_rounded_s
        memory_cost = 0.00000350 * memory_gibibyte * runtime_rounded_s
        return v_cpu_cost + memory_cost + invocation_price
    else:
        raise Exception("Unknown region")


def __megabyte_to_gibibyte(mb: int) -> float:
    """Converts megabytes to gibibytes"""
    number_of_bytes = mb * 1e6
    return number_of_bytes / (2 ** 30)


def __gcp_get_runtime_rounded_seconds(runtime_ns: float) -> float:
    runtime_ms = runtime_ns / 1e6
    runtime_rounded_ms = int(math.ceil(runtime_ms / 100.0)) * 100
    runtime_rounded_s = runtime_rounded_ms / 1e3
    return runtime_rounded_s


def __megabyte_to_mebibyte(mb: int) -> float:
    """Converts megabytes to mebibytes"""
    number_of_bytes = mb * 1e6
    return number_of_bytes / (2 ** 20)


def main():
    fd1 = FunctionDeployment(
        function_provider="aws",
        function_region="us-east-1",
        function_memory_mb=128,
        storage_provider="aws",
        storage_region="us-east-1",
        code_exec_time_ns=int(1e9),
        invocation_latency_ns=0,
        download_files=set(),
        upload_files=set(),
        download_bandwidth_mbs=100,
        upload_bandwidth_mbs=100
    )

    fd2 = FunctionDeployment(
        function_provider="aws",
        function_region="us-east-1",
        function_memory_mb=128,
        storage_provider="aws",
        storage_region="us-east-1",
        code_exec_time_ns=int(2*1e9),
        invocation_latency_ns=0,
        download_files=set(),
        upload_files=set(),
        download_bandwidth_mbs=100,
        upload_bandwidth_mbs=100
    )

    print(compute_runtime_cost(fd1))
    print(compute_runtime_cost(fd2))
    print(compute_runtime_cost(fd1) * 2 == compute_runtime_cost(fd2))

if __name__ == "__main__":
    main()
