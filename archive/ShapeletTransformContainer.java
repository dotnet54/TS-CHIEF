package trees.splitters.st.dev;

import java.util.Random;

import application.AppConfig;
import core.TransformContainer;
import data.timeseries.Dataset;
import data.timeseries.TimeSeries;
import data.timeseries.UTimeSeries;
import data.timeseries.MTSDataset;
import trees.splitters.SplitCriterion;
import trees.splitters.st.ShapeletEx;

public class ShapeletTransformContainer implements TransformContainer{
	private int lower = 0; //smallest shapelet length in %
	private int upper = 1; //largest shapelet length in %
	private int r = 1; //number of intervals == number of shapelets
	private ShapeletEx[] shapelets; 
//	SplittableRandom rand = new SplittableRandom();
	private Random rand = AppConfig.getRand(); //TODO allow setting seed  -- refactor
	private boolean normalize = AppConfig.st_normalize;

	public ShapeletTransformContainer(int l, int u, int r) {
		shapelets = new ShapeletEx[r];
	}

	public void fit(Dataset data) throws Exception {


		for (int i = 0; i < r; i++) {
			
			int r = rand.nextInt(data.size());
			TimeSeries s = data.getSeries(r);
			
			ShapeletEx shapelet = new ShapeletEx(s);
			shapelet.initRandomly(rand);	
			if (normalize) {
				shapelet.normalize();
			}
			shapelets[r] = shapelet;

		}
		
		
	}

	public Dataset transform(Dataset train) {

		for (int i = 0; i < r; i++) {
			
			SplitCriterion sc = trainSingleShapelet(data, indices, shapelet, size);
			
			sc.attribute = i; //add index of shapelet in the list so that we can later find the shapelet that produced this criteria
			
			splitCriteria.add(sc);					
		}

			

		
		return null;
	}

}
