
class File:
    def __init__(self, path: str, size_mb: float, transfer_time_ns):
        self.path = path
        self.size_mb = size_mb
        self.transfer_time_ns = transfer_time_ns

    def get_path(self):
        return self.path

    def get_size_mb(self):
        return self.size_mb

    def __str__(self):
        return f"File: path={self.path}, size_mb={self.size_mb}, transfer_time_ns={self.transfer_time_ns}\n"

    def __repr__(self):
        return f"File: path={self.path}, size_mb={self.size_mb}, transfer_time_ns={self.transfer_time_ns}\n"

    def __eq__(self, other):
        return self.path == other.path

    def __hash__(self):
        return hash(self.path)
