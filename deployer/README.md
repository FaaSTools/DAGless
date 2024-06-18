# Deployer

This is Deployer of DAGless. It is responsible for deploying the function created by the `workflowAgnosticFaaSifier` to the FaaS platform including its files.

## Usage

First install the dependencies defined in the Pipfile.

Next ensure that Terraform is installed and configured on your machine.

Finally set the `PROJECT_PATH`variable in the `deploy.py` file to the path to pointing to the `workflowConfig.json`. Upon execution of the `deploy.py` the Terraform script will be generated in the `terraform` directory. This can then simply be executed using the command `terraform apply`.

Further a `invocation_config.json` will be generated. This file contains the information required to invoke the function.