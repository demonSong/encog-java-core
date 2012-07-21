package org.encog.ensemble.adaboost;

import org.encog.ensemble.Ensemble;
import org.encog.ensemble.EnsembleML;
import org.encog.ensemble.EnsembleMLMethodFactory;
import org.encog.ensemble.EnsembleTrainFactory;
import org.encog.ensemble.EnsembleTypes;
import org.encog.ensemble.EnsembleTypes.ProblemType;
import org.encog.ensemble.aggregator.EnsembleAggregator;
import org.encog.ensemble.bagging.BaggingML;
import org.encog.ensemble.data.factories.EnsembleDataSetFactory;
import org.encog.mathutil.VectorAlgebra;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataPair;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.train.MLTrain;

import java.util.ArrayList;
import java.util.TreeMap;

public class AdaBoost extends Ensemble {

	private int T;
	private VectorAlgebra va;
	private ArrayList<Double> weights;
	
	public AdaBoost(int iterationsT, EnsembleMLMethodFactory mlFactory, EnsembleTrainFactory trainFactory, EnsembleAggregator aggregator) {
		this.T = iterationsT;
		this.mlFactory = mlFactory;
		this.va = new VectorAlgebra();
		this.weights = new ArrayList<Double>();
		this.members = new ArrayList<EnsembleML>();
		this.trainFactory = trainFactory;
	}
	
	public int train(double targetAccuracy, boolean verbose) {
		ArrayList<Double> D = new ArrayList<Double>();
		int dss = dataSetFactory.getInputData().size();
		for (int k = 0; k < dss; k++)
			D.add(1.0 / (float) dss);
		for (int i = 0; i < T; i++) {
			dataSetFactory.setSignificance(D);
			MLDataSet thisSet = dataSetFactory.getNewDataSet();
			AdaBoostML newML = new AdaBoostML(mlFactory.createML(dataSetFactory.getInputData().getInputSize(), dataSetFactory.getInputData().getIdealSize()));
			MLTrain train = trainFactory.getTraining(newML.getMl(), thisSet);
			newML.train(train,targetAccuracy,verbose);
			double newWeight = getWeightedError(newML,thisSet);
			members.add(newML);
			weights.add(newWeight);
			D = updateD(newML,thisSet,D);
		}
		return T;
	}

	private double epsilon(AdaBoostML ml, MLDataSet dataSet) {
		int bad = 0;
		for (MLDataPair data: dataSet) {
			if (ml.classify(data.getInput()) != ml.winner(data.getIdeal()))
				bad++;
		}
		return (float) bad / (float) dataSet.size();
	}
	
	private ArrayList<Double> updateD(AdaBoostML ml, MLDataSet dataSet, ArrayList<Double> D_t) {
		ArrayList<Double> D_tplus1 = new ArrayList<Double>();
		double epsilon = epsilon(ml, dataSet);
		double alpha_t = Math.log(1 - epsilon / epsilon);
		for (int i = 0; i < dataSet.size(); i++) {
			double D_tplus1_i = D_t.get(i) * Math.exp(-alpha_t * va.dotProduct(dataSet.get(i).getIdeal().getData(), ml.compute(dataSet.get(i).getInput()).getData()));
			D_tplus1.add(D_tplus1_i);
		}
		return D_tplus1;
	}
	
	@Override
	public void initMembers() {
		//This cannot do anything, as member generation is strictly linked to training!
	}
	
	private double getWeightedError(AdaBoostML newML, MLDataSet dataSet) {
		double sum = 0;
		for (int i = 0; i < dataSet.size(); i++) {
			MLDataPair currentData = dataSet.get(i);
			if (newML.classify(currentData.getInput()) != newML.winner(currentData.getIdeal()))
				sum += currentData.getSignificance();
		}
		return sum;
	}

	@Override
	public void addMember(EnsembleML newMember) throws NotPossibleInThisMethod {
		throw new NotPossibleInThisMethod();
	}

	@Override
	public ProblemType getProblemType() {
		return EnsembleTypes.ProblemType.CLASSIFICATION;
	}

}
