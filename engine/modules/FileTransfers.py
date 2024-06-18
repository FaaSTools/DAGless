import logging
import numpy as np

logging.basicConfig(level=logging.INFO, format='%(name)s - %(levelname)s - %(message)s')


class FileTransfers:
    """Class to represent the file transfers of a function - it flattens the file transfers to a dict of lists"""

    def __init__(self, file_transfers: list) -> None:
        self.number_of_files = sum(len(file_transfer.get("files", [])) for file_transfer in file_transfers)
        self.file_transfers = {
            "storage_provider": [],
            "transfer_type": [],
            "file_path": [],
            "file_size_mb": [],
            "transfer_time_ns": []
        }
        for file_transfer in file_transfers:
            files = file_transfer.get("files", [])
            if not files:
                continue

            provider = file_transfer.get("provider", "").lower()
            transfer_type = file_transfer.get("transferType", "").lower()

            for file in files:
                self.file_transfers["storage_provider"].append(provider)
                self.file_transfers["transfer_type"].append(transfer_type)
                self.file_transfers["file_path"].append(file.get("filePath", ""))
                self.file_transfers["file_size_mb"].append(file.get("fileSizeInMB", np.NAN))
                self.file_transfers["transfer_time_ns"].append(file.get("transferTimeInNanoSeconds", np.NAN))

    def __str__(self) -> str:
        """Returns a string representation of the file transfers"""
        return f"File transfers: {self.file_transfers}"

    def get_total_transfer_time_ns(self) -> int:
        """Returns the total transfer time in ns"""
        return sum(self.file_transfers["transfer_time_ns"])

    def get_total_file_size_mb(self) -> float:
        """Returns the total file size in MB"""
        return sum(self.file_transfers["file_size_mb"])

    def get_total_upload_time_ns(self) -> int:
        """Returns the total upload time in ns"""
        zipped_list = zip(self.file_transfers["transfer_type"], self.file_transfers["transfer_time_ns"])
        return sum([transfer_time for transfer_type, transfer_time in zipped_list if transfer_type == "upload"])

    def get_total_download_time_ns(self) -> int:
        """Returns the total download time in ns"""
        zipped_list = zip(self.file_transfers["transfer_type"], self.file_transfers["transfer_time_ns"])
        return sum([transfer_time for transfer_type, transfer_time in zipped_list if transfer_type == "download"])

    def get_total_upload_size_mb(self) -> float:
        """Returns the total upload size in MB"""
        zipped_list = zip(self.file_transfers["transfer_type"], self.file_transfers["file_size_mb"])
        return sum([file_size for transfer_type, file_size in zipped_list if transfer_type == "upload"])

    def get_total_download_size_mb(self) -> float:
        """Returns the total download size in MB"""
        zipped_list = zip(self.file_transfers["transfer_type"], self.file_transfers["file_size_mb"])
        return sum([file_size for transfer_type, file_size in zipped_list if transfer_type == "download"])

    def get_download_bandwidth_mbs(self) -> float:
        """Returns the approximated download bandwidth in MB/s (not Mb/s) from the storage to the function"""
        download_time_ns = self.get_total_download_time_ns()
        download_size_mb = self.get_total_download_size_mb()
        try:
            download_time_s = download_time_ns / 1e+9
            return download_size_mb / download_time_s
        except ZeroDivisionError:
            return np.NAN

    def get_upload_bandwidth(self) -> float:
        """Returns the approximated upload bandwidth in MB/s (not Mb/s) from the function to the storage"""
        upload_time_ns = self.get_total_upload_time_ns()
        upload_size_mb = self.get_total_upload_size_mb()
        try:
            upload_time_s = upload_time_ns / 1e+9
            return upload_size_mb / upload_time_s
        except ZeroDivisionError:
            return np.NAN

    def get_number_of_files(self) -> int:
        """Returns the number of files"""
        return self.number_of_files
