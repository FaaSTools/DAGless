import json
import logging

logging.basicConfig(level=logging.INFO, format='%(name)s - %(levelname)s - %(message)s')


class WorkflowConfig:
    def __init__(self, path_to_config: str) -> None:
        self.__workflow_config = self.__validate_config(path_to_config)

    @staticmethod
    def __validate_config(path_to_config) -> list:
        with open(path_to_config, 'r') as config_file:
            config = json.load(config_file)

        # Ensure the loaded configuration is a list
        if not isinstance(config, list):
            raise TypeError("Config must be of type list")

        # Define required fields and their types
        required_fields = {
            "order": int,
            "statementRange": dict,
            "downloadFilePaths": list,
            "uploadFilePaths": list,
            "inputVariables": list,
            "outputVariables": list,
            "parallel": bool
        }

        # Validate each workflow in the configuration
        for workflow in config:
            # Check for missing keys
            missing_keys = [key for key, type_ in required_fields.items() if key not in workflow]
            if missing_keys:
                raise ValueError(
                    f"The following keys must be specified for each workflow: {', '.join(missing_keys)}")

            # Check types of each key-value pair
            for key, value in workflow.items():
                expected_type = required_fields.get(key)
                if expected_type and not isinstance(value, expected_type):
                    raise TypeError(f"Key '{key}' must be of type {expected_type}")

                # Additional validation for the 'parallel' key
                # if key == "parallel" and value:
                #     if not workflow.get("forEachIterations"):
                #         raise ValueError("Key 'forEachIterations' must be specified for parallel functions")
                #     if not isinstance(workflow["forEachIterations"], list):
                #         raise TypeError("Key 'forEachIterations' must be of type list")

        return config

    def get_parallel_by_order(self, order: int) -> bool:
        """Returns True if the function with the given order is parallel"""
        return next(
            filter(
                lambda x: x.get("order") == order, self.__workflow_config
            ), None
        ).get("parallel")

    def get_function_by_order(self, order: int) -> dict:
        """Returns the function with the given order"""
        return next(
            filter(
                lambda x: x.get("order") == order, self.__workflow_config
            ), None
        )

    def get_for_each_iterations_by_order(self, order: int) -> list:
        """Returns the forEachIterations for the function with the given order"""
        if not self.get_parallel_by_order(order):
            raise ValueError(f"Function with order {order} is not parallel")
        return next(
            filter(
                lambda x: x.get("order") == order, self.__workflow_config
            ), None
        ).get("forEachIterations")

    def get_for_each_collection_by_order(self, order: int) -> str | None:
        """Returns the forEachCollection for the function with the given order"""
        if not self.get_parallel_by_order(order):
            return None
        return next(
            filter(
                lambda x: x.get("parallel") == 'true',
                self.__workflow_config[order].get("inputVariables")
            )
        ).get('identifier')

    def get_statement_range_by_order(self, order: int) -> tuple:
        """Returns the statement range for the function with the given order"""
        function = self.get_function_by_order(order)
        statement_range = function.get("statementRange")

        if statement_range is None:
            raise Exception("Statement range is None")

        start_range = statement_range.get("start")
        end_range = statement_range.get("end")

        if start_range is None or end_range is None:
            raise Exception("Start or end range is None")

        return start_range, end_range

    def get_uris_from_split_function_config(self, bucket, function_order: int, transfer_type: str) -> list:
        """Gets the file URIs (download or upload) of the split function from the split_function_config.json"""
        function = self.get_function_by_order(function_order)
        files = function.get(f"{transfer_type}FilePaths", [])

        bucket = bucket.rstrip('/')
        logging.info(f"Bucket: {bucket}")

        uris = [f"{bucket}/{file.lstrip('/')}" for file in files]

        logging.info(f"Number of {transfer_type.lower()} files for function {function_order}: {len(uris)}")
        return uris

    def set_input_values_by_order(self, order: int, key: str, value: str) -> None:
        """
        Sets the value for the given key in the function with the given order.
        """
        function = self.get_function_by_order(order)

        for input_value in function.get("inputVariables", []):
            if input_value.get("identifier") == key:
                input_value["value"] = value
                return

    def get_input_variables_by_order(self, order: int) -> dict:
        input_variables = self.get_function_by_order(order).get("inputVariables", [])
        return {input_variable.get("identifier"): input_variable.get("value") for input_variable in input_variables}

    def write_config(self, path_to_config: str) -> None:
        """Writes the workflow config to the given path"""
        with open(path_to_config, 'w') as config_file:
            json.dump(self.__workflow_config, config_file, indent=4)

