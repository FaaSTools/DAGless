import os
import json
import logging
import secrets

logging.basicConfig(level=logging.INFO, format='%(name)s - %(levelname)s - %(message)s')


class DeploymentConfiguration:
    def __init__(self, path: str) -> None:
        # Check if the path is a file
        if not os.path.isfile(path):
            raise FileNotFoundError(f"File not found: {path}")
        with open(path, "r") as config_file:
            self.__config = json.load(config_file)

        self.__template_variables = {}
        self.__jinja_template = ""
        self.__invocation_config = []
        self.__bucket_deployment = {}
        self.__entry_point = ""
        self.__nonce = secrets.token_hex(4)

        # Create the template variables for the terraform template
        self.__create_template_vars()

    def get_engine_config(self):
        return {
            "jinja_template": self.__jinja_template,
            "jinja_template_vars": self.__template_variables
        }

    def get_invocation_config(self):
        return self.__invocation_config

    def __create_invocation_config_benchmark(self) -> None:
        workflow_list = []

        for provider in ["aws", "gcp"]:
            function_deployment = self.__get_function_deployment(provider)

            if function_deployment:
                function_regions = function_deployment.get("function_regions", [])
                memory_config = function_deployment.get("memory_config", [])

                workflow_list.extend(
                    {
                        "invocation_count": function_deployment.get("invocation_count", 1),
                        "functions": [
                            f"{func}-{region}-{memory}MB"
                            for func in self.__template_variables[f"functions_{provider}"]
                        ],
                        "bucket": (
                            f"{self.__get_bucket_prefix(self.__bucket_deployment.get('provider_name'))}"
                            f"file-bucket-"
                            f"{self.__bucket_deployment.get('bucket_region')}-"
                            f"{self.__nonce}/"
                        ),
                    }
                    for region in function_regions
                    for memory in memory_config
                )

        self.__invocation_config = workflow_list

    def __create_template_vars(self) -> None:
        """Creates the template variables for the terraform template"""

        # Validate independent fields and add them to the template variables
        self.__validate_independent_fields()
        # Set independent fields
        self.__entry_point = self.__config["entryPoint"]
        self.__bucket_deployment = self.__config["bucketDeployment"]
        # Add provider-independent template variables to the template variables
        self.__add_provider_provider_independent_template_vars()

        # Determine which deployment type is used and add the corresponding template variables
        if "functionDeployment" not in self.__config:
            logging.info("Using optimized deployment")
            self.__validate_optimized_deployment()
            self.__add_optimized_template_vars()
            self.__create_invocation_config_optimized()
            self.__jinja_template = "terraform_optimized.j2"
        else:
            logging.info("Using benchmark deployment")
            self.__validate_benchmark_deployment()
            self.__add_google_template_vars()
            self.__add_aws_template_vars()
            self.__create_invocation_config_benchmark()
            self.__jinja_template = "terraform_benchmark.j2"

    def __add_provider_provider_independent_template_vars(self) -> None:
        """Adds the provider-independent template variables"""
        self.__template_variables["path_to_split_functions"] = self.__config["pathToSplitFunctions"].rstrip('/')
        self.__template_variables["path_to_dir_monitor"] = self.__config["pathToDirMonitor"].rstrip('/')
        self.__template_variables["path_to_function_files"] = self.__config["pathToFunctionFiles"].rstrip('/')

        self.__template_variables["bucket_region"] = self.__bucket_deployment.get("bucket_region", "").lower()
        self.__template_variables["bucket_provider"] = self.__bucket_deployment.get("provider_name", "").lower()

        self.__template_variables["entry_point"] = self.__entry_point
        self.__template_variables["project_id"] = self.__config.get('GCPProjectId', '')
        self.__template_variables["service_account_email"] = self.__config.get('GCPServiceAccountEmail', '')
        self.__template_variables["lambda_role_arn"] = self.__config.get('AWSLambdaRoleArn', '')

        self.__template_variables["nonce"] = self.__nonce

    def __add_aws_template_vars(self, provider="aws") -> None:
        function_deployment = self.__get_function_deployment(provider)

        if function_deployment is None:
            logging.info(f"No {provider.upper()} functions found")
            return

        provider_region_aws = set(function_deployment.get("function_regions", []))
        if self.__bucket_deployment.get("provider_name") == "aws":
            provider_region_aws.add(self.__bucket_deployment.get("bucket_region"))

        # AWS configuration
        self.__template_variables.update({
            "aws": True,
            "deployment_region_aws": function_deployment.get("deployment_region"),
            "function_regions_aws": function_deployment.get("function_regions"),
            "handler_aws": f"{self.__entry_point}::handleRequest".replace("..", "."),
            "memory_configs_aws": function_deployment.get("memory_config"),
            "provider_regions_aws": list(provider_region_aws),
        })

        self.__add_functions_to_template_vars(provider)

    def __add_functions_to_template_vars(self, provider):
        # Find .jar files in target directory for GCP functions
        project_path = self.get_path_to_split_functions()
        function_list = sorted(
            [
                directory_name
                for _, directory_names, _ in os.walk(project_path)
                for directory_name in directory_names
                if f"{provider.lower()}" in directory_name
            ],
            key=lambda x: int(x.split("_").pop()),
        )
        self.__template_variables[f"functions_{provider.lower()}"] = function_list
        logging.info(f"Found {len(function_list)} functions for {provider.upper()}")

    def __add_google_template_vars(self, provider="gcp") -> None:
        function_deployment = self.__get_function_deployment(provider)

        if function_deployment is None:
            logging.info(f"No {provider.upper()} functions found")
            return

        # GCP configuration
        self.__template_variables.update({
            "gcp": True,
            "deployment_region_gcp": function_deployment.get("deployment_region"),
            "function_regions_gcp": function_deployment.get("function_regions"),
            "memory_configs_gcp": function_deployment.get("memory_config"),
        })

        self.__add_functions_to_template_vars(provider)

    def __get_function_deployment(self, provider):
        function_deployment = next(
            (
                fd for fd in self.__config.get("functionDeployment")
                if fd.get("provider_name") == provider
            ), None
        )
        return function_deployment

    def __add_optimized_template_vars(self) -> None:
        self.__template_variables["optimized_deployment"] = [
            {
                "order": function["order"],
                "provider": function["provider"],
                "region": function["region"],
                "memory": function["memory"],
                "function_name": f"{function.get('provider')}_{function.get('order')}",
            } for function in self.__config.get("configSplitFunctions", [])]

        provider_regions_aws = {function["region"] for function in self.__config.get("configSplitFunctions", [])
                                if function["provider"] == "aws"}
        self.__template_variables["provider_regions_aws"] = list(provider_regions_aws)

    def __create_invocation_config_optimized(self) -> None:
        function_list = [(f"{function.get('provider')}_"
                          f"{function.get('order')}-"
                          f"{function.get('region')}-"
                          f"{function.get('memory')}"
                          f"MB")
                         for function in self.__config.get("configSplitFunctions", [])]

        self.__invocation_config = {
            "invocation_count": 1,
            "functions": function_list,
            "bucket": (f"{self.__get_bucket_prefix(self.__bucket_deployment.get('provider_name'))}"
                       f"file-bucket-"
                       f"{self.__bucket_deployment.get('bucket_region')}-"
                       f"{self.__nonce}")
        }

    def __validate_optimized_deployment(self):
        required_keys = ["order", "provider", "region", "memory"]
        type_checks = {
            "order": int,
            "provider": str,
            "region": str,
            "memory": int,
        }

        for function in self.__config["configSplitFunctions"]:
            for key in required_keys:
                if key not in function:
                    raise KeyError(f"{key} key not found in functionDeployment")

            for key, expected_type in type_checks.items():
                if not isinstance(function[key], expected_type):
                    raise TypeError(f"{key} must be {expected_type.__name__}")

    def __validate_independent_fields(self):
        """Validates the top level keys of the config file"""
        required_keys = ["pathToSplitFunctions", "pathToDirMonitor", "pathToFunctionFiles", "bucketDeployment",
                         "entryPoint", "GCPProjectId", "AWSLambdaRoleArn", "GCPServiceAccountEmail"]

        # Check if the config has the required keys
        for key in required_keys:
            if key not in self.__config:
                raise KeyError(f"{key} key not found in config")

        # Check types
        type_checks = {
            "pathToSplitFunctions": str,
            "pathToDirMonitor": str,
            "pathToFunctionFiles": str,
            "bucketDeployment": dict,
            "entryPoint": str,
            "GCPProjectId": str,
            "AWSLambdaRoleArn": str,
            "GCPServiceAccountEmail": str
        }

        for key, expected_type in type_checks.items():
            if not isinstance(self.__config[key], expected_type):
                raise TypeError(f"{key} must be {expected_type.__name__}")

        self.__validate_bucket_deployment()

    def __validate_benchmark_deployment(self):
        """Validates the function deployment of the config file"""
        for provider in self.__config["functionDeployment"]:
            required_keys = ["provider_name", "deployment_region", "function_regions", "memory_config",
                             "invocation_count"]

            # Check required keys
            for key in required_keys:
                if key not in provider:
                    raise KeyError(f"{key} key not found in provider")

            # Check types
            type_checks = {
                "provider_name": str,
                "deployment_region": str,
                "function_regions": list,
                "memory_config": list,
                "invocation_count": int
            }

            for key, expected_type in type_checks.items():
                if not isinstance(provider[key], expected_type):
                    raise TypeError(f"{key} must be {expected_type.__name__}")

            # Check specific keys for Google Cloud Platform (GCP)
            if provider["provider_name"] == "gcp":

                # Check values for GCP-specific keys
                for region in provider["function_regions"]:
                    if not isinstance(region, str) or len(region) < 1:
                        raise ValueError("regions must contain at least one non-empty string")

                for memory_config in provider["memory_config"]:
                    if not isinstance(memory_config, int) or memory_config < 135 or memory_config > 34400:
                        pass
                        #raise ValueError("memory_config for gcp must be between 135MB and 34400MB")
                        # TODO REACTIVATE THIS LINE AFTER EVALUATION

            # Check values for AWS-specific keys
            if provider["provider_name"] == "aws":
                for region in provider["function_regions"]:
                    if not isinstance(region, str) or len(region) < 1:
                        raise ValueError("regions must contain at least one non-empty string")

                for memory_config in provider["memory_config"]:
                    if not isinstance(memory_config, int) or memory_config < 128 or memory_config > 10240:
                        raise ValueError("memory_config for aws must be between 128MB and 10240MB")

    def __validate_bucket_deployment(self):
        """Validates the bucket deployment of the config file"""
        if "provider_name" not in self.__config.get("bucketDeployment"):
            raise KeyError("provider_name key not found in bucketDeployment")
        if "bucket_region" not in self.__config.get("bucketDeployment"):
            raise KeyError("bucket_region key not found in bucketDeployment")
        if "provider_name" == "gcp" and "project_id" not in self.__config.get("bucketDeployment"):
            raise KeyError("project_id key not found in bucketDeployment")

        # Check types
        type_checks = {
            "provider_name": str,
            "bucket_region": str,
        }
        for key, expected_type in type_checks.items():
            if not isinstance(self.__config["bucketDeployment"][key], expected_type):
                raise TypeError(f"{key} must be {expected_type.__name__}")

    # Getters for the config - the config should not be accessed directly to avoid NoneType errors
    def get_path_to_split_functions(self) -> str:
        return self.__config["pathToSplitFunctions"].rstrip('/')

    @staticmethod
    def __get_bucket_prefix(provider: str) -> str:
        if provider == "aws":
            return f"s3://"
        elif provider == "gcp":
            return f"gs://"
        else:
            raise ValueError(f"Provider {provider} not supported")
