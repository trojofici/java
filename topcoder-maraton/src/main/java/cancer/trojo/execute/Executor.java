package cancer.trojo.execute;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.channels.ShutdownChannelGroupException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import cancer.trojo.preprocess.Preprocess;

import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgcodecs.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_features2d.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.WindowConstants;

import org.bytedeco.javacpp.opencv_core.*;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.javacpp.indexer.FloatBufferIndexer;
import org.bytedeco.javacpp.indexer.Indexer;
import org.bytedeco.javacpp.indexer.UByteBufferIndexer;
import org.bytedeco.javacpp.indexer.UShortBufferIndexer;
import org.bytedeco.javacv.*;
import org.encog.Encog;
import org.encog.engine.network.activation.ActivationSigmoid;
import org.encog.ml.CalculateScore;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataPair;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.ml.data.basic.BasicMLDataPair;
import org.encog.ml.train.strategy.Greedy;
import org.encog.ml.train.strategy.HybridStrategy;
import org.encog.ml.train.strategy.StopTrainingStrategy;
import org.encog.neural.data.NeuralDataSet;
import org.encog.neural.data.basic.BasicNeuralDataSet;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.Train;
import org.encog.neural.networks.training.TrainingSetScore;
import org.encog.neural.networks.training.anneal.NeuralSimulatedAnnealing;
import org.encog.neural.networks.training.concurrent.ConcurrentTrainingManager;
import org.encog.neural.networks.training.propagation.Propagation;
import org.encog.neural.networks.training.propagation.back.Backpropagation;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;
import org.encog.util.simple.EncogUtility;

public class Executor {
	protected static Map<String, Double> trainCases = new ConcurrentHashMap<>();
	protected static Map<String, Double> testCases = new ConcurrentHashMap<>();
	// protected static double[][] trainInput;
	// protected static double[][] trainOutput;
	protected static List<MLDataPair> trainPair = new ArrayList<>();
	protected static List<MLDataPair> testPair = new ArrayList<>();
	// protected static double[][] testInput;
	// protected static double[][] testOutput;
	protected static Map<String, List<Double>> data = new ConcurrentHashMap<>();
	protected static BasicNetwork network = new BasicNetwork();
	protected static int trainInputSize = 0;
	static AtomicInteger counter = new AtomicInteger(0);
	public static int threadPoolSize = 4;

	protected static String CASE_FILE_POSTFIX = "-TS_half.png";

	public static void main(String[] args) throws IOException, InterruptedException {
		loadTrainCases();
		System.out.println("loadTrainCases finished");
		loadTestCases();
		System.out.println("loadTestCases finished");
		loadTrainingData();
		System.out.println("loadTrainingData finished");
		prepareTrainingData();
		System.out.println("prepareTrainingData finished");
		loadTestData();
		System.out.println("loadTestData finished");
		prepareTestData();
		System.out.println("prepareTestData finished");

		trainNetwork();
		System.out.println("train finished");

		// Thread.sleep(50000);
	}

