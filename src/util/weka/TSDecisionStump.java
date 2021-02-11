package util.weka;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;

public class TSDecisionStump extends weka.classifiers.trees.DecisionStump{

	/**
	 * 
	 */
	private static final long serialVersionUID = 6930527907221931771L;
	protected double[][] m_UnNormalizedClassDistribution;

	public TSDecisionStump() {
		super();
	}
	
	public int getAttribIndex() {
		return this.m_AttIndex;
	}
	
	public double getSplitPoint() {
		return this.m_SplitPoint;
	}
	
	public double[][] getDistribution(){
		return m_Distribution;
	}
	
	public double[][] getUnNormalizedDistribution(){
		return m_UnNormalizedClassDistribution;
	}	
	
//	@Override
//	public double[] distributionForInstance(Instance instance) throws Exception {
//
//		// default model?
//		if (m_ZeroR != null) {
//			return m_ZeroR.distributionForInstance(instance);
//		}
//
//		return m_Distribution[whichSubset(instance)];
//	}
	
	public int getBranch(Instance instance) throws Exception {
		return whichSubset(instance);
	}
	
	
	/**
	 * Returns the subset an instance falls into.
	 * 
	 * @param instance the instance to check
	 * @return the subset the instance falls into
	 * @throws Exception if something goes wrong
	 */
	@Override
	protected int whichSubset(Instance instance) throws Exception {

		if (instance.isMissing(m_AttIndex)) {
			return 2;
		} else if (instance.attribute(m_AttIndex).isNominal()) {
			if ((int) instance.value(m_AttIndex) == m_SplitPoint) {
				return 0;
			} else {
				return 1;
			}
		} else {
//			System.out.println("which subset: " + instance.value(m_AttIndex) + " vs " + m_SplitPoint);
			if (instance.value(m_AttIndex) <= m_SplitPoint) {
				return 0;
			} else {
				return 1;
			}
		}
	}
	
	/**
	 * Generates the classifier.
	 *
	 * @param instances set of instances serving as training data
	 * @throws Exception if the classifier has not been generated successfully
	 */
	@Override
	public void buildClassifier(Instances instances) throws Exception {

		double bestVal = Double.MAX_VALUE, currVal;
		double bestPoint = -Double.MAX_VALUE;
		int bestAtt = -1, numClasses;

		// can classifier handle the data?
		getCapabilities().testWithFail(instances);

		// remove instances with missing class
		instances = new Instances(instances);
		instances.deleteWithMissingClass();

		// only class? -> build ZeroR model
		if (instances.numAttributes() == 1) {
			System.err.println(
					"Cannot build model (only class attribute present in data!), " + "using ZeroR model instead!");
			m_ZeroR = new weka.classifiers.rules.ZeroR();
			m_ZeroR.buildClassifier(instances);
			return;
		} else {
			m_ZeroR = null;
		}

		double[][] bestDist = new double[3][instances.numClasses()];

		m_Instances = new Instances(instances);

		if (m_Instances.classAttribute().isNominal()) {
			numClasses = m_Instances.numClasses();
		} else {
			numClasses = 1;
		}

		// For each attribute
		boolean first = true;
		for (int i = 0; i < m_Instances.numAttributes(); i++) {
			if (i != m_Instances.classIndex()) {

				// Reserve space for distribution.
				m_Distribution = new double[3][numClasses];

				// Compute value of criterion for best split on attribute
				if (m_Instances.attribute(i).isNominal()) {
					currVal = findSplitNominal(i);
				} else {
					currVal = findSplitNumeric(i);
				}
				if ((first) || (currVal < bestVal)) {
					bestVal = currVal;
					bestAtt = i;
					bestPoint = m_SplitPoint;
					for (int j = 0; j < 3; j++) {
						System.arraycopy(m_Distribution[j], 0, bestDist[j], 0, numClasses);
					}
				}

				// First attribute has been investigated
				first = false;
			}
		}

		// Set attribute, split point and distribution.
		m_AttIndex = bestAtt;
		m_SplitPoint = bestPoint;
		m_Distribution = bestDist;
		
		//TODO shifaz modified
		m_UnNormalizedClassDistribution = new double[3][instances.numClasses()];
		for (int j = 0; j < 3; j++) {
			System.arraycopy(m_Distribution[j], 0, m_UnNormalizedClassDistribution[j], 0, numClasses);
		}

		//end
		
		if (m_Instances.classAttribute().isNominal()) {
			for (int i = 0; i < m_Distribution.length; i++) {
				double sumCounts = Utils.sum(m_Distribution[i]);
				if (sumCounts == 0) { // This means there were only missing attribute values
					System.arraycopy(m_Distribution[2], 0, m_Distribution[i], 0, m_Distribution[2].length);
					Utils.normalize(m_Distribution[i]);
				} else {
					Utils.normalize(m_Distribution[i], sumCounts);
				}
			}
		}

		// Save memory
		m_Instances = new Instances(m_Instances, 0);
	}
	
}
