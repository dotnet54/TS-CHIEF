package util.weka;

import weka.classifiers.meta.RotationForest;
import weka.classifiers.trees.DecisionStump;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Utils;

public class RotationForestStump extends RotationForest{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	public RotationForestStump() {
		super(); //TODO if replacing J48 then note that this creates a new J48 which might be discarded without using
		
	    m_Classifier = new TSDecisionStump();
	    DecisionStump base_classififer = ((DecisionStump)(m_Classifier));
		
//		J48 base_classififer = ((J48)(m_Classifier));
		
		//TODO customize J48 here
//		base_classififer.set
	}
	
	public int getBranch(Instance instance) throws Exception {
		
		TSDecisionStump base_classififer = ((TSDecisionStump)(m_Classifiers[0]));

		m_RemoveUseless.input(instance);
		instance = m_RemoveUseless.output();
		m_RemoveUseless.batchFinished();

		m_Normalize.input(instance);
		instance = m_Normalize.output();
		m_Normalize.batchFinished();
		
		
		Instance converted = this.convertInstance(instance, 0); //TODO i
		
		return base_classififer.getBranch(converted);
	}
	
	/**
	 * Calculates the class membership probabilities for the given test instance.
	 *
	 * @param instance the instance to be classified
	 * @return preedicted class probability distribution
	 * @throws Exception if distribution can't be computed successfully
	 */
	@Override
	public double[] distributionForInstance(Instance instance) throws Exception {

		m_RemoveUseless.input(instance);
		instance = m_RemoveUseless.output();
		m_RemoveUseless.batchFinished();

		m_Normalize.input(instance);
		instance = m_Normalize.output();
		m_Normalize.batchFinished();

		double[] sums = new double[instance.numClasses()], newProbs;

		for (int i = 0; i < m_Classifiers.length; i++) {
			Instance convertedInstance = convertInstance(instance, i);
			if (instance.classAttribute().isNumeric() == true) {
				sums[0] += m_Classifiers[i].classifyInstance(convertedInstance);
			} else {
				newProbs = m_Classifiers[i].distributionForInstance(convertedInstance);
				for (int j = 0; j < newProbs.length; j++)
					sums[j] += newProbs[j];
			}
		}
		if (instance.classAttribute().isNumeric() == true) {
			sums[0] /= (double) m_NumIterations;
			return sums;
		} else if (Utils.eq(Utils.sum(sums), 0)) {
			return sums;
		} else {
			Utils.normalize(sums);
			return sums;
		}
	}
	
	/**
	 * Transforms an instance for the i-th classifier.
	 *
	 * @param instance the instance to be transformed
	 * @param i        the base classifier number
	 * @return the transformed instance
	 * @throws Exception if the instance can't be converted successfully
	 */
	@Override
	protected Instance convertInstance(Instance instance, int i) throws Exception {
		Instance newInstance = new DenseInstance(m_Headers[i].numAttributes());
		newInstance.setWeight(instance.weight());
		newInstance.setDataset(m_Headers[i]);
		int currentAttribute = 0;

		// Project the data for each group
		for (int j = 0; j < m_Groups[i].length; j++) {
			Instance auxInstance = new DenseInstance(m_Groups[i][j].length + 1);
			int k;
			for (k = 0; k < m_Groups[i][j].length; k++) {
				auxInstance.setValue(k, instance.value(m_Groups[i][j][k]));
			}
			auxInstance.setValue(k, instance.classValue());
			auxInstance.setDataset(m_ReducedHeaders[i][j]);
			m_ProjectionFilters[i][j].input(auxInstance);
			auxInstance = m_ProjectionFilters[i][j].output();
			m_ProjectionFilters[i][j].batchFinished();
			for (int a = 0; a < auxInstance.numAttributes() - 1; a++) {
				newInstance.setValue(currentAttribute++, auxInstance.value(a));
			}
		}

		newInstance.setClassValue(instance.classValue());
		return newInstance;
	}
	
	public int getAttribIndex() {
		return ((TSDecisionStump)(m_Classifiers[0])).getAttribIndex();
	}
	
	public double getSplitPoint() {
		return ((TSDecisionStump)(m_Classifiers[0])).getSplitPoint();
	}
	
	public double[][] getDistribution(){
		return ((TSDecisionStump)(m_Classifiers[0])).getDistribution();
	}
	
	public double[][] getUnNormalizedDistribution(){
		return ((TSDecisionStump)(m_Classifiers[0])).getUnNormalizedDistribution();
	}	
	
}
