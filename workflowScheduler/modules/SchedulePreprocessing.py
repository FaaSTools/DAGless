import os
import logging

import pandas as pd

logging.basicConfig(level=logging.INFO, format='%(name)s - %(levelname)s - %(message)s')


class SchedulePreprocessing:
    def __init__(self, path_to_benchmarks: str) -> None:
        if not os.path.isdir(path_to_benchmarks):
            raise Exception("Cannot find directory containing benchmarks!")
        self.path_to_benchmarks = path_to_benchmarks

    def preprocess_functions(self) -> pd.DataFrame:
        logging.info("Preprocessing functions...")
        workflow_result_df = self.__create_workflow_results_df()
        logging.info("Preprocessing functions finished!")
        return workflow_result_df

    def preprocess_file_transfers(self) -> pd.DataFrame:
        file_transfer_df = self.__create_file_transfer_df()
        return file_transfer_df

    def __create_file_transfer_df(self) -> pd.DataFrame:
        directory_list = os.listdir(self.path_to_benchmarks)
        # only include directories - MacOS creates a .DS_Store file which is not a directory
        directory_list = list(
            filter(
                lambda x: os.path.isdir(os.path.join(self.path_to_benchmarks, x)),
                directory_list
            )
        )

        # each directory represents a workflow and is assumed to contain a workflow_result.csv
        file_transfers_df = [
            pd.read_csv(os.path.join(self.path_to_benchmarks, directory, "file_transfers.csv"))
            for directory in directory_list
        ]
        return pd.concat(file_transfers_df, axis=0).reset_index(drop=True)

    @staticmethod
    def __set_median_to_column(df: pd.DataFrame, column: str):
        """
       Computes the median of a pd.Series in a pd.DataFrame and sets the values in the specified column.

       :param df: DataFrame containing the data.
       :param column: Name of the column for which to compute and set the median.
       """
        if column not in df:
            raise ValueError(f"Column \"{column}\" not defined in the DataFrame!")

        median = df[column].median()
        df.loc[:, column] = median

    def __set_median_to_columns(self, df: pd.DataFrame) -> None:
        """Sets the median value for all rows in the dataframe for download_bandwidth_mbs, upload_bandwidth_mbs and
        invocation_latency_ns"""
        self.__set_median_to_column(df, "download_bandwidth_mbs")
        self.__set_median_to_column(df, "upload_bandwidth_mbs")
        self.__set_median_to_column(df, "invocation_latency_ns")

    def __create_workflow_results_df(self) -> pd.DataFrame:
        """
        Creates a dataframe from all function benchmarks. Latency and Bandwidth get replaced with the median
        value per memory config and region.

        :param path_to_benchmark_results: Path to the directory containing benchmark results.
        :return: DataFrame with median latency and bandwidth columns.
        """
        directory_list = os.listdir(self.path_to_benchmarks)
        # only include directories - MacOS creates a .DS_Store file which is not a directory
        directory_list = list(
            filter(
                lambda x: os.path.isdir(os.path.join(self.path_to_benchmarks, x)),
                directory_list
            )
        )

        # each directory represents a workflow and is assumed to contain a workflow_result.csv
        workflow_result_dfs = [
            pd.read_csv(os.path.join(self.path_to_benchmarks, directory, "workflow_result.csv"))
            for directory in directory_list
        ]

        for df in workflow_result_dfs:
            # sets median to download_bandwidth_mbs, upload_bandwidth_mbs and invocation_latency_ns
            # each df represents an execution of the workflow in a region with a specific memory config
            self.__set_median_to_columns(df)

        return pd.concat(workflow_result_dfs, axis=0)
