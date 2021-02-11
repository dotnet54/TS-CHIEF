import sys
import pandas as pd


def combine_results_into_one_dataframe(results_dir,
                                       dataset,
                                       measures=None,
                                       output_file=""):
    '''
    Load results of all given measures from perExp files in a given parent dir and dataset
    Generates {dataset}.all.csv
    '''
    if measures is None:
        measures = ['euc', 'dtwf', 'dtwr', 'ddtwf', 'ddtwr',
                    'wdtw', 'wddtw', 'lcss', 'msm', 'erp', 'twe']
    experiments_data_list = []

    for m in measures:
        exp_file = f"{results_dir}/{dataset}/{m}-{dataset}-loocv.exp.csv"
        #         print(exp_file)
        df_exp_tmp = pd.read_csv(exp_file, index_col=False)
        df_exp_tmp.rename(columns=lambda x: x.strip(), inplace=True)
        df_exp_tmp = df_exp_tmp.reset_index(drop=True)
        df_exp_tmp['file'] = exp_file
        df_exp_tmp['rowID'] = df_exp_tmp.index
        experiments_data_list.append(df_exp_tmp)

    df_exp = pd.concat(experiments_data_list)
    #     print(f"all: {dataset}")
    if output_file is not None:
        df_exp.to_csv(f"{results_dir}/{dataset}/{dataset}{output_file}.all.csv", index=False)

    return df_exp


# testing code
# results_dir = "E:/git/experiments/knn/23-7-2020/all/"
# test_dir = "E:/git/TS-CHIEF-DEV/out/knn/e1/"
# df_all_per_exp = combine_results_into_one_dataframe(test_dir, 'BasicMotions',output_file='-loocv')
# df_all_per_exp

def find_best_experiments(results_dir,
                          dataset,
                          dependency=None,
                          measures=None,
                          input_file="-train",
                          output_file="",
                          seed=6463564):
    '''
    Uses {dataset}-all.csv as input to find the best experiment per each grouping.
    Groupings can be filtered by dependency or measure
    By default uses all dimensions only
    Generates {dataset}-best-x.csv files
    '''

    if dependency is None:
        dependency = [True, False]
    if measures is None:
        measures = ['euc', 'dtwf', 'dtwr', 'ddtwf', 'ddtwr',
                    'wdtw', 'wddtw', 'lcss', 'msm', 'erp', 'twe']
    df_all = pd.read_csv(f"{results_dir}/{dataset}/{dataset}{input_file}.all.csv")

    df_all = df_all[(df_all['noDims'] != 1) & (df_all['useDependentDims'].isin(dependency)) &
                    (df_all['name'].isin(measures))]

    # filter the relevant columns
    df_all = df_all[['iterationkey', 'dataset', 'trainAccuracy', 'name', 'measure',
                     'noDims', 'useDependentDims', 'dimensionsToUse', 'paramID', 'trainTime', 'seed', 'file']]
    df_all.reset_index(inplace=True, drop=False)
    df_all.rename(columns={"index": "index_all"}, inplace=True)

    # apply the groupby and aggregations
    grp = df_all.groupby(['dataset', 'name', 'noDims', 'useDependentDims'])

    # idxmax does not tie break randomly - could have a significant impact for small datasets
    df_max_accuracy = df_all.iloc[grp['trainAccuracy'].idxmax()]

    # TODO random tie break max
    # df_max_accuracy = df_all.iloc[grp['trainAccuracy'].values.max()]

    df_max_accuracy.reset_index(inplace=True, drop=True)

    if output_file is not None:
        df_max_accuracy.to_csv(f"{results_dir}/{dataset}/{dataset}{output_file}.best.csv", index=True)

    return df_max_accuracy


# testing code
# results_dir = "E:/git/experiments/knn/23-7-2020/all/"
# test_dir = "E:/git/TS-CHIEF-DEV/out/knn/e1/"
# df_best_per_dataset = find_best_experiments(test_dir, 'BasicMotions',[True,False], output_file="-b")
# df_best_per_dataset

# args: seed results_dir dataset_name
def main(argv):
    print("Python Start: " + " ".join(argv))

    # -seed=6463564
    seed = argv[0]
    results_dir = argv[1]
    dataset = argv[2]

    df_all_per_exp = combine_results_into_one_dataframe(results_dir, dataset, output_file='-train')
    df_best_per_dataset = find_best_experiments(results_dir, dataset, dependency=[True, False], output_file="-b",
                                                seed=seed)
    print("Python End:--------------------------------------------")


if __name__ == "__main__":
    sys.argv = "6463564 E:/git/TS-CHIEF-DEV/out/knn/e1/ BasicMotions".split();
    main(sys.argv)