	protected static void trainNetwork() {
		ConcurrentTrainingManager man = ConcurrentTrainingManager.getInstance();
		network = new BasicNetwork();
		// BasicLayer le = new BasicLayer(10);
		System.out.println("trainInputSize:" + trainInputSize);
		network.addLayer(new BasicLayer(new ActivationSigmoid(), true, trainInputSize));
		network.addLayer(new BasicLayer(new ActivationSigmoid(), true, 250));
		network.addLayer(new BasicLayer(new ActivationSigmoid(), true, 30));
		// network.addLayer(new BasicLayer(new ActivationSigmoid(), true, 10));
		network.addLayer(new BasicLayer(new ActivationSigmoid(), true, 1));

		// network.setLogic(new FeedforwardLogic());
		network.getStructure().finalizeStructure();
		network.reset();

		NeuralDataSet trainingSet = new BasicNeuralDataSet(trainPair);
		// trainingSet.
		// train the neural network

		// final Train train = new Backpropagation(network, trainingSet, 0.7,
		// 0.8);
		CalculateScore score = new TrainingSetScore(trainingSet);
		final NeuralSimulatedAnnealing trainAlt = new NeuralSimulatedAnnealing(network, score, 1, 0.1, 50);

		// final Train trainMain = new Backpropagation(network,
		// trainingSet,0.00001, 0.0);
		final ResilientPropagation trainMain = new ResilientPropagation(network, trainingSet);
		trainMain.setThreadCount(threadPoolSize);

		// trainMain.set
		// final Train trainMain = new Backpropagation(network, trainingSet);

		// Evaluate the neural network.
		// EncogUtility.trainToError(trainMain, 0.01);

		StopTrainingStrategy stop = new StopTrainingStrategy(0.000001d, 200);
		// trainMain.addStrategy(new Greedy());
		// trainMain.addStrategy(new HybridStrategy(trainAlt));
		// man.addTrainingJob(trainMain);
		trainMain.addStrategy(stop);
		int epoch = 0;
		int batch = 50;
		while (!stop.shouldStop()) {
			// System.out.println();
			trainMain.iteration(batch);
			System.out.println("Training " + "jojo" + ", Epoch #" + epoch + " Error:" + trainMain.getError());
			epoch += batch;
			writeOutTestCSV(epoch, trainMain);
			if(epoch>4000) {
				break;
			}
			
		}

		/*
		 * int epoch = 1;
		 * 
		 * do { train.iteration(); System.out.println("Epoch #" + epoch +
		 * " Error:" + train.getError()); epoch++; } while (train.getError() >
		 * 0.001);
		 */

		// test the neural network

		// EncogUtility.evaluate(network, trainingSet);

		System.out.println("Neural Network Results:");
		for (MLDataPair pair : trainingSet) {
			final MLData output = network.compute(pair.getInput());
			System.out.println("actual=" + output.getData(0) + ",ideal=" + pair.getIdeal().getData(0) + ", error:"
					+ Math.abs(output.getData(0) - pair.getIdeal().getData(0)));

		}

		System.out.println("Training " + "finished" + ", Epoch #" + epoch + " Error:" + trainMain.getError());

		Encog.getInstance().shutdown();

	}

	protected static class MLDataPairWithId extends BasicMLDataPair {
		public String key;

		public MLDataPairWithId(MLData input, MLData output, String key) {
			super(input, output);
			this.key = key;
		}

	}

	protected static void prepareTrainingData() {
		prepareData(trainPair, trainCases);
	}

	protected static void prepareData(List<MLDataPair> trainPair, Map<String, Double> data0) {
		for (String key : data0.keySet()) {
			List<Double> caseData0 = data.get(key);
			trainInputSize = caseData0.size();
			// trainInputSize = 70;
			double[] caseData = new double[trainInputSize];
			for (int i = 0; i < trainInputSize; i++) {
				caseData[i] = caseData0.get(i);
			}
			double[] caseOutput = new double[1];
			caseOutput[0] = data0.get(key);
			// caseOutput[0] = 0.5d;
			MLData input = new BasicMLData(caseData);
			MLData output = new BasicMLData(caseOutput);
			MLDataPairWithId pair = new MLDataPairWithId(input, output, key);
			trainPair.add(pair);
		}
	}

	protected static void loadTestCases0() {
		// String r = Preprocess.OUTPUT_ROOT + "/";
		// CASE_FILE_POSTFIX;
		testCases.clear();
		File folder = new File(Preprocess.PROCESSED_ROOT);
		File[] listOfFiles = folder.listFiles();
		for (int i = 0; i < listOfFiles.length; i++) {
			File fill = listOfFiles[i];
			String n = fill.getName();
			if (fill.isFile() && n.endsWith(CASE_FILE_POSTFIX)) {

				String caseKey = n.substring(0, n.length() - CASE_FILE_POSTFIX.length());
				if (!trainCases.containsKey(caseKey)) {
					testCases.put(caseKey, Double.NaN);
					System.out.println("Test file:" + n + ":" + caseKey);
				}
			}
		}
	}

