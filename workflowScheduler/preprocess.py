from modules.SchedulePreprocessing import SchedulePreprocessing


def main():
    preprocessing = SchedulePreprocessing(path_to_benchmarks="") # path to the benchmarks dir

    functions_df = preprocessing.preprocess_functions()
    file_transfer_df = preprocessing.preprocess_file_transfers()

    functions_df.to_csv(path_or_buf="") # path to store the functions.csv
    file_transfer_df.to_csv(path_or_buf="") # path to store the file_transfers.csv

if __name__ == "__main__":
    main()
