package trojo.topcoder.randomforest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import trojo.topcoder.randomforest.ForestRoot.ForestInfo;
import trojo.topcoder.randomforest.ForestRoot.ForestListener;
import trojo.topcoder.randomforest.ForestRoot.ForestSettings;
import trojo.topcoder.randomforest.ForestRoot.RandomForest;
import trojo.topcoder.randomforest.ForestRoot.TreesInfo;
import trojo.topcoder.randomforest.Overmind.ProblemEntry;
import trojo.topcoder.randomforest.Overmind.UsageFeature.UsageFeatureType;

public class Overmind<X extends ProblemEntry> implements ForestListener {
	public static final int F_I = 2;
	Problem<X> problem;
	RandomForest forest;
	List<ProblemEntryData<X>> trainingEntries;
	List<ProblemEntryData<X>> testEntries;
	List<UsageFeature> usedFeatures = new LinkedList<UsageFeature>();

	public Overmind(Problem<X> problem, List<X> trainingEntries, List<X> testEntries) {
		this.problem = problem;
		this.forest = new RandomForest(this);
		this.trainingEntries = this.extractData(trainingEntries);
		this.testEntries = this.extractData(testEntries);
		List<AbstractFeature> fes = trainingEntries.get(0).entryData.features;
		for (AbstractFeature fe : fes) {
			usedFeatures.add(new UsageFeature(fe.description, UsageFeatureType.INPUT, 0));
		}
	}

	public static class DoubleProblem extends EcmaProblem<DoubleEntry> {

		@Override
		public ProblemEntryData<DoubleEntry> extractData(DoubleEntry entry) {
			ProblemEntryData<DoubleEntry> toReturn = new ProblemEntryData<DoubleEntry>(entry);
			for (int i = 0; i < entry.vals.length; i++) {
				toReturn.features.add(new InputFeature("i" + i, entry.vals[i]));
			}
			return toReturn;
		}
	}

	public abstract static class EcmaProblem<X extends ProblemEntry> extends Problem<X> {
		ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("ecmascript");
		Bindings bindings = scriptEngine.createBindings();
		protected Map<String, CompiledScript> ecmaFeatureScripts = new ConcurrentHashMap<String, CompiledScript>();
		
		@Override
		public void completeDataStart(List<UsageFeature> usedFeatures) {
			super.completeDataStart(usedFeatures);
			prepareEcmaOperations(usedFeatures);
		}
		
		@Override
		public void completeDataEnd(List<UsageFeature> usedFeatures) {
			super.completeDataEnd(usedFeatures);
		}
		
