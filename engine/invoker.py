from modules.WorkflowInvoker import WorkflowInvoker
import logging

logging.basicConfig(level=logging.INFO, format='%(name)s - %(levelname)s - %(message)s')


def main():
    invoker = WorkflowInvoker(
        path_to_invocation_config="",           # path to the invocation config 
        path_to_split_faasfifcation_config="",  # path to the workflowConfig.json
        path_to_results="",                     # defines the path to store the results
        worker=10,                              # defines concurrency level
        block_size=1,                           # defines block size
        warm_up=False                           # defines if warm up is needed
    )

    invoker.invoke_workflow_for_learning() # determines data flow
    invoker.invoke_workflow_for_benchmarking(parallel=True) # runs the benchmarking


if __name__ == "__main__":
    main()