	protected static void loadTestCases() throws IOException {
		testCases.clear();
		BufferedReader csv = new BufferedReader(new FileReader("test.csv"));
		CSVParser parser = new CSVParser(csv, CSVFormat.EXCEL);
		for (CSVRecord csvRecord : parser.getRecords()) {
			String id = csvRecord.get(0);
			testCases.put(id, Double.NaN);
		}
		System.out.println(">>>test case count:"+testCases.size());
		parser.close();
	}

	protected static void prepareTestData() {
		prepareData(testPair, testCases);
	}

	protected static void loadTestData() {
		loadData(testCases.keySet());
	}

	protected static void loadData(Set<String> keys) {

		ExecutorService pool = Executors.newFixedThreadPool(threadPoolSize);
		// ExecutorService pool = Executors.newFixedThreadPool(1);
		for (String key : keys) {
			pool.execute(new LoadDataCommand(key));
		}
		pool.shutdown();
		try {
			pool.awaitTermination(10000, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			System.gc();
			e.printStackTrace();
		}

	}

	protected static void loadTrainCases() throws IOException {
		trainCases.clear();
		BufferedReader csv = new BufferedReader(new FileReader("training.csv"));
		CSVParser parser = new CSVParser(csv, CSVFormat.EXCEL);
		int cnt = 0;
		for (CSVRecord csvRecord : parser.getRecords()) {
			String id = csvRecord.get(0);
			String valS = csvRecord.get(1);
			Double val = Double.valueOf(valS);
			// System.out.println(id + ":" + val);
			trainCases.put(id, val);
			cnt++;
			//if(cnt>10) break;
		}
		parser.close();

	}

	static Object lk = new Object();

	protected static List<Double> loadSobelHistograms(Mat colorImage) {
		// System.out.println("Depth:" + colorImage.depth());
		Mat df_dx = new Mat();
		Mat df_dy = new Mat();
		int depth = CV_32F;
		Sobel(colorImage, df_dx, depth, 1, 0);
		Sobel(colorImage, df_dy, depth, 0, 1);
		List<Double> toReturn = new ArrayList<>();
		toReturn.addAll(loadDataHistogram(df_dx));
		toReturn.addAll(loadDataHistogram(df_dy));
		df_dx.release();
		df_dy.release();
		return toReturn;
	}
	
	protected static List<Double> loadGausHistograms(Mat colorImage) {
		List<Double> toReturn = new ArrayList<>();
		int kSize = 91;
		int count = 4;
		double sigma = 0.5d;
		double sigmaStep = 2.5d;
		double sigmaOuterQ = 2d;
		Mat output = new Mat();
		Mat gaus = new Mat();
		for (int i = 0; i < count; i++) {
			Mat gausInner = getGaussianKernel(kSize, sigma);
			Mat gausOuter = getGaussianKernel(kSize, sigma*sigmaOuterQ);
			subtract(gausInner, gausOuter, gaus);
			filter2D(colorImage, output, 8,gaus);
			toReturn.addAll(loadDataHistogram(output));
			sigma*=sigmaStep;
		}
		return toReturn;

	}

	public static List<Double> loadDataHistogram(Mat colorImage) {
		//return loadDataHistogram(colorImage, 7, 2, 7, true);
		return loadDataHistogram(colorImage, 10, 2, 10, true);
		//return loadDataHistogram(colorImage, 15, 10, 15, true);
		//return loadDataHistogram(colorImage, 3, 3, 3, true);
	}

	protected static List<Double> loadDataHistogram(Mat colorImage, int redSize, int greenSize, int blueSize,
			boolean normalize) {
		int maxVal = 255;
		List<Double> toReturn = new ArrayList<>(redSize * greenSize * blueSize);

		int[][][] counts = new int[redSize][greenSize][blueSize];
		int[] redBuckets = new int[redSize - 1];
		int[] greenBuckets = new int[greenSize - 1];
		int[] blueBuckets = new int[blueSize - 1];
		int step = maxVal / redSize;
		int val = step;
		for (int i = 0; i < redSize - 1; i++) {
			redBuckets[i] = val;
			val += step;
		}
		step = maxVal / greenSize;
		val = step;
		for (int i = 0; i < greenSize - 1; i++) {
			greenBuckets[i] = val;
			val += step;
		}
		step = maxVal / blueSize;
		val = step;
		for (int i = 0; i < blueSize - 1; i++) {
			blueBuckets[i] = val;
			val += step;
		}

		Indexer indexer0 = colorImage.createIndexer();
		//FloatBufferIndexer
		if (indexer0 instanceof FloatBufferIndexer) {
			Mat newImage = new Mat(colorImage.rows(),colorImage.cols(), CV_8UC3);
			//TODO calcultate mint and max and add to output
			convertScaleAbs(colorImage, newImage);
			colorImage = newImage;
			indexer0 = colorImage.createIndexer();
		}
		

		if (indexer0 instanceof UShortBufferIndexer) {
			UShortBufferIndexer indexer = (UShortBufferIndexer) indexer0;
			for (int i = 0; i < 3 * colorImage.cols() * colorImage.rows(); i += 3) {
				int red = indexer.get(i);
				int green = indexer.get(i + 1);
				int blue = indexer.get(i + 2);
				int redI = Arrays.binarySearch(redBuckets, red);
				if (redI < 0) {
					redI = -1 * (redI + 1);
				}
				int greenI = Arrays.binarySearch(greenBuckets, green);
				if (greenI < 0) {
					greenI = -1 * (greenI + 1);
				}
				int blueI = Arrays.binarySearch(blueBuckets, blue);
				if (blueI < 0) {
					blueI = -1 * (blueI + 1);
				}
				// System.out.println(redI+":"+greenI+":"+blueI);
				counts[redI][greenI][blueI]++;
			}
		}else {
			UByteBufferIndexer indexer = (UByteBufferIndexer) indexer0;
			for (int i = 0; i < 3 * colorImage.cols() * colorImage.rows(); i += 3) {
				int red = indexer.get(i);
				int green = indexer.get(i + 1);
				int blue = indexer.get(i + 2);
				int redI = Arrays.binarySearch(redBuckets, red);
				if (redI < 0) {
					redI = -1 * (redI + 1);
				}
				int greenI = Arrays.binarySearch(greenBuckets, green);
				if (greenI < 0) {
					greenI = -1 * (greenI + 1);
				}
				int blueI = Arrays.binarySearch(blueBuckets, blue);
				if (blueI < 0) {
					blueI = -1 * (blueI + 1);
				}
				// System.out.println(redI+":"+greenI+":"+blueI);
				counts[redI][greenI][blueI]++;
			}

		}

		double q = 1.0d;
		if (normalize) {
			q = 1d / (double) (colorImage.cols() * colorImage.rows());
		}

		for (int i = 0; i < redSize; i++) {
			for (int j = 0; j < greenSize; j++) {
				for (int k = 0; k < blueSize; k++) {
					toReturn.add(((double) counts[i][j][k]) * q);
				}
			}
		}

		return toReturn;
	}

	protected static List<Double> loadDataHistogramOld(Mat colorImage) {

		int redSize = 10;
		int blueSize = 10;
		int greenSize = 10;
		List<Double> toReturn = new ArrayList<>(redSize * blueSize * greenSize);
		// int counter0 = counter.incrementAndGet();
		int width = colorImage.cols();
		int height = colorImage.rows();
		// System.out.println(width+":" +
		// height+":"+colorImage.channels()+":"+colorImage.depth());

		Mat output = new Mat();
		float[] histRange = { 0, 255f };

		IntPointer intPtrChannels = new IntPointer(3);
		IntPointer intPtrHistSize = new IntPointer(blueSize, greenSize, redSize);
		PointerPointer<FloatPointer> ptrPtrHistRange = new PointerPointer<FloatPointer>(histRange, histRange,
				histRange);
		while (true) {
			try {

				calcHist(colorImage, 1, intPtrChannels, new Mat(), output, 3, intPtrHistSize, ptrPtrHistRange, true,
						false);
				break;
			} catch (RuntimeException ex) {
				colorImage = colorImage.clone();
				// m.release();
				// m = imread(new File(toLoad).getAbsolutePath(),
				// CV_LOAD_IMAGE_COLOR);
				// ex.printStackTrace();
			}
		}
		// System.out.println(output+":"+output.createIndexer().getClass());
		FloatBufferIndexer indx = (FloatBufferIndexer) output.createIndexer();
		for (int i = 0; i < redSize * blueSize * greenSize; i++) {
			// System.out.println(i);
			toReturn.add((double) indx.get(i));
		}
		output.release();
		return toReturn;
	}

	protected static List<Double> loadDataCropImage(Mat colorImage) {
		List<Double> toReturn = new ArrayList<>(9 * colorImage.rows() * colorImage.cols());
		UByteBufferIndexer indexer = (UByteBufferIndexer) colorImage.createIndexer();
		int chCnt = colorImage.channels();
		List<Double> toPut = new ArrayList<>(9 * colorImage.rows() * colorImage.cols());
		for (int i = 0; i < colorImage.rows(); i++) {
			for (int j = 0; j < colorImage.cols(); j++) {
				// System.out.println(indexer.get(i, j));
				for (int channel = 0; channel < chCnt; channel++) {
					int index = (i * colorImage.cols()) + j;
					int offset = index * chCnt + channel;
					toPut.add(indexer.get(index) / 255d);
				}
			}
		}
		// FIXME finish implementation
		return toReturn;
	}

	private static class LoadDataCommand implements Runnable {
		String caseName;

		public LoadDataCommand(String caseName) {
			this.caseName = caseName;
		}

		public void run() {
			loadDataFromFile(caseName);
		}
	}

	protected static void loadDataFromFile(String caseName) {
		// calcHist()

		String r = Preprocess.PROCESSED_ROOT + "/";
		String toLoad = r + caseName + CASE_FILE_POSTFIX;
		System.out.println("Loading:" + toLoad);

		Mat m = imread(new File(toLoad).getAbsolutePath(), CV_LOAD_IMAGE_COLOR);
		// System.out.println("Loaded:" + m);
		List<Double> toPut0 = new ArrayList<>();

		toPut0.addAll(loadDataHistogram(m));
		toPut0.addAll(loadSobelHistograms(m));
		toPut0.addAll(loadGausHistograms(m));
		

		// trainInputSize = toPut0.size();
		data.put(caseName, toPut0);
		m.release();
	}

	protected static void loadTrainingData() {
		loadData(trainCases.keySet());
	}

	protected static void writeOutTestCSV(int epoch, Propagation prop) {
		CSVPrinter printer = null;
		try {
			String fileName = Preprocess.OUTPUT_ROOT + "/test" + epoch + "_" + prop.getError() + ".csv";
			BufferedWriter csv = new BufferedWriter(new FileWriter(fileName));
			CSVFormat format = CSVFormat.DEFAULT.withQuote(null);
			printer = new CSVPrinter(csv, format);

			for (MLDataPair pair : testPair) {
				MLDataPairWithId withId = (MLDataPairWithId) pair;
				final MLData output0 = network.compute(withId.getInput());
				double output = output0.getData(0);
				String key = withId.key;
				//System.out.println("withId.key:"+key);
				printer.printRecord(key,  String.format("%.2f", output));
				// System.out.println("actual=" + output.getData(0) + ",ideal="
				// + pair.getIdeal().getData(0) + ", error:"
				// + Math.abs(output.getData(0) - pair.getIdeal().getData(0)));

			}
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (printer != null) {
				try {
					printer.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				try {
					printer.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}

}
