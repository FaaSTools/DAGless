import copy

from modules.ServerlessCostEstimator import compute_runtime_cost
from modules.FileTransferCostEstimator import compute_file_transfer_cost


class FunctionDeployment:
    def __init__(
            self,
            function_provider: str,
            function_region: str,
            function_memory_mb: int,
            storage_provider: str,
            storage_region: str,
            code_exec_time_ns: int,
            invocation_latency_ns: float,
            download_files: set,
            upload_files: set,
            download_bandwidth_mbs: float,
            upload_bandwidth_mbs: float,
    ):
        self.function_provider = function_provider
        self.function_region = function_region
        self.function_memory_mb = function_memory_mb
        self.storage_provider = storage_provider
        self.storage_region = storage_region
        self.code_exec_time_ns = code_exec_time_ns
        self.invocation_latency_ns = invocation_latency_ns
        self.download_files = download_files
        self.upload_files = upload_files
        self.download_bandwidth_mbs = download_bandwidth_mbs
        self.upload_bandwidth_mbs = upload_bandwidth_mbs

    def merge(self, function_deployment):
        """Reduces the current function deployment by the given function deployment"""
        required_equal_attributes = [
            "function_provider",
            "function_region",
            "function_memory_mb",
            "storage_provider",
            "storage_region",
            "invocation_latency_ns",
            "download_bandwidth_mbs",
            "upload_bandwidth_mbs"
        ]
        for attribute in required_equal_attributes:
            if getattr(self, attribute) != getattr(function_deployment, attribute):
                raise Exception(f"Cannot reduce function deployments with different {attribute}")

        # the code execution time can simply be added since we use the same memory deployment
        self.code_exec_time_ns += function_deployment.code_exec_time_ns

        self.download_files = (self.download_files.union(
            function_deployment.get_download_files()).difference(set(self.upload_files)))

        self.upload_files = self.upload_files.union(function_deployment.get_upload_files())

    def mergeable(self, function_deployment) -> bool:
        required_equal_attributes = [
            "function_provider",
            "function_region",
            "function_memory_mb",
            "storage_provider",
            "storage_region",
            "invocation_latency_ns",
            "download_bandwidth_mbs",
            "upload_bandwidth_mbs"
        ]
        return all(getattr(self, attribute) == getattr(function_deployment, attribute) for attribute in
                   required_equal_attributes)

    def get_cost(self) -> float:
        runtime_cost = compute_runtime_cost(self)
        file_transfer_cost = compute_file_transfer_cost(self)
        return (runtime_cost + file_transfer_cost) * 1e3

    def get_files_transferred(self) -> str:
        return f"download_files={self.download_files}, upload_files={self.upload_files}"

    def get_runtime_s(self) -> float:
        download_time_s = sum([file.transfer_time_ns for file in self.download_files]) / 1e9
        upload_time_s = sum([file.transfer_time_ns for file in self.upload_files]) / 1e9
        code_exec_time_s = self.code_exec_time_ns / 1e9
        return download_time_s + code_exec_time_s + upload_time_s

    def get_total_download_size_mb(self) -> float:
        return sum([file.size_mb for file in self.download_files])

    def get_total_upload_size_mb(self) -> float:
        return sum([file.size_mb for file in self.upload_files])

    def get_download_files(self) -> set:
        return {copy.deepcopy(file) for file in self.download_files}

    def get_upload_files(self) -> set:
        return {copy.deepcopy(file) for file in self.upload_files}

    def __str__(self):
        return (f"FunctionDeployment: function_memory_mb={self.function_memory_mb}, "
                f"code_exec_time_ns={self.code_exec_time_ns}, cost={self.get_cost()}, "
                f"runtime={self.get_runtime_s()}, "
                f"function_region={self.function_region}, "
                f"storage_region={self.storage_region}"
                f"download_files={len(self.download_files)}, "
                f"upload_files={len(self.upload_files)}, "
                f"download_size_mb={self.get_total_download_size_mb()}, "
                f"upload_size_mb={self.get_total_upload_size_mb()}")


    def __repr__(self):
        return self.__str__()
