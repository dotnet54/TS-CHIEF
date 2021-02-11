package trees.boosting;

import application.AppConfig;
import core.threading.MultiThreadedTasks;
import data.timeseries.MTSDataset;
import data.timeseries.TimeSeries;
import trees.TSCheifForest;
import trees.TSCheifTree;
import util.PrintUtilities;

public class BoostedTSChiefForest extends TSCheifForest {

    public BoostedTSChiefForest(int forest_id, MultiThreadedTasks parallel_tasks){
        super(forest_id, parallel_tasks);
    }

    public void fit(MTSDataset train_data) throws Exception {
        result.startTimeTrain = System.nanoTime();

        System.out.println("Boosting: " + AppConfig.boosting);
        //Charlotte: With boosting
        double epsilon_t; // weighted error for the tree t
        MTSDataset sample = train_data;
        for (int i = 0; i < AppConfig.num_trees; i++) {
            epsilon_t = 1;
            while (epsilon_t>0.5) {
                trees[i].train(sample, null);
                //-- compute epsilon_t
                epsilon_t = trees[i].tree_weighted_error(sample);
                //System.out.print("epsilon=" + epsilon_t + "\n");
                if (epsilon_t > 0.5) { 			// worse than random classifier
                    //System.out.print("Worse than random classifier\n");
                    trees[i] = new TSCheifTree(i, this);
                    sample = train_data.bootstrap(1.0);
                }
            }
            if (epsilon_t == 0.0) {				// perfect classifier
                sample = train_data.bootstrap(1.0);
            } else {
                trees[i].beta = epsilon_t / (1.0-epsilon_t);
                for (int j = 0; j < sample.size(); j++) {
                    TimeSeries query = sample.getSeries(j);
                    Integer predicted_label = trees[i].predict(query, j);

                    if (!predicted_label.equals(query.label())) {
                        sample.getSeries(j).setWeight( sample.getSeries(j).weight()/(2*epsilon_t));
                    } else {
                        sample.getSeries(j).setWeight( sample.getSeries(j).weight()/(2*(1.0-epsilon_t)));
                    }
                    if (sample.getSeries(j).weight()<1e-8) {
                        sample.getSeries(j).setWeight(1e-8);
                    }
                }
            }
            if (AppConfig.verbosity > 0) {
                System.out.print(i+".");
                if (AppConfig.verbosity > 2) {
                    PrintUtilities.printMemoryUsage(true);
                    if ((i+1) % 20 == 0) {
                        System.out.println();
                    }
                }
            }
            if (epsilon_t!=0) {
                System.out.print(" Trees " + i + " -- beta=" + trees[i].beta + " -- epsilon=" + epsilon_t + "\n");
            }
        }

        result.endTimeTrain = System.nanoTime();
        result.elapsedTimeTrain = result.endTimeTrain - result.startTimeTrain;

        if (AppConfig.verbosity > 0) {
            System.out.print("\n");
        }

//		System.gc();
        if (AppConfig.verbosity > 0) {
            PrintUtilities.printMemoryUsage();
        }

    }

}
