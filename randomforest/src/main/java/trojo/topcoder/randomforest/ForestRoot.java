package trojo.topcoder.randomforest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.print.attribute.standard.NumberOfInterveningJobs;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class ForestRoot {
	// public static Logger log = Logger.getLogger(ForestRoot.class.getName());
	public static final int F_I = 2;
//0.01, 500, 1500000, 0.8, 0.8
	
	public static interface ForestListener {
		public void onTreeBuild(int scheduledCount, int trainedCount, long elapsedTime);
	}
	
	public static class ForestSettings {
		public int maxTrees = 8;
		public double cutoffError = 1.0;
		public int minEntriesPerNode = 3;
		public int maxLevel = 500;
		public long maxTime = 150000;
		public double usedEntriesPercentage = 1.0d;
		public double usedFeaturesPercentage = 1.0d;
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
			return ToStringBuilder.reflectionToString(this);
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
			return ToStringBuilder.reflectionToString(this);
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

		void executeTraining(final double[][] data, ForestSettings settings, int[] prefferedOrder) {
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
				System.out.println("Starting training threads");

				for (int i = 0; i < maxNumberOfRunners; i++) {
					RegressionTree tree = new RegressionTree(data, settings.maxLevel, settings.cutoffError, settings.minEntriesPerNode,
							settings.usedEntriesPercentage, usedFeaturesCount, epoch);
					ForestTask task = new ForestTask(tree, prefferedOrder);
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
						RegressionTree tree = new RegressionTree(data, settings.maxLevel, settings.cutoffError, settings.minEntriesPerNode,
								settings.usedEntriesPercentage, usedFeaturesCount, epoch);
						ForestTask task = new ForestTask(tree, prefferedOrder);
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
						RegressionTree tree = new RegressionTree(data, settings.maxLevel, settings.cutoffError, settings.minEntriesPerNode,
								settings.usedEntriesPercentage, usedFeaturesCount, epoch);
						tree.build(prefferedOrder);
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
			int[] prefferedOrder;

			ForestTask(RegressionTree tree,int[] prefferedOrder) {
				this.tree = tree;
				this.prefferedOrder = prefferedOrder;
			}

			@Override
			public RegressionTree call() throws Exception {
				this.tree.build(this.prefferedOrder);
				return this.tree;
			}

		}
	}

	static class RegressionTree {
		int epoch;
		//private static final int MIN_ENTRIES_PER_NODE = 3;
		private Node rootNode;
		private Random rnd = new Random();
		private double[][] treeData;
		double usedEntriesPercentage;
		int usedFeaturesCount;
		int maxLevel;
		double cutoffError;
		int minEntriesPerNode;

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

		private int[] selectFeatures(int[] prefferedOrder) {
			int numberOfFeatures = treeData[0].length - F_I;
			int numberOfThrowAways = numberOfFeatures - usedFeaturesCount;
			Set<Integer> toThrowOut = new HashSet<Integer>(numberOfThrowAways);
			while (toThrowOut.size() < numberOfThrowAways) {
				int index = rnd.nextInt(numberOfFeatures);
				toThrowOut.add(index);
			}
			int[] toReturn = new int[usedFeaturesCount];
			int cnt = 0;
			if(prefferedOrder==null || prefferedOrder.length==0) {
				for (int i = 0; i < numberOfFeatures; i++) {
					if (!toThrowOut.contains(i)) {
						toReturn[cnt] = i + F_I;
						cnt++;
					}
				}
				return toReturn;
			} else {
				if(prefferedOrder.length!=numberOfFeatures) {
					throw new IllegalArgumentException("prefferedOrder wrong size:"+prefferedOrder.length);
				}
				for (int i = 0; i < numberOfFeatures; i++) {
					if (!toThrowOut.contains(i)) {
						toReturn[cnt] = prefferedOrder[i] + F_I;
						cnt++;
					}
				}
				return toReturn;
			}
			
			
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

		void build(int[] prefferedOrder) {
			try {
				double[][] selectedEntries = this.selectEntries();
				double average = calculateAverage(selectedEntries);
				double error = calculateError(selectedEntries, average);
				this.rootNode = new Node(1, selectedEntries, average, error);
				this.rootNode.build(prefferedOrder);
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

		RegressionTree(final double[][] data, final int maxLevel, double cutoffError, int minEntriesPerNode, double usedEntriesPercentage,
				int usedFeaturesCount, int epoch) {
			this.treeData = data;
			this.maxLevel = maxLevel;
			this.usedEntriesPercentage = usedEntriesPercentage;
			this.usedFeaturesCount = usedFeaturesCount;
			this.cutoffError = cutoffError;
			this.minEntriesPerNode = minEntriesPerNode;
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
			
			boolean checkValues(int featureIndex) {
				for (int i = 0; i < entries.length; i++) {
					if(Double.isNaN(entries[i][featureIndex])) {
						//System.out.println("Feature NAN");
						return false;
					}
				}
				return true;
			}

			void calculateBestFeature(int[] featuresIndexes, Double2 bestAverages, Double2 bestErrors,
					BestFeatureInfo bestInfo) {
				int n = entries.length;
				bestInfo.type = BestFeatureType.error;
				double minErrorSum = Double.MAX_VALUE;
				for (int i = 0; i < featuresIndexes.length; i++) {
					int featureIndex = featuresIndexes[i];
					// System.out.println("Feature start:"+featureIndex);
					if(!checkValues(featureIndex)) {
						continue;
					}
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

						if (e1 + e2 < minErrorSum * 0.999 && e1 + e2 < minErrorSum - 0.0001) {
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

			void build(int[] prefferedOrder) {
				if (this.entries.length <= minEntriesPerNode) {
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
				int[] featuresIndexes = selectFeatures(prefferedOrder);
				calculateBestFeature(featuresIndexes, bestAverages, bestErrors, this.bestFeature);
				DoubleArray2 entryIndexes2 = splitOnFeature(bestFeature.splitFeature, bestFeature.splitVal);

				Node leftNode = new Node(this.level + 1, entryIndexes2.left, bestAverages.left,
						(Math.sqrt(bestErrors.left)));
				Node rightNode = new Node(this.level + 1, entryIndexes2.right, bestAverages.right,
						(Math.sqrt(bestErrors.right)));
				this.right = rightNode;
				this.left = leftNode;
				this.entries = new double[1][this.entries[0].length];
				this.left.build(prefferedOrder);
				this.right.build(prefferedOrder);

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
