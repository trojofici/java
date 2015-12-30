package trojo.topcoder.randomforest;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import trojo.topcoder.randomforest.ForestRoot.ForestListener;
import trojo.topcoder.randomforest.ForestRoot.ForestSettings;
import trojo.topcoder.randomforest.ForestRoot.RandomForest;
import trojo.topcoder.randomforest.Overmind.ProblemEntry;

public class Overmind<X extends ProblemEntry> implements ForestListener {
	public static final int F_I = 2;
	Problem<X> problem;
	RandomForest forest;
	List<ProblemEntryData<X>> trainingEntries;
	List<ProblemEntryData<X>> testEntries;

	public Overmind(Problem<X> problem, List<X> trainingEntries, List<X> testEntries) {
		this.problem = problem;
		this.forest = new RandomForest(this);
		this.trainingEntries = this.extractData(trainingEntries);
		this.testEntries = this.extractData(testEntries);
	}

	public static abstract class Problem<X extends ProblemEntry> {
		public abstract ProblemEntryData<X> extractData(X entry);

		public abstract void completeData(X sourceEntry, ProblemEntryData<X> entryData);
	}

	public void completeEntries() {
		// TODO implement data completion
		
		
		
		
		
		for (ProblemEntryData<X> problemEntryData : trainingEntries) {
			problem.completeData(problemEntryData.source, problemEntryData);
		}
		for (ProblemEntryData<X> problemEntryData : testEntries) {
			problem.completeData(problemEntryData.source, problemEntryData);
		}

	}

	public void train() {
		int feaureCount = trainingEntries.get(0).features.size();
		double[][] trainData = new double[trainingEntries.size()][feaureCount + F_I];
		double entryId = 0.0f;
		for (int i = 0; i < trainData.length; i++) {
			ProblemEntryData<X> entryData = trainingEntries.get(i);
			for (int j = 0; j < feaureCount; j++) {
				//System.out.println("entryData.features.get(j).value:"+entryData.features.get(j).value);
				trainData[i][j + F_I] = entryData.features.get(j).value;
			}
			trainData[i][0] = entryId;
			trainData[i][1] = entryData.getOutput();
			//System.out.println(trainData[i][1]);
			entryId += 1.0f;
			//System.out.println("Train data:"+Arrays.toString(trainData[i]));
		}
		ForestSettings settings = new ForestSettings();
		this.forest.executeTraining(trainData, settings);

	}

	public void fillTestOutput() {
		int feaureCount = testEntries.get(0).features.size();
		double[] testData = new double[feaureCount + F_I];
		double entryId = 0.0f;
		for (int i = 0; i < testData.length; i++) {
			ProblemEntryData<X> entryData = testEntries.get(i);
			for (int j = 0; j < feaureCount; j++) {
				testData[j + F_I] = entryData.features.get(j).value;

			}
			testData[0] = entryId;
			//System.out.println("Test data:"+Arrays.toString(testData));
			double output = this.forest.predict(testData);
			entryData.setOutput(output);
			entryId += 1.0f;
		}
		// ForestSettings settings = new ForestSettings();
	}

	/*
	 * public void train(List<X> entries, int maxTrees, double cutoffError, int
	 * maxLevel, long maxTime, double usedEntriesPercentage, double
	 * usedFeaturesPercentage) { double[][] data = extractData(entries);
	 * this.forest.executeTraining(data, maxTrees, cutoffError, maxLevel,
	 * maxTime, usedEntriesPercentage, usedFeaturesPercentage);
	 * 
	 * }
	 */

	private List<ProblemEntryData<X>> extractData(List<X> entries) {
		List<ProblemEntryData<X>> toReturn = new ArrayList<ProblemEntryData<X>>(entries.size());
		for (X entry : entries) {
			toReturn.add(extractData(entry));
		}
		return toReturn;
	}

	private ProblemEntryData<X> extractData(X entry) {
		return problem.extractData(entry);
	}

	public static abstract class ProblemEntry {
		double id;
		ProblemEntryData entryData;
		double output;

		public double getOutput() {
			//System.out.println("getOutput():"+this+":"+output);
			return output;
		}

		public void setOutput(double output) {
			//System.out.println("setOutput0():"+this+":"+output);
			this.output = output;
		}

		public ProblemEntry(double id) {
			this.id = id;
		}
		public double getId() {
			return id;
		}
	}

	public static enum FeatureType {
		input, calculated
	}

	public static enum FeatureCompletionType {
		average, custom, regression
	}

	public static class ProblemEntryData<X extends ProblemEntry> {
		X source;
		private double id;
		public List<Feature> features = new LinkedList<Overmind.Feature>();
		private double output;

		public ProblemEntryData(X source) {
			this.id = source.id;
			this.source = source;
			this.source.entryData = this;
			this.output = source.output;
		}
		
		public double getId() {
			return id;
		}
		
		public double getOutput() {
			return output;
		}
		
		protected void setOutput(double output) {
			//System.out.println("setOutput():"+this+":"+output);
			this.output = output;
			source.setOutput(output);
		}
	}

	public static class Feature {
		FeatureType type;
		FeatureCompletionType completionType;
		String description;
		double value;

		public Feature(FeatureType type, String description, double value, FeatureCompletionType completionType) {
			this.type = type;
			this.description = description;
			this.value = value;
			this.completionType = completionType;
		}

		public Feature(String description, double value) {
			this.type = FeatureType.input;
			this.description = description;
			this.value = value;
			this.completionType = FeatureCompletionType.regression;
		}

	}

	@Override
	public void onTreeBuild(int scheduledCount, int trainedCount, long elapsedTime) {
		//System.out.println("A tree has been built scheduledCount:"+scheduledCount+",builCount:"+trainedCount+",elapsedTime:"+elapsedTime);
		if(trainedCount==1) System.out.println("A tree has been built scheduledCount:"+scheduledCount+",builCount:"+trainedCount+",elapsedTime:"+elapsedTime);
		if(trainedCount%10==0) System.out.println("A tree has been built scheduledCount:"+scheduledCount+",builCount:"+trainedCount+",elapsedTime:"+elapsedTime);
		
	}
}
