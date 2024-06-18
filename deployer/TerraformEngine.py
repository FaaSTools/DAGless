import os
import json
import logging
import subprocess

from jinja2 import Environment, PackageLoader, select_autoescape

from modules.DeploymentConfiguration import DeploymentConfiguration

logging.basicConfig(level=logging.INFO, format='%(name)s - %(levelname)s - %(message)s')


class TerraformEngine:
    def __init__(self, path_to_config, output_path) -> None:
        deployment_config = DeploymentConfiguration(path_to_config)
        self.__engine_config = deployment_config.get_engine_config()
        self.__invocation_config = deployment_config.get_invocation_config()
        self.__path_to_split_functions = deployment_config.get_path_to_split_functions()
        self.__output_path = output_path

        env = Environment(
            loader=PackageLoader('TerraformEngine'),
            autoescape=select_autoescape(['j2'])
        )

        self.__function_template = env.get_template(self.__engine_config.get("jinja_template"))
        self.__bucket_template = env.get_template("terraform_storage.j2")
        self.__provider_template = env.get_template("terraform_provider.j2")
        self.__template_vars = self.__engine_config.get("jinja_template_vars")

    def compile_sources(self) -> None:
        """
        Compiles AWS and GCP functions in parallel and removes unnecessary files.

        The method walks through the project directory, identifies AWS and GCP functions, and compiles them in parallel
        using Maven. It checks for successful compilation and removes unnecessary JAR files.

        Raises:
            RuntimeError: If split-function.jar is not created.

        :return: None
        """
        logging.info("Compiling sources...")

        function_list_aws = [os.path.join(dirpath, dirname) for dirpath, dirnames, _ in
                             os.walk(self.__path_to_split_functions)
                             for dirname in dirnames if "aws" in dirname]

        function_list_gcp = [os.path.join(dirpath, dirname) for dirpath, dirnames, _ in
                             os.walk(self.__path_to_split_functions)
                             for dirname in dirnames if "gcp" in dirname]

        compile_command = ["mvn", "clean", "package"]

        # Compile all functions in parallel
        child_processes = []
        for function_list in [function_list_aws, function_list_gcp]:
            for function in function_list:
                child_processes.append(
                    subprocess.Popen(compile_command, cwd=function, stdout=subprocess.DEVNULL,
                                     stderr=subprocess.STDOUT))

        for child_process in child_processes:
            child_process.wait()

        for function in function_list_gcp:
            target_path = os.path.join(function, "target", "split-function.jar")
            if not os.path.exists(target_path):
                trailing_path = os.sep.join(os.path.normpath(target_path).split(os.sep)[-3:-2])
                raise RuntimeError("Could not create split-function.jar for function " + trailing_path)

        for function in function_list_gcp:
            target_dir = os.path.join(function, "target")
            for _, _, filenames in os.walk(target_dir):
                for filename in filenames:
                    if filename.endswith(".jar") and not filename.startswith("split-function"):
                        os.remove(os.path.join(target_dir, filename))

        logging.info("Compilation done!")

    def write_template_to_file(self, template, filename) -> None:
        """Writes a Terraform template to a file."""
        file_path = os.path.join(self.__output_path, filename)
        # make directory if it does not exist
        os.makedirs(os.path.dirname(file_path), exist_ok=True)
        with open(file_path, "w") as fh:
            fh.write(template.render(self.__template_vars))

    def write_terraform_template(self) -> None:
        """Writes the Terraform templates to files."""
        self.write_template_to_file(self.__function_template, "functions.tf")
        self.write_template_to_file(self.__bucket_template, "storage.tf")
        self.write_template_to_file(self.__provider_template, "provider.tf")

    def write_invocation_config(self, path_to_invocation_config) -> None:
        with open(path_to_invocation_config, "w") as fh:
            fh.write(json.dumps(self.__invocation_config, indent=4))
