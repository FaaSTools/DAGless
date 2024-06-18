# Workflow Agnostic FaaSifier

The Workflow Agnostic FaaSifier is dependent on the libraries in the `libs` directory. All of which should be installed in the local Maven repository before usage.

## Usage

Firstly a `workflowConfig.json` file should be created in the. A sample can be found in the `src/` directory.

A more detailed description of the `workflowConfig.json` will be added in the future.

Next the create the jar using the command `mvn clean package`. This will create a `WorkflowAgnosticFaaSifier.jar` in the `target/` directory.

Finally run the jar using the command `java -jar target/WorflowAgnosticFaaSifier.jar -c <pathToWorkflowConfig.json>`

This will create the functions in the directory specified in the `workflowConfig.json` file.