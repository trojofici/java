package trojo.topcoder.randomforest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

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
import trojo.topcoder.randomforest.Overmind.UsageFeature.FeatureDataType;
import trojo.topcoder.randomforest.Overmind.UsageFeature.FeatureUsageType;

public class Overmind<X extends ProblemEntry> implements ForestListener {
	public static final int F_I = 2;
	Problem<X, ? extends Object> problem;
	RandomForest forest;
	List<ProblemEntryData<X>> trainingEntries;
	List<ProblemEntryData<X>> testEntries;
	List<UsageFeature> usedFeatures = new LinkedList<UsageFeature>();

	public Overmind(Problem<X,?> problem, List<X> trainingEntries, List<X> testEntries) {
		this.problem = problem;
		this.forest = new RandomForest(this);
		this.trainingEntries = this.extractData(trainingEntries);
		this.testEntries = this.extractData(testEntries);
		List<AbstractFeature> fes = trainingEntries.get(0).entryData.features;
		for (AbstractFeature fe : fes) {
			usedFeatures.add(new UsageFeature(fe.description, fe.getUsageType(), fe.getDataType(), 0));
		}
	}

	public static class DoubleProblem extends EcmaProblem<DoubleEntry> {

		@Override
		public ProblemEntryData<DoubleEntry> extractData(DoubleEntry entry) {
			ProblemEntryData<DoubleEntry> toReturn = new ProblemEntryData<DoubleEntry>(entry);
			for (int i = 0; i < entry.vals.length; i++) {
				toReturn.features.add(new InputFeature("i" + i, entry.vals[i], FeatureDataType.DOUBLE));
			}
			return toReturn;
		}
	}
	
	public static class EcmaCompletionData {
		//ScriptEngine scriptEngine;
		Bindings bindings;
		protected Map<String, CompiledScript> ecmaFeatureScripts = new ConcurrentHashMap<String, CompiledScript>();
		protected Map<String, FeatureDataType> ecmaFeatureDataTypes = new ConcurrentHashMap<String, FeatureDataType>();
		
		public void initThreadStrucues() {
			ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("ecmascript");
			this.bindings = scriptEngine.createBindings();
		}
		
	}

	public abstract static class EcmaProblem<X extends ProblemEntry> extends Problem<X,EcmaCompletionData> {
		EcmaCompletionData rootCompletionData = new EcmaCompletionData();
		
		@Override
		public void completeDataStart(List<UsageFeature> usedFeatures) {
			super.completeDataStart(usedFeatures);
			prepareEcmaOperations(usedFeatures);
		}
		
		@Override
		public void completeDataEnd(List<UsageFeature> usedFeatures) {
			super.completeDataEnd(usedFeatures);
		}
		
		protected static class BaseDescription {
			String description;
			FeatureDataType dataType;
			public BaseDescription(String description, FeatureDataType dataType) {
				this.description = description;
				this.dataType = dataType;
			}
		}
		
		@Override
		public EcmaCompletionData completeDataThreadStart() {
			EcmaCompletionData toReturn = new EcmaCompletionData();
			toReturn.initThreadStrucues();
			toReturn.ecmaFeatureDataTypes = this.rootCompletionData.ecmaFeatureDataTypes;
			toReturn.ecmaFeatureScripts = this.rootCompletionData.ecmaFeatureScripts;
			return toReturn;
		}
		
