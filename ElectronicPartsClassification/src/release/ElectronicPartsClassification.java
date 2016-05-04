package release;


import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import release.ElectronicPartsClassification.ForestRoot.ForestInfo;
import release.ElectronicPartsClassification.ForestRoot.ForestListener;
import release.ElectronicPartsClassification.ForestRoot.ForestSettings;
import release.ElectronicPartsClassification.ForestRoot.RandomForest;
import release.ElectronicPartsClassification.ForestRoot.TreesInfo;
import release.ElectronicPartsClassification.Overmind.EcmaProblem;
import release.ElectronicPartsClassification.Overmind.InputFeature;
import release.ElectronicPartsClassification.Overmind.ProblemEntry;
import release.ElectronicPartsClassification.Overmind.ProblemEntryData;
import release.ElectronicPartsClassification.Overmind.UsageFeature.FeatureDataType;
import release.ElectronicPartsClassification.Overmind.UsageFeature.FeatureUsageType;

public class ElectronicPartsClassification {
	public static class ELEEntry extends ProblemEntry {
		String inputLine;
		public ELEEntry(double id, String inputLine) {
			super(id);
			this.inputLine = inputLine;
			//this.setOutput(output);
		}
		
	}
	
	public static class ELEProblem extends EcmaProblem<ELEEntry> {
		//DateFormat df = new SimpleDateFormat("E MM dd kk:mm:ss z yyyy");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
		
		protected List<InputFeature> parseType(String id, String value, String[] options) {
			List<InputFeature> toReturn = new LinkedList<InputFeature>();
			boolean legalValue = false;
			for (int i = 0; i < options.length; i++) {
				boolean eq = options[i].equals(value);
				double val = (eq?1d:0d);
				legalValue = legalValue || eq;
				InputFeature toAdd = new InputFeature(id+"_"+options[i],val, FeatureDataType.BOOLEAN);
				toReturn.add(toAdd);
			}
			if(!legalValue) throw new IllegalArgumentException(id+"has wrong value:"+value+", legal options"+Arrays.toString(options));
			return toReturn;
		}
		
		protected List<InputFeature> parseType(String id, String value, int low, int high) {
			List<InputFeature> toReturn = new LinkedList<InputFeature>();
			int intVal = Integer.parseInt(value);
			
			if(intVal<low || intVal>high) {
				throw new IllegalArgumentException(id+"has wrong value:"+value+", legal options between <"+low+","+high+">");
			}
		
			for (int i = low; i <= high; i++) {
				boolean eq = (i==intVal);
				double val = (eq?1d:0d);
				InputFeature toAdd = new InputFeature(id+"_"+i,val, FeatureDataType.BOOLEAN);
				toReturn.add(toAdd);
			}
			return toReturn;
		}
		
