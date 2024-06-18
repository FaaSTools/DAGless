import os
import json


class InvokerConfiguration:
    def __init__(self, path_to_config: str) -> None:
        self.config = self.__validate_config(path_to_config)

    @staticmethod
    def __validate_config(path_to_config: str) -> list:
        """Validate the configuration file and return the configuration as a list."""
        if not os.path.isfile(path_to_config):
            raise FileNotFoundError(f"Configuration file {path_to_config} not found")

        with open(path_to_config, 'r') as config_file:
            config = json.load(config_file)

        if not isinstance(config, list):
            raise TypeError("Config must be of type list")

        for workflow in config:
            required_keys = ["invocation_count", "functions", "bucket"]
            missing_keys = [key for key in required_keys if key not in workflow]

            if missing_keys:
                raise ValueError(f"The following keys must be specified for each workflow: {', '.join(missing_keys)}")

        return config

    def get_workflows(self) -> list:
        return self.config