		protected void prepareEcmaOperations(List<UsageFeature> usedFeatures) {
			double minUsage = 0.02;
			ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("ecmascript");
			this.rootCompletionData.ecmaFeatureScripts.clear();
			this.rootCompletionData.ecmaFeatureDataTypes.clear();
			List<BaseDescription> baseDescriptions = new ArrayList<BaseDescription>();
			for (UsageFeature fe : usedFeatures) {
				if(fe.featureType==FeatureUsageType.INPUT) {
					baseDescriptions.add(new BaseDescription(fe.description, fe.dataType));
				} else if(fe.usedRatio>minUsage) {
					baseDescriptions.add(new BaseDescription(fe.description, fe.dataType));
				}
			}
			
			for (int i = 0; i < baseDescriptions.size(); i++) {
				BaseDescription d1 = baseDescriptions.get(i);
				for (int j = i+1; j < baseDescriptions.size(); j++) {
					BaseDescription d2 = baseDescriptions.get(j);
					try {
						if(d1.dataType!=d2.dataType) continue;
						if(d1.dataType==FeatureDataType.DOUBLE) {
							//System.out.println("prepareEcmaOperations"+d1.description+":"+d2.description);
							String newKey = "("+d1.description+")+("+d2.description+")";
							CompiledScript compiledScript = ((Compilable) scriptEngine).compile(newKey);
							this.rootCompletionData.ecmaFeatureScripts.put(newKey, compiledScript);
							this.rootCompletionData.ecmaFeatureDataTypes.put(newKey, d1.dataType);
							newKey = "("+d1.description+")-("+d2.description+")";
							compiledScript = ((Compilable) scriptEngine).compile(newKey);
							this.rootCompletionData.ecmaFeatureScripts.put(newKey, compiledScript);
							this.rootCompletionData.ecmaFeatureDataTypes.put(newKey, d1.dataType);
							newKey = "("+d1.description+")*("+d2.description+")";
							compiledScript = ((Compilable) scriptEngine).compile(newKey);
							this.rootCompletionData.ecmaFeatureScripts.put(newKey, compiledScript);
							this.rootCompletionData.ecmaFeatureDataTypes.put(newKey, d1.dataType);
							newKey = "("+d1.description+")/("+d2.description+")";
							compiledScript = ((Compilable) scriptEngine).compile(newKey);
							this.rootCompletionData.ecmaFeatureScripts.put(newKey, compiledScript);
							this.rootCompletionData.ecmaFeatureDataTypes.put(newKey, d1.dataType);
						}
					} catch (ScriptException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		@Override
		public void completeData(ProblemEntryData<X> entryData, EcmaCompletionData problemThreadsafeData) {
			List<AbstractFeature> features0 = entryData.source.entryData.features;
			List<AbstractFeature> ecmaFeatures = new LinkedList<AbstractFeature>();
			Map<String, EcmaFeature> ecmaCache = new HashMap<String, EcmaFeature>(); 
			for (int i = 0; i < features0.size(); i++) {
				AbstractFeature f0 = features0.get(i);
				if (f0 instanceof EcmaFeature) {
					ecmaCache.put(f0.description, (EcmaFeature)f0);
				} else {
					problemThreadsafeData.bindings.put(f0.description, f0.value);
				}
			}
			
			for (String desc : problemThreadsafeData.ecmaFeatureScripts.keySet()) {
				CompiledScript script = problemThreadsafeData.ecmaFeatureScripts.get(desc);
				FeatureDataType type = problemThreadsafeData.ecmaFeatureDataTypes.get(desc);
				if(ecmaCache.containsKey(desc)) {
					//System.out.println("Using cache");
					EcmaFeature toAdd = ecmaCache.get(desc);
					ecmaFeatures.add(toAdd);
				} else {
					try {
						Object result0 = script.eval(problemThreadsafeData.bindings);
						Double result = (Double)result0;
						//if(result.isNaN()) {
							//System.out.println("Lolo");
						//	result = Double.MIN_VALUE/2;
						//}
						EcmaFeature toAdd = new EcmaFeature(desc, result, type);
						ecmaFeatures.add(toAdd);
					} catch (ScriptException e) {
						e.printStackTrace();
					}
				}
			}
			features0.addAll(ecmaFeatures);
		}
	}

	public static abstract class Problem<X extends ProblemEntry, Y> {
		public abstract ProblemEntryData<X> extractData(X entry) throws Exception;
		public void completeDataStart(List<UsageFeature> usedFeatures) {};
		public abstract Y completeDataThreadStart();
		public abstract void completeData(ProblemEntryData<X> entryData, Y problemThreadsafeData);
		public void completeDataEnd(List<UsageFeature> usedFeatures){};
		
		protected class CompleteEntriesThread extends Thread {
			Y problemThreadsafeData;
			int modulo;
			int threadCnt;
			List<ProblemEntryData<X>> list;
			CountDownLatch latch;
			public CompleteEntriesThread(List<ProblemEntryData<X>> list, Y problemThreadsafeData, int modulo, int threadCnt, CountDownLatch latch) {
				this.modulo = modulo;
				this.threadCnt = threadCnt;
				this.list = list;
				this.latch = latch;
				this.problemThreadsafeData = problemThreadsafeData;
			}
			@Override
			public void run() {
				System.out.println("Started thread:"+this.modulo);
				for (int i = modulo; i < list.size(); i+=threadCnt) {
					ProblemEntryData<X> element = list.get(i);
					Problem.this.completeData(element, this.problemThreadsafeData);
					int cmp = completedCount.incrementAndGet();
				}
				latch.countDown();
				System.out.println("Completed thread:"+this.modulo);
			}
			
		}
		
		AtomicInteger completedCount = new AtomicInteger();
		public void completeEntries(List<ProblemEntryData<X>> entries, List<UsageFeature> usedFeatures) {
			completedCount.set(0);
			completeDataStart(usedFeatures);
			System.out.println("completeEntries start");
			ForestSettings settings = new ForestSettings();
			int threadCnt = settings.maxNumberOfRunners;
			//threadCnt = 4;
			CountDownLatch latch = new CountDownLatch(threadCnt);
			for (int i = 0; i < threadCnt; i++) {
				Y threadData = completeDataThreadStart();
				CompleteEntriesThread th = new CompleteEntriesThread(entries
						, threadData,  i, threadCnt, latch);
				th.start();
			} 
			try {
				latch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			
			/*int completionStep = trainingEntries.size()/20;
			int i = 0;
			for (ProblemEntryData<X> problemEntryData : trainingEntries) {
				problem.completeData(problemEntryData.source, problemEntryData, usedFeatures);
				i++;
				if((i%completionStep)==0) {
					System.out.println("Completed "+(i*100)/trainingEntries.size()+"%");
				}
			}*/
			/*
			System.out.println("completeEntries test start");
			Y threadData = completeDataThreadStart();
			for (ProblemEntryData<X> problemEntryData : testEntries) {
				completeData(problemEntryData, threadData);
			}
			problem.completeDataEnd(usedFeatures);
			System.out.println("completeEntries end");*/

		}
	}
	
	/*protected class CompleteEntriesThread extends Thread {
		Y problemThreadsafeData;
		int modulo;
		int threadCnt;
		List<ProblemEntryData<X>> list;
		CountDownLatch latch;
		public CompleteEntriesThread(List<ProblemEntryData<X>> list, Y problemThreadsafeData, int modulo, int threadCnt, CountDownLatch latch) {
			this.modulo = modulo;
			this.threadCnt = threadCnt;
			this.list = list;
			this.latch = latch;
			this.problemThreadsafeData = problemThreadsafeData;
		}
		@Override
		public void run() {
			System.out.println("Started thread:"+this.modulo);
			for (int i = modulo; i < list.size(); i+=threadCnt) {
				ProblemEntryData<X> element = list.get(i);
				problem.completeData(element, this.problemThreadsafeData, usedFeatures);
				int cmp = completedCount.incrementAndGet();
			}
			latch.countDown();
			System.out.println("Completed thread:"+this.modulo);
		}
		
	}
	AtomicInteger completedCount = new AtomicInteger();
	public void completeEntries() {
		completedCount.set(0);
		problem.completeDataStart(usedFeatures);
		System.out.println("completeEntries start");
		ForestSettings settings = new ForestSettings();
		int threadCnt = settings.maxNumberOfRunners;
		threadCnt = 1;
		CountDownLatch latch = new CountDownLatch(threadCnt);
		for (int i = 0; i < threadCnt; i++) {
			Y threadData = problem.completeDataThreadStart();
			CompleteEntriesThread th = new CompleteEntriesThread(trainingEntries, threadData,  i, threadCnt, latch);
			th.start();
		} 
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		
		/*int completionStep = trainingEntries.size()/20;
		int i = 0;
		for (ProblemEntryData<X> problemEntryData : trainingEntries) {
			problem.completeData(problemEntryData.source, problemEntryData, usedFeatures);
			i++;
			if((i%completionStep)==0) {
				System.out.println("Completed "+(i*100)/trainingEntries.size()+"%");
			}
		}*/
		/*
		System.out.println("completeEntries test start");
		Y threadData = problem.completeDataThreadStart();
		for (ProblemEntryData<X> problemEntryData : testEntries) {
			problem.completeData(problemEntryData, threadData, usedFeatures);
		}
		problem.completeDataEnd(usedFeatures);
		System.out.println("completeEntries end");

	}*/
	
	public void completeEntries() {
		problem.completeDataStart(usedFeatures);
		System.out.println("completeEntries start");
		problem.completeEntries(this.trainingEntries, usedFeatures);
		problem.completeEntries(this.testEntries, usedFeatures);
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
			this.usedFeatures.add(new UsageFeature(fe.description, fe.getUsageType(), fe.getDataType(), tInfo.usedFeatures[i]));
			usedSum+=tInfo.usedFeatures[i];
		}
		
		for (UsageFeature uf : usedFeatures) {
			uf.usedRatio = (double)uf.usedCount/usedSum;
		}
	}
	
	public void printUsedFeatures() {
		Collections.sort(this.usedFeatures);
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
		List<ProblemEntryData<X>> toReturn = new CopyOnWriteArrayList<ProblemEntryData<X>>();
		for (X entry : entries) {
			toReturn.add(extractData(entry));
		}
		return toReturn;
	}

	private ProblemEntryData<X> extractData(X entry) {
		try {
			return problem.extractData(entry);
		} catch (Throwable th) {
			throw new RuntimeException("Failder extract id:"+entry.getId(),th);
		}
		
		
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
		FeatureDataType dataType;
		public EcmaFeature(String description, double value, FeatureDataType dataType) {
			super(description, value);
			this.dataType = dataType;
		}

		@Override
		public FeatureUsageType getUsageType() {
			return FeatureUsageType.ECMA;
		}
		
		@Override
		public FeatureDataType getDataType() {
			return this.dataType;
		}
	}

	public static class InputFeature extends AbstractFeature {
		FeatureCompletionType completionType;
		FeatureDataType dataType;

		public InputFeature(String description, double value, FeatureDataType dataType, FeatureCompletionType completionType) {
			super(description, value);
			this.completionType = completionType;
			this.dataType = dataType;
		}

		public InputFeature(String description, double value, FeatureDataType dataType) {
			this(description, value, dataType, FeatureCompletionType.regression);
		}
		
		@Override
		public FeatureUsageType getUsageType() {
			return FeatureUsageType.INPUT;
		}
		
		@Override
		public FeatureDataType getDataType() {
			return this.dataType;
		}

	}
	
	
	
	public static class UsageFeature implements Comparable<UsageFeature>{
		public static enum FeatureUsageType {
			INPUT, ECMA;
		}
		public static enum FeatureDataType {
			DOUBLE, BOOLEAN;
		}
		
		String description;
		FeatureUsageType featureType; 
		FeatureDataType dataType;
		int usedCount;
		double usedRatio;
		public UsageFeature(String description, FeatureUsageType type, FeatureDataType dataType, int usedCount) {
			this.description = description;
			this.usedCount = usedCount;
			this.featureType = type;
			this.dataType = dataType;
		}
		@Override
		public int compareTo(UsageFeature o) {
			return -Integer.compare(this.usedCount, o.usedCount);
		}
	}

	public abstract static class AbstractFeature {
		String description;
		double value;
		public AbstractFeature(String description, double value) {
			this.description = description;
			this.value = value;
		}
		
		public abstract FeatureUsageType getUsageType();
		public abstract FeatureDataType getDataType();

	}

	@Override
	public void onTreeBuild(int scheduledCount, int trainedCount, long elapsedTime) {
		// System.out.println("A tree has been built
		// scheduledCount:"+scheduledCount+",builCount:"+trainedCount+",elapsedTime:"+elapsedTime);
		// if(trainedCount==1) System.out.println("A tree has been built
		// scheduledCount:"+scheduledCount+",builCount:"+trainedCount+",elapsedTime:"+elapsedTime);
		if (trainedCount % 1 == 0) {
			System.out.println("A tree has been built scheduledCount:" + scheduledCount + ",builCount:" + trainedCount
					+ ",elapsedTime:" + elapsedTime);
		}

	}
}