		@Override
		public ProblemEntryData<ELEEntry> extractData(ELEEntry entry) throws ParseException {
			String[] inputs = entry.inputLine.split(",");
			//System.out.println(inputs[28]);
			String outputS = inputs[28];
			double output = 0d;
			if(outputS.equals("No")  ) {
				output = 0d;
			} else if(outputS.equals("Maybe")  ) {
				output = 1d;
			} else if(outputS.equals("Yes")  ) {
				output = 2d;
			} else throw new IllegalArgumentException("Wrong SPECIAL_PART:"+outputS);
			entry.setOutput(output);
			ProblemEntryData<ELEEntry> toReturn = new ProblemEntryData<ELEEntry>(entry);
			InputFeature toAdd = new InputFeature("PRODUCT_NUMBER", Double.parseDouble(inputs[0]), FeatureDataType.DOUBLE);
			toReturn.features.add(toAdd);
			
			toAdd = new InputFeature("CUSTOMER_NUMBER", Double.parseDouble(inputs[1]), FeatureDataType.DOUBLE);
			toReturn.features.add(toAdd);
			
			toAdd = new InputFeature("TRANSACTION_DATE",df.parse(inputs[2]).getTime(), FeatureDataType.DOUBLE);
			toReturn.features.add(toAdd);
			
			toAdd = new InputFeature("PRODUCT_PRICE", Double.parseDouble(inputs[3]), FeatureDataType.DOUBLE);
			toReturn.features.add(toAdd);
			
			toAdd = new InputFeature("GROSS_SALES", Double.parseDouble(inputs[4]), FeatureDataType.DOUBLE);
			toReturn.features.add(toAdd);
			
			toAdd = new InputFeature("CUSTOMER_ZIP", Double.parseDouble(inputs[7]), FeatureDataType.DOUBLE);
			toReturn.features.add(toAdd);
			
			toReturn.features.addAll(parseType("CUSTOMER_SEGMENT1",inputs[8],new String[]{"A", "B"}));
			toReturn.features.addAll(parseType("CUSTOMER_TYPE1",inputs[10],new String[]{"1", "2", "3"}));
			toReturn.features.addAll(parseType("CUSTOMER_TYPE2",inputs[11],new String[]{"A", "B", "C"}));
			toReturn.features.addAll(parseType("CUSTOMER_ACCOUNT_TYPE",inputs[13],new String[]{"ST", "DM"}));
			
			toAdd = new InputFeature("CUSTOMER_FIRST_ORDER_DATE",df.parse(inputs[14]).getTime(), FeatureDataType.DOUBLE);
			toReturn.features.add(toAdd);
			
			toReturn.features.addAll(parseType("PRODUCT_CLASS_ID1",inputs[15],1,12));
			toReturn.features.addAll(parseType("PRODUCT_CLASS_ID2",inputs[16],15,31));
			
			toAdd = new InputFeature("PRODUCT_CLASS_ID3", Double.parseDouble(inputs[17]), FeatureDataType.DOUBLE);
			toReturn.features.add(toAdd);
			
			toAdd = new InputFeature("PRODUCT_CLASS_ID4", Double.parseDouble(inputs[18]), FeatureDataType.DOUBLE);
			toReturn.features.add(toAdd);
			
			toReturn.features.addAll(parseType("BRAND",inputs[19],new String[]{"IN_HOUSE", "NOT_IN_HOUSE"}));
			
			toAdd = new InputFeature("PRODUCT_ATTRIBUTE_X", Double.parseDouble(inputs[20]), FeatureDataType.DOUBLE);
			toReturn.features.add(toAdd);
			
			toReturn.features.addAll(parseType("PRODUCT_SALES_UNIT",inputs[21],new String[]{"N", "Y"}));
			
			toAdd = new InputFeature("SHIPPING_WEIGHT", Double.parseDouble(inputs[22]), FeatureDataType.DOUBLE);
			toReturn.features.add(toAdd);
			
			toAdd = new InputFeature("TOTAL_BOXES_SOLD", Double.parseDouble(inputs[23]), FeatureDataType.DOUBLE);
			toReturn.features.add(toAdd);
			
			toAdd = new InputFeature("PRODUCT_COST1", Double.parseDouble(inputs[24]), FeatureDataType.DOUBLE);
			toReturn.features.add(toAdd);
			
			toReturn.features.addAll(parseType("PRODUCT_UNIT_OF_MEASURE",inputs[25],new String[]{"B", "EA", "LB"}));
			toReturn.features.addAll(parseType("ORDER_SOURCE",inputs[26],new String[]{"A", "B"}));
			toReturn.features.addAll(parseType("PRICE_METHOD",inputs[27],1,5));
			return toReturn;
		}
		
	}
	
	public String[] classifyParts(String[] trainingData, String[] testingData) {
		String[] toReturn = new String[0];
		ELEProblem eleProblem = new ELEProblem();
		List<ELEEntry> train = new ArrayList<ELEEntry>();
		List<ELEEntry> test = new ArrayList<ELEEntry>();
		for (int i = 0; i < trainingData.length; i++) {
			ELEEntry toAdd = new ELEEntry(i, trainingData[i]);
			train.add(toAdd);
		}
		for (int i = 0; i < testingData.length; i++) {
			ELEEntry toAdd = new ELEEntry(i, trainingData[i]);
			test.add(toAdd);
		}
		
		
		Overmind<ELEEntry> over = new Overmind<ELEEntry>(eleProblem, train, test);
		for (int i = 0; i < 4; i++) {
			over.completeEntries();
			over.train();
			/*over.fillTestOutput();
			for (Iterator<ELEEntry> iterator = test.iterator(); iterator.hasNext();) {
				ELEEntry problemEntryData = iterator.next();
				System.out.println("Test output:"+problemEntryData.getOutput());
			}*/
			over.printUsedFeatures();
			
		}
						
		
		
		
		
		
		return toReturn;
	}




public static class Overmind<X extends ProblemEntry> implements ForestListener {
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