		protected void prepareEcmaOperations(List<UsageFeature> usedFeatures) {
			double minUsage = 0.1d;
			ecmaFeatureScripts.clear();
			List<String> baseDescriptions = new LinkedList<String>();
			for (UsageFeature fe : usedFeatures) {
				if(fe.featureType==UsageFeatureType.INPUT) {
					baseDescriptions.add(fe.description);
				} else if(fe.usedRatio>minUsage){
					baseDescriptions.add(fe.description);
				}
			}
			
			for (int i = 0; i < baseDescriptions.size(); i++) {
				String d1 = baseDescriptions.get(i);
				for (int j = i+1; j < baseDescriptions.size(); j++) {
					String d2 = baseDescriptions.get(j);
					try {
						String newKey = "("+d1+")+("+d2+")";
						CompiledScript compiledScript = ((Compilable) scriptEngine).compile(newKey);
						ecmaFeatureScripts.put(newKey, compiledScript);
						newKey = "("+d1+")*("+d2+")";
						compiledScript = ((Compilable) scriptEngine).compile(newKey);
						ecmaFeatureScripts.put(newKey, compiledScript);
						//newKey = "("+d1+")/("+d2+")";
						//compiledScript = ((Compilable) scriptEngine).compile(newKey);
						//ecmaFeatureScripts.put(newKey, compiledScript);
					} catch (ScriptException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		@Override
		public void completeData(X sourceEntry, ProblemEntryData<X> entryData, List<UsageFeature> usedFeatures) {
			List<AbstractFeature> features0 = sourceEntry.entryData.features;
			List<AbstractFeature> ecmaFeatures = new LinkedList<AbstractFeature>();
			for (int i = 0; i < features0.size(); i++) {
				AbstractFeature f0 = features0.get(i);
				if (!(f0 instanceof EcmaFeature)) {
					bindings.put(f0.description, f0.value);
				}
			}
			
			for (String desc : ecmaFeatureScripts.keySet()) {
				CompiledScript script = ecmaFeatureScripts.get(desc);
				try {
					Object result0 = script.eval(bindings);
					Double result = (Double)result0;
					if(result.isNaN()) {
						//System.out.println("Lolo");
						result = Double.MIN_VALUE/2;
					}
					EcmaFeature toAdd = new EcmaFeature(desc, result);
					ecmaFeatures.add(toAdd);
				} catch (ScriptException e) {
					e.printStackTrace();
				}
			}
			features0.addAll(ecmaFeatures);
		}
	}

	public static abstract class Problem<X extends ProblemEntry> {
		public abstract ProblemEntryData<X> extractData(X entry);
		public void completeDataStart(List<UsageFeature> usedFeatures) {};
		public abstract void completeData(X sourceEntry, ProblemEntryData<X> entryData, List<UsageFeature> usedFeatures);
		public void completeDataEnd(List<UsageFeature> usedFeatures){};
	}

	public void completeEntries() {
		problem.completeDataStart(usedFeatures);
		System.out.println("completeEntries start");
		for (ProblemEntryData<X> problemEntryData : trainingEntries) {
			problem.completeData(problemEntryData.source, problemEntryData, usedFeatures);
		}
		System.out.println("completeEntries test start");
		for (ProblemEntryData<X> problemEntryData : testEntries) {
			problem.completeData(problemEntryData.source, problemEntryData, usedFeatures);
		}
		problem.completeDataEnd(usedFeatures);
		System.out.println("completeEntries end");

	}

	public void train() {
		int feaureCount = trainingEntries.get(0).features.size();
		double[][] trainData = new double[trainingEntries.size()][feaureCount + F_I];
		double entryId = 0.0f;
		for (int i = 0; i < trainData.length; i++) {
			ProblemEntryData<X> entryData = trainingEntries.get(i);
			for (int j = 0; j < feaureCount; j++) {
				// System.out.println("entryData.features.get(j).value:"+entryData.features.get(j).value);
				trainData[i][j + F_I] = entryData.features.get(j).value;
			}
			trainData[i][0] = entryId;
			trainData[i][1] = entryData.getOutput();
			// System.out.println(trainData[i][1]);
			entryId += 1.0f;
			// System.out.println("Train data:"+Arrays.toString(trainData[i]));
		}
		ForestSettings settings = new ForestSettings();
		this.forest.executeTraining(trainData, settings);
		fillFeatureUsageInfo();
	}
	
	protected void fillFeatureUsageInfo() {
		this.usedFeatures.clear();
		ForestInfo info = this.forest.getForestInfo();
		TreesInfo tInfo = info.epochInfo.get(info.epochInfo.size() - 1);
		List<AbstractFeature> features = this.trainingEntries.get(0).features;
		double usedSum = 0.0d;
		for (int i = F_I; i < tInfo.usedFeatures.length; i++) {
			AbstractFeature fe = features.get(i-F_I);
			this.usedFeatures.add(new UsageFeature(fe.description, fe.getUsageType(), tInfo.usedFeatures[i]));
			usedSum+=tInfo.usedFeatures[i];
		}
		
		for (UsageFeature uf : usedFeatures) {
			uf.usedRatio = (double)uf.usedCount/usedSum;
		}
	}
	
	public void printUsedFeatures() {
		for (UsageFeature f : this.usedFeatures) {
			if(f.usedCount>0) {
				System.out.println(f.description + "\t\t:" + f.usedCount+"\t\t:"+f.usedRatio);
			}
			
		}
	}
	
	public void fillTestOutput() {
		int feaureCount = testEntries.get(0).features.size();
		double[] testData = new double[feaureCount + F_I];
		double entryId = 0.0f;
		for (int i = 0; i < testEntries.size(); i++) {
			ProblemEntryData<X> entryData = testEntries.get(i);
			for (int j = 0; j < feaureCount; j++) {
				testData[j + F_I] = entryData.features.get(j).value;

			}
			testData[0] = entryId;
			// System.out.println("Test data:"+Arrays.toString(testData));
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

	public static class DoubleEntry extends ProblemEntry {
		double[] vals;

		public DoubleEntry(double id, double[] vals, double output) {
			super(id);
			this.vals = vals;
			this.setOutput(output);
		}

	}

	public static abstract class ProblemEntry {
		double id;
		ProblemEntryData entryData;
		double output;

		public double getOutput() {
			// System.out.println("getOutput():"+this+":"+output);
			return output;
		}

		public void setOutput(double output) {
			// System.out.println("setOutput0():"+this+":"+output);
			this.output = output;
		}

		public ProblemEntry(double id) {
			this.id = id;
		}

		public double getId() {
			return id;
		}
	}

	public static enum FeatureCompletionType {
		average, custom, regression
	}

	public static class ProblemEntryData<X extends ProblemEntry> {
		X source;
		private double id;
		public List<AbstractFeature> features = new LinkedList<AbstractFeature>();
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
			// System.out.println("setOutput():"+this+":"+output);
			this.output = output;
			source.setOutput(output);
		}
	}

	public static class EcmaFeature extends AbstractFeature {
		public EcmaFeature(String description, double value) {
			super(description, value);
		}

		@Override
		public UsageFeatureType getUsageType() {
			return UsageFeatureType.ECMA;
		}
	}

	public static class InputFeature extends AbstractFeature {
		FeatureCompletionType completionType;

		public InputFeature(String description, double value, FeatureCompletionType completionType) {
			super(description, value);
			this.completionType = completionType;
		}

		public InputFeature(String description, double value) {
			this(description, value, FeatureCompletionType.regression);
		}
		
		@Override
		public UsageFeatureType getUsageType() {
			return UsageFeatureType.INPUT;
		}

	}
	
	
	
	public static class UsageFeature {
		public static enum UsageFeatureType {
			INPUT, ECMA;
		}
		String description;
		UsageFeatureType featureType; 
		int usedCount;
		double usedRatio;
		public UsageFeature(String description, UsageFeatureType type, int usedCount) {
			this.description = description;
			this.usedCount = usedCount;
			this.featureType = type;
		}
	}

	public abstract static class AbstractFeature {
		String description;
		double value;
		public AbstractFeature(String description, double value) {
			this.description = description;
			this.value = value;
		}
		
		public abstract UsageFeatureType getUsageType();

	}

	@Override
	public void onTreeBuild(int scheduledCount, int trainedCount, long elapsedTime) {
		// System.out.println("A tree has been built
		// scheduledCount:"+scheduledCount+",builCount:"+trainedCount+",elapsedTime:"+elapsedTime);
		// if(trainedCount==1) System.out.println("A tree has been built
		// scheduledCount:"+scheduledCount+",builCount:"+trainedCount+",elapsedTime:"+elapsedTime);
		if (trainedCount % 10 == 0) {
			System.out.println("A tree has been built scheduledCount:" + scheduledCount + ",builCount:" + trainedCount
					+ ",elapsedTime:" + elapsedTime);
		}

	}
}
