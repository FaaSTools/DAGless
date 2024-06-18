import re

from modules.FileTransfers import FileTransfers
from modules.MonitorResult import MonitorResult


class FunctionResult:
    """Class to represent the result of a function invocation"""

    def __init__(self, name: str, provider: str, region: str, memory: int, memory_unit: str, code_exec_time_ns: int,
                 function_exec_time_ns: int, total_exec_time_ns: int, storage_name: str, file_transfers: FileTransfers,
                 monitor_result: MonitorResult, statement_range: tuple, parallel: bool, parallel_factor: int) -> None:
        self.name = name
        self.provider = provider
        self.region = region
        self.memory = memory
        self.memory_unit = memory_unit
        self.code_exec_time_ns = code_exec_time_ns
        self.function_exec_time_ns = function_exec_time_ns
        self.total_exec_time_ns = total_exec_time_ns
        self.storage_name = storage_name
        self.file_transfers = file_transfers
        self.monitor_result = monitor_result
        self.statement_range = statement_range
        self.parallel = parallel
        self.parallel_factor = parallel_factor

    def get_name(self) -> str:
        """Returns the name of the function"""
        return self.name

    def get_function_order(self) -> str:
        """Returns the order of the function"""
        regex = r".{3}_\d+"
        match = re.search(regex, self.name).group()
        return match[4:]

    def get_provider(self) -> str:
        """Returns the provider of the function"""
        return self.provider

    def get_region(self) -> str:
        """Returns the region of the function"""
        return self.region

    def get_memory(self) -> int:
        """Returns the memory of the function"""
        return self.memory

    def get_memory_unit(self) -> str:
        """Returns the memory unit of the function"""
        return self.memory_unit

    def get_code_exec_time_ns(self) -> int:
        """Returns the whitebox execution time in ns"""
        return self.code_exec_time_ns

    def get_total_exec_time_ns(self) -> int:
        """Returns the blackbox execution time in ns"""
        return self.total_exec_time_ns

    def get_invocation_latency_ns(self) -> int:
        """Returns the latency in ns computed like this:
        black_box_time - function_exec_time"""
        return self.total_exec_time_ns - self.function_exec_time_ns

    def get_storage(self) -> str:
        """Returns the storage name of the function"""
        return self.storage_name

    def get_storage_region(self) -> str:
        """Returns the region of the bucket"""
        if self.provider == "aws":
            regex = r"[a-z]{2}-[a-z]+-\d{1}"
            return re.search(regex, self.storage_name).group()
        elif self.provider == "gcp":
            regex = r"[a-z]+-[a-z]+\d{1}"
            return re.search(regex, self.storage_name).group()
        else:
            raise Exception("Unknown provider")

    def get_storage_provider(self) -> str:
        if self.storage_name[:2] == "gs":
            return "gcp"
        elif self.storage_name[:2] == "s3":
            return "aws"
        else:
            raise Exception("Unknown storage provider!")

    def get_file_transfers(self) -> FileTransfers:
        """Returns FileTransfers of the function"""
        return self.file_transfers

    def get_monitor_result(self) -> MonitorResult:
        """Returns MonitorResult of the function"""
        return self.monitor_result

    def get_statement_range(self) -> str:
        """Returns the statement range of the function"""
        return f"{self.statement_range[0]}-{self.statement_range[1]}"

    def get_parallel(self) -> bool:
        """Returns whether the function contains a for each loop or not"""
        return self.parallel

    def get_parallel_factor(self) -> int:
        """Returns the parallel factor of the function"""
        return self.parallel_factor

    def get_function_exec_time_ns(self) -> int:
        """Returns the function execution time in ns"""
        return self.function_exec_time_ns