	public abstract static class EcmaProblem<X extends ProblemEntry> extends Problem<X> {
		ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("ecmascript");
		Bindings bindings = scriptEngine.createBindings();
		protected Map<String, CompiledScript> ecmaFeatureScripts = new ConcurrentHashMap<String, CompiledScript>();
		protected Map<String, FeatureDataType> ecmaFeatureDataTypes = new ConcurrentHashMap<String, FeatureDataType>();
		
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
		
		protected void prepareEcmaOperations(List<UsageFeature> usedFeatures) {
			double minUsage = 0.01;
			ecmaFeatureScripts.clear();
			ecmaFeatureDataTypes.clear();
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
							String newKey = "("+d1.description+")+("+d2.description+")";
							CompiledScript compiledScript = ((Compilable) scriptEngine).compile(newKey);
							ecmaFeatureScripts.put(newKey, compiledScript);
							ecmaFeatureDataTypes.put(newKey, d1.dataType);
							newKey = "("+d1.description+")*("+d2.description+")";
							compiledScript = ((Compilable) scriptEngine).compile(newKey);
							//ecmaFeatureScripts.put(newKey, compiledScript);
							ecmaFeatureDataTypes.put(newKey, d1.dataType);
							//newKey = "("+d1.description+")/("+d2.description+")";
							//compiledScript = ((Compilable) scriptEngine).compile(newKey);
							//ecmaFeatureScripts.put(newKey, compiledScript);
						}
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
				FeatureDataType type = ecmaFeatureDataTypes.get(desc);
				try {
					Object result0 = script.eval(bindings);
					Double result = (Double)result0;
					if(result.isNaN()) {
						//System.out.println("Lolo");
						result = Double.MIN_VALUE/2;
					}
					EcmaFeature toAdd = new EcmaFeature(desc, result, type);
					ecmaFeatures.add(toAdd);
				} catch (ScriptException e) {
					e.printStackTrace();
				}
			}
			features0.addAll(ecmaFeatures);
		}
	}

	public static abstract class Problem<X extends ProblemEntry> {
		public abstract ProblemEntryData<X> extractData(X entry) throws Exception;
		public void completeDataStart(List<UsageFeature> usedFeatures) {};
		public abstract void completeData(X sourceEntry, ProblemEntryData<X> entryData, List<UsageFeature> usedFeatures);
		public void completeDataEnd(List<UsageFeature> usedFeatures){};
	}

	public void completeEntries() {
		problem.completeDataStart(usedFeatures);
		System.out.println("completeEntries start");
		int completionStep = trainingEntries.size()/20;
		int i = 0;
		for (ProblemEntryData<X> problemEntryData : trainingEntries) {
			problem.completeData(problemEntryData.source, problemEntryData, usedFeatures);
			i++;
			if((i%completionStep)==0) {
				System.out.println("Completed "+(i*100)/trainingEntries.size()+"%");
			}
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
			this.usedFeatures.add(new UsageFeature(fe.description, fe.getUsageType(), fe.getDataType(), tInfo.usedFeatures[i]));
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
	
	
	
	public static class UsageFeature {
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
		if (trainedCount % 10 == 0) {
			System.out.println("A tree has been built scheduledCount:" + scheduledCount + ",builCount:" + trainedCount
					+ ",elapsedTime:" + elapsedTime);
		}

	}
}

public static class ForestRoot {
	// public static Logger log = Logger.getLogger(ForestRoot.class.getName());
	public static final int F_I = 2;
//0.01, 500, 1500000, 0.8, 0.8
	
	public static interface ForestListener {
		public void onTreeBuild(int scheduledCount, int trainedCount, long elapsedTime);
	}
	
	public static class ForestSettings {
		public int maxTrees = 4;
		public double cutoffError = 0.01;
		public int maxLevel = 500;
		public long maxTime = 150000;
		public double usedEntriesPercentage = 1.0d;
		public double usedFeaturesPercentage = 0.9d;
		public boolean runParallel = true;
		public int maxNumberOfRunners = 8;
	}
	/*public static class ForestSettings {
		public int maxTrees = 1;
		public double cutoffError = 0.01;
		public int maxLevel = 500;
		public long maxTime = 150000;
		public double usedEntriesPercentage = 1.0f;
		public double usedFeaturesPercentage = 1.0f;
		public boolean runParallel = false;
		public int maxNumberOfRunners = 8;
	}*/
	

	public static enum BestFeatureType {
		error, correlation, none, distance
	};

	public static class TreesInfo {
		public int[] usedFeatures;
		@Override
		public String toString() {
			return "";
			//return ToStringBuilder.reflectionToString(this);
		}
	}

	public static class TreeInfo {
		public int[] usedFeatures;
	}

	public static class ForestInfo {
		public int numberOfTrees;
		public List<TreesInfo> epochInfo = new LinkedList<TreesInfo>();
		@Override
		public String toString() {
			//ToStringBuilder ts = new ToStringBuilder(this);
			return "";
			//return ToStringBuilder.reflectionToString(this);
		}
	}

	public static class RandomForest {
		private ForestListener forestListener;
		List<RegressionTree> trees = new ArrayList<RegressionTree>();
		int epoch = 0;
		// public int maxNumberOfRunners = 4;
		// public boolean runParallel = true;
		// private static final double NAN = Double.NaN;
		// private static final int MAX_LEVEL_HOSP = 6;
		// private static final int MAX_LEVEL = 10;
		
		private ForestInfo forestInfo = new ForestInfo();

		// private final SimpleRandom rnd = new SimpleRandom();
		public ForestInfo getForestInfo() {
			ForestInfo toReturn = this.forestInfo;
			int calculateFrom = toReturn.epochInfo.size();
			for (int i = calculateFrom; i < this.epoch; i++) {
				TreesInfo epochInfo = new TreesInfo();
				for (RegressionTree tree : this.trees) {
					if (tree.epoch == i) {
						TreeInfo treeInfo = tree.getTreeInfo();
						mergeUsedFeaturesInto(treeInfo, epochInfo);
					}
				}
				toReturn.epochInfo.add(epochInfo);

			}
			toReturn.numberOfTrees = this.trees.size();
			return toReturn;
		}

		void mergeUsedFeaturesInto(TreeInfo tree, TreesInfo trees) {
			if (trees.usedFeatures == null) {
				trees.usedFeatures = Arrays.copyOf(tree.usedFeatures, tree.usedFeatures.length);
			} else {
				for (int i = F_I; i < tree.usedFeatures.length; i++) {
					tree.usedFeatures[i] += tree.usedFeatures[i];
				}
			}
		}

		/*
		 * public void train(List<X> entries, int maxTrees, double cutoffError,
		 * int maxLevel, long maxTime, double usedEntriesPercentage, double
		 * usedFeaturesPercentage) { double[][] data = extractData(entries);
		 * this.executeTraining(data, maxTrees, cutoffError, maxLevel, maxTime,
		 * usedEntriesPercentage, usedFeaturesPercentage);
		 * 
		 * }
		 * 
		 * public void predict(List<X> entries) { for (X entry : entries) {
		 * double[] features = extractData(entry); double p =
		 * this.predict(features); entry.setOutput(p); } }
		 * 
		 * public void predict(X entry) { double[] features = extractData(entry);
		 * double p = this.predict(features); entry.setOutput(p); }
		 * 
		 * private double[][] extractData(List<X> entries) { double[]
		 * childFeatures = problem.extractData(entries.get(0)); double[][]
		 * dataFeatures = new double[entries.size()][childFeatures.length];
		 * 
		 * for (int i = 0; i < entries.size(); i++) { childFeatures =
		 * problem.extractData(entries.get(i)); for (int j = 0; j <
		 * childFeatures.length; j++) { dataFeatures[i][j] = childFeatures[j]; } }
		 * return dataFeatures; }
		 * 
		 * private double[] extractData(X entry) { return
		 * problem.extractData(entry); }
		 */

		public RandomForest(ForestListener listener) {
			this.forestListener = listener;
			// this.maxNumberOfRunners = maxNumberOfRunners;
			// this.runParallel = runParallel;
		}

		void add(RegressionTree tree) {
			trees.add(tree);
		}

		int size() {
			return trees.size();
		}

		void clear() {
			trees.clear();
		}

		void executeTraining(final double[][] data, ForestSettings settings) {
			this.clear();
			long startTime = System.currentTimeMillis();
			int usedFeaturesCount = data[0].length - F_I;
			usedFeaturesCount = Math.min(
					Math.max(0, (int) Math.round((double) usedFeaturesCount * settings.usedFeaturesPercentage)),
					data[0].length - F_I);
			settings.usedEntriesPercentage = Math.min(Math.max(0.01, settings.usedEntriesPercentage), 1.0f);
			if (settings.runParallel) {
				int trainedCnt = 0;
				int scheduledCnt = 0;
				int maxNumberOfRunners = Math.min(settings.maxTrees, settings.maxNumberOfRunners);
				ExecutorService executor = Executors.newFixedThreadPool(settings.maxNumberOfRunners);
				CompletionService<RegressionTree> compService = new ExecutorCompletionService<RegressionTree>(executor);

				for (int i = 0; i < maxNumberOfRunners; i++) {
					RegressionTree tree = new RegressionTree(data, settings.maxLevel, settings.cutoffError,
							settings.usedEntriesPercentage, usedFeaturesCount, epoch);
					ForestTask task = new ForestTask(tree);
					scheduledCnt++;
					compService.submit(task);
				}
				// TODO implement adptive timeut
				boolean timeout = false;
				// List<RegressionTree> completedTrees = Concurrent
				long elapsedTime = 0;
				while (true) {
					trainedCnt++;
					try {
						//System.out.println("Wait for build tree trainedCnt:" + trainedCnt + ", scheduledCnt:" + scheduledCnt);
						Future<RegressionTree> treeF = compService.take();
						elapsedTime = System.currentTimeMillis() - startTime;
						//System.out.println("Build tree trainedCnt:" + trainedCnt + ", scheduledCnt:" + scheduledCnt);
						this.add(treeF.get());
						this.forestListener.onTreeBuild(scheduledCnt, trainedCnt, elapsedTime);
					} catch (InterruptedException ex) {
						ex.printStackTrace();
					} catch (ExecutionException ex) {
						ex.printStackTrace();
					} finally {
						//trainedCnt++;
					}
					if (!timeout) {
						timeout = (System.currentTimeMillis() > startTime + settings.maxTime);
					}
					if (!timeout && scheduledCnt < settings.maxTrees) {
						RegressionTree tree = new RegressionTree(data, settings.maxLevel, settings.cutoffError,
								settings.usedEntriesPercentage, usedFeaturesCount, epoch);
						ForestTask task = new ForestTask(tree);
						scheduledCnt++;
						compService.submit(task);
					}
					if (timeout) {
						System.out.println("Timeout trainedCnt:" + trainedCnt + ", scheduledCnt:" + scheduledCnt);
						if (scheduledCnt == trainedCnt)
							break;
					}
					if (scheduledCnt == trainedCnt && scheduledCnt == settings.maxTrees) {
						System.out.println("Finished before timeout:" + trainedCnt + ", scheduledCnt:" + scheduledCnt);
						if (scheduledCnt <= trainedCnt)
							break;
					}
				}

			} else {
				int i = 0;
				System.out.println("settings.maxTrees:"+settings.maxTrees);
				for (int trees = 0; trees < settings.maxTrees;) {
					
					try {
						//3000:500:0.01:0.800000011920929:70:0
						//3000:500:0.01:0.8:70:0
						//System.out.println(data.length+":"+settings.maxLevel+":"+settings.cutoffError+":"+settings.usedEntriesPercentage+":"+usedFeaturesCount+":"+epoch);
						RegressionTree tree = new RegressionTree(data, settings.maxLevel, settings.cutoffError,
								settings.usedEntriesPercentage, usedFeaturesCount, epoch);
						tree.build();
						this.add(tree);
						i++;
						trees++;
					} catch (Throwable th) {
						th.printStackTrace();
					}
					long elapsedTime = System.currentTimeMillis() - startTime;
					this.forestListener.onTreeBuild(i, i, elapsedTime);
					if (System.currentTimeMillis() > startTime + settings.maxTime) {
						System.out.println("timeout:"+settings.maxTime);
						break;
					}
				}
			}
			epoch++;
		}

		double predict(double[] features) {
			double ret = 0;
			for (RegressionTree tree : trees) {
				double values = tree.predict(features);
				ret += values;
			}
			ret /= (double) trees.size();
			return ret;
		}

		class ForestTask implements Callable<RegressionTree> {
			RegressionTree tree;

			ForestTask(RegressionTree tree) {
				this.tree = tree;
			}

			@Override
			public RegressionTree call() throws Exception {
				this.tree.build();
				return this.tree;
			}

		}
	}

	static class RegressionTree {
		int epoch;
		private static final int MIN_ENTRIES_PER_NODE = 3;
		private Node rootNode;
		private Random rnd = new Random();
		private double[][] treeData;
		double usedEntriesPercentage;
		int usedFeaturesCount;
		int maxLevel;
		double cutoffError;

		private double[][] selectEntries() {
			double[][] toReturn = makeRandomArray(treeData, usedEntriesPercentage);
			return toReturn;
		}

		private double[][] makeRandomArray(double[][] arr, double resultPercentage) {
			int numberOfEntries = (int) (resultPercentage * arr.length);
			return makeRandomArray(arr, numberOfEntries);
		}

		private double[][] makeRandomArray(double[][] arr, int resultCnt) {
			int allCnt = arr.length;
			int numberOfThrowAways = allCnt - resultCnt;

			double[][] toReturn = new double[resultCnt][arr[0].length];
			Set<Integer> toThrowOut = new HashSet<Integer>(numberOfThrowAways);
			while (toThrowOut.size() < numberOfThrowAways) {
				int index = rnd.nextInt(allCnt);
				toThrowOut.add(index);
			}
			int cnt = 0;
			for (int i = 0; i < allCnt; i++) {
				if (!toThrowOut.contains(i)) {
					toReturn[cnt] = arr[i];
					cnt++;
				}
			}
			return toReturn;

		}

		private int[] selectFeatures() {
			int numberOfFeatures = treeData[0].length - F_I;
			int numberOfThrowAways = numberOfFeatures - usedFeaturesCount;
			Set<Integer> toThrowOut = new HashSet<Integer>(numberOfThrowAways);
			while (toThrowOut.size() < numberOfThrowAways) {
				int index = rnd.nextInt(numberOfFeatures);
				toThrowOut.add(index);
			}
			int[] toReturn = new int[usedFeaturesCount];
			int cnt = 0;
			for (int i = 0; i < numberOfFeatures; i++) {
				if (!toThrowOut.contains(i)) {
					toReturn[cnt] = i + F_I;
					cnt++;
				}
			}
			return toReturn;
		}

		private double calculateAverage(double[][] entries) {
			double sum = 0.0f;
			for (int i = 0; i < entries.length; i++) {
				double v = entries[i][1];
				sum += v;
			}
			return sum / (double) entries.length;

		}

		private double calculateError(double[][] entries, double average) {
			double sum = 0f;
			for (int i = 0; i < entries.length; i++) {
				double v = entries[i][1];
				sum += (v - average) * (v - average);
			}
			return sum;
		}

		void build() {
			try {
				double[][] selectedEntries = this.selectEntries();
				double average = calculateAverage(selectedEntries);
				double error = calculateError(selectedEntries, average);
				this.rootNode = new Node(1, selectedEntries, average, error);
				this.rootNode.build();
			} catch (Throwable th) {
				System.out.println("Build failed");
				th.printStackTrace();
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("Build failed HE?");
				throw th;
			}
		}

		TreeInfo getTreeInfo() {
			TreeInfo toReturn = new TreeInfo();
			Node node = this.rootNode;
			toReturn.usedFeatures = node.getNodeInfo().usedFeatures;
			return toReturn;
		}

		RegressionTree(final double[][] data, final int maxLevel, double cutoffError, double usedEntriesPercentage,
				int usedFeaturesCount, int epoch) {
			this.treeData = data;
			this.maxLevel = maxLevel;
			this.usedEntriesPercentage = usedEntriesPercentage;
			this.usedFeaturesCount = usedFeaturesCount;
			this.cutoffError = cutoffError;
			this.epoch = epoch;
		}

		/*
		 * private final double errorOLD(int n, double sum, double sumSquares) {
		 * double toReturn = n == 0 ? 0 : sumSquares - sum * sum / n;
		 * System.out.println("Error calculated:" + toReturn); return toReturn; }
		 * 
		 * private final double error(int n, double average) { double toReturn = n
		 * == 0 ? 0 : sumSquares - (sum * sum / n);
		 * System.out.println("Error calculated:" + toReturn); return toReturn; }
		 */

		double predict(double[] data) {
			return this.rootNode.predict(data);
		}

		class BestFeatureInfo {
			BestFeatureType type = BestFeatureType.none;
			int splitFeature = -1;
			// double comp = Double.NEGATIVE_INFINITY;
			double splitVal = Double.NaN;

			@Override
			public String toString() {
				return "type:" + type + ", featureIndex:" + splitFeature + ", splitVal:" + splitVal;// +
																																// ", comp:"+comp;
			}
		}

		class Node {
			Node left, right;
			int level;
			double average, error;
			double[][] entries;
			BestFeatureInfo bestFeature = new BestFeatureInfo();

			public String toString() {
				String toReturn = "";
				double[] values = new double[entries.length];
				int to = Math.min(values.length, 20);
				for (int i = 0; i < to; i++) {
					values[i] = entries[i][1];
				}
				toReturn = "FeatureInfo:" + bestFeature + ", level:" + level + ",average:" + average + ",error:" + error
						+ ",values(" + (to == values.length) + ")" + Arrays.toString(values);

				return toReturn;
			}

			Node(int level, double[][] entries, double average, double error) {
				this.level = level;
				this.average = average;
				this.error = error;
				this.entries = entries;
			}

			boolean isLeaf() {
				return left == null && right == null;
			}

			class Double2 {
				double left;
				double right;

				@Override
				public String toString() {
					return "[left:" + left + ", right:" + right + "]";
				}
			}

			class DoubleArray2 {
				double[][] left;
				double[][] right;
			}

			DoubleArray2 splitOnFeature(int featureIndex, double featureValue) {
				DoubleArray2 toReturn = new DoubleArray2();
				List<double[]> left = new ArrayList<double[]>(entries.length / 2);
				List<double[]> right = new ArrayList<double[]>(entries.length / 2);
				for (int i = 0; i < entries.length; i++) {
					double[] entry = entries[i];
					if (entry[featureIndex] < featureValue) {
						left.add(entry);
					} else {
						right.add(entry);
					}
				}
				int size = entries[0].length;
				toReturn.left = new double[left.size()][size];
				toReturn.right = new double[right.size()][size];
				for (int i = 0; i < left.size(); i++) {
					toReturn.left[i] = left.get(i);
				}
				for (int i = 0; i < right.size(); i++) {
					toReturn.right[i] = right.get(i);
				}
				return toReturn;
			}

			String toString(double[][] entries, int valueIndex) {
				double[] values = new double[entries.length];
				for (int i = 0; i < values.length; i++) {
					values[i] = entries[i][valueIndex];
				}
				return Arrays.toString(values);

			}

			class EntryFeatureComparator implements Comparator<double[]> {
				int featureIndex = 0;

				EntryFeatureComparator(int featureIndex) {
					this.featureIndex = featureIndex;
				}

				@Override
				public int compare(double[] o1, double[] o2) {
					return Double.compare(o1[featureIndex], o2[featureIndex]);
				}

			}

			void calculateBestFeature(int[] featuresIndexes, Double2 bestAverages, Double2 bestErrors,
					BestFeatureInfo bestInfo) {
				int n = entries.length;
				bestInfo.type = BestFeatureType.error;
				double minErrorSum = Double.MAX_VALUE;
				for (int i = 0; i < featuresIndexes.length; i++) {
					int featureIndex = featuresIndexes[i];
					// System.out.println("Feature start:"+featureIndex);
					Arrays.sort(entries, new EntryFeatureComparator(featureIndex));
					double av1 = 0.0f;
					double e1 = 0.0f;
					double av2 = calculateAverage(entries);
					double e2 = calculateError(entries, av2);
					// System.out.println("Feature values:"+toString(entries,
					// featureIndex));
					for (int j = 1; j < n; j++) {

						double v = entries[j - 1][1];
						double av1_0 = av1;
						double av2_0 = av2;
						av1 = av1 + (v - av1) / (double) (j);
						av2 = av2 + (av2 - v) / (double) (n - j);
//						if(Double.isNaN(av1)||Double.isNaN(av2new)||Double.isNaN(e1)||Double.isNaN(e2)) {
//							System.out.println("Lolo");
//							
//						}
						// System.out.println("Averages:"+av1+":"+av2);
						e1 = e1 + v * v + (double) (j - 1) * av1_0 * av1_0 - (double) j * av1 * av1;
						e2 = e2 - v * v + (double) (n - j + 1) * av2_0 * av2_0 - (double) (n - j) * av2 * av2;
						// System.out.println("Errors:"+e1+":"+e2);
						boolean valEqual = entries[j][featureIndex] == entries[j - 1][featureIndex]; 
						if (valEqual) {
							// System.out.println("Jump to next. Same values");
							continue;
						}

						if (e1 + e2 < minErrorSum - 0.000001) {
							minErrorSum = e1 + e2;
							bestErrors.left = e1;
							bestErrors.right = e2;
							bestAverages.left = av1;
							bestAverages.right = av2;
							bestInfo.splitFeature = featureIndex;
							bestInfo.splitVal = entries[j][featureIndex];
						}
//						if(Double.isNaN(av1)||Double.isNaN(av2)||Double.isNaN(e1)||Double.isNaN(e2)) {
//							System.out.println("Lolo");
//							
//						}
					}
//					if(bestInfo.splitFeature==-1) {
//						System.out.println("Lolo");
//					}
					// System.out.println("Feature end:"+featureIndex);
				}
				if (bestErrors.left < 0.0f)
					bestErrors.left = 0.0f;
				if (bestErrors.right < 0.0f)
					bestErrors.right = 0.0f;
				// System.out.println("Calculated best:"+this);
			}

			void build() {
				if (this.entries.length <= MIN_ENTRIES_PER_NODE) {
					//System.out.println("Stopped on min nodes:"+this.toString());
					return;
				}
				if (this.error <= cutoffError) {
					//System.out.println("Stopped on low error:"+this.toString());
					return;
				}
				if (this.level > maxLevel) {
					//System.out.println("Stopped on maxLevel:"+this.toString());
					return;
				}

				Double2 bestAverages = new Double2();
				Double2 bestErrors = new Double2();
				int[] featuresIndexes = selectFeatures();
				calculateBestFeature(featuresIndexes, bestAverages, bestErrors, this.bestFeature);
				DoubleArray2 entryIndexes2 = splitOnFeature(bestFeature.splitFeature, bestFeature.splitVal);

				Node leftNode = new Node(this.level + 1, entryIndexes2.left, bestAverages.left,
						(Math.sqrt(bestErrors.left)));
				Node rightNode = new Node(this.level + 1, entryIndexes2.right, bestAverages.right,
						(Math.sqrt(bestErrors.right)));
				this.right = rightNode;
				this.left = leftNode;
				this.entries = new double[1][this.entries[0].length];
				this.left.build();
				this.right.build();

			}

			double predict(double[] features) {
				if (!this.isLeaf()) {
					if (features[this.bestFeature.splitFeature] < this.bestFeature.splitVal) {
						// System.out.println("Predict left node:" + this);
						// System.out.println("Input feature value:" +
						// features[this.bestFeature.splitFeature]);
						return left.predict(features);
					} else {
						// System.out.println("Predict right node:" + this);
						// System.out.println("Input feature value:" +
						// features[this.bestFeature.splitFeature]);
						return right.predict(features);
					}
				}
				// System.out.println("Predict leaf node:" + this);
				return this.average;
			}

			class NodeInfo {
				public int[] usedFeatures = null;

				public NodeInfo() {
					if (entries == null || entries.length == 0) {
						usedFeatures = new int[100000000];
					} else {
						usedFeatures = new int[entries[0].length];
					}
				}

			}

			NodeInfo getNodeInfo() {
				NodeInfo toReturn = new NodeInfo();
				if (this.bestFeature.splitFeature >= F_I)
					toReturn.usedFeatures[this.bestFeature.splitFeature]++;
				if (this.left != null) {
					NodeInfo leftInfo = this.left.getNodeInfo();
					mergeUsedFeaturesInto(leftInfo, toReturn);
				}
				if (this.right != null) {
					NodeInfo rightInfo = this.right.getNodeInfo();
					mergeUsedFeaturesInto(rightInfo, toReturn);
				}
				return toReturn;
			}

			void mergeUsedFeaturesInto(NodeInfo child, NodeInfo parent) {
				for (int i = F_I; i < entries[0].length; i++) {
					parent.usedFeatures[i] += child.usedFeatures[i];

				}
			}
		}
	}
}
}
