import pandas as pd


class MonitorResult:
    def __init__(self, monitor_results: list) -> None:
        """Creates a new monitor result object"""
        self.__monitor_results = monitor_results

        self.__accessed_files = [result.get("accessedFiles", []) for result in self.__monitor_results]
        self.__created_files = [result.get("createdFiles", []) for result in self.__monitor_results]

    def __str__(self) -> str:
        """Returns a string representation of the monitor result"""
        return f"Accessed files: {self.__accessed_files}\nCreated files: {self.__created_files}\nIteration object: {self.__iteration_object}"

    def get_accessed_files_as_series(self) -> pd.Series:
        """Returns a pandas series of accessed files"""
        accessed_set = {file for file_list in self.__accessed_files for file in file_list}
        return pd.Series(list(accessed_set))

    def get_created_files_as_series(self) -> pd.Series:
        """Returns a pandas series of created files"""
        created_set = {file for file_list in self.__created_files for file in file_list}
        return pd.Series(list(created_set))

    def get_accessed_files(self) -> list:
        """Returns a list of accessed files"""
        accessed_set = {file for file_list in self.__accessed_files for file in file_list}
        return list(accessed_set)

    def get_created_files(self) -> list:
        """Returns a list of created files"""
        created_set = {file for file_list in self.__created_files for file in file_list}
        return list(created_set)

    def get_for_each_iterations(self) -> list:
        """Returns the monitor result"""
        return [
            {
                "downloadFilePaths": monitor_result.get("accessedFiles", []),
                "uploadFilePaths": monitor_result.get("createdFiles", []),
                "iterationObject": monitor_result.get("iterationObject", [])
            }
            for monitor_result in self.__monitor_results
        ]
