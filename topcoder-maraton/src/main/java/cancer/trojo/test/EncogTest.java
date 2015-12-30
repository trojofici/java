package cancer.trojo.test;

import org.encog.engine.network.activation.ActivationSigmoid;
import org.encog.ml.CalculateScore;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataPair;
import org.encog.ml.train.strategy.Greedy;
import org.encog.ml.train.strategy.HybridStrategy;
import org.encog.ml.train.strategy.StopTrainingStrategy;
import org.encog.neural.data.NeuralDataSet;
import org.encog.neural.data.basic.BasicNeuralDataSet;
import org.encog.neural.flat.FlatLayer;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.layers.Layer;
import org.encog.neural.networks.training.Train;
import org.encog.neural.networks.training.TrainingSetScore;
import org.encog.neural.networks.training.anneal.NeuralSimulatedAnnealing;
import org.encog.neural.networks.training.propagation.back.Backpropagation;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;

public class EncogTest {
	
	public static class TrojoLayer extends FlatLayer {
		
	}
	

	public static void main(String[] args) {
		// Logging.stopConsoleLogging();

		BasicNetwork network = new BasicNetwork();
		BasicLayer le = new BasicLayer(10);
		network.addLayer(new BasicLayer(new ActivationSigmoid(), true, 2));
		network.addLayer(new BasicLayer(new ActivationSigmoid(), true, 3));
		network.addLayer(new BasicLayer(new ActivationSigmoid(), true, 1));
		
		
		// network.setLogic(new FeedforwardLogic());
		network.getStructure().finalizeStructure();
		network.reset();

		double[][] inputX = { { 1.0, 1.0 }, { 1.0, 0.0 }, { 0.0, 1.0 }, { 0.0, 0.0 } };
		double[][] outputX = { { 0.0 }, // A XOR B
				{ 1.0 }, { 1.0 }, { 0.0 } };

		NeuralDataSet trainingSet = new BasicNeuralDataSet(inputX, outputX);
		//trainingSet.
		// train the neural network
		
		//final Train train = new Backpropagation(network, trainingSet, 0.7, 0.8);
		CalculateScore score = new TrainingSetScore(trainingSet);
		final NeuralSimulatedAnnealing trainAlt = new NeuralSimulatedAnnealing(
				network, score, 10, 2, 100);
		
		//final Train trainMain = new Backpropagation(network, trainingSet,0.00001, 0.0);
		final Train trainMain = new ResilientPropagation(network, trainingSet);
		//final Train trainMain = new Backpropagation(network, trainingSet);
		
		
		
		StopTrainingStrategy stop = new StopTrainingStrategy();
		trainMain.addStrategy(new Greedy());
		//trainMain.addStrategy(new HybridStrategy(trainAlt));
		trainMain.addStrategy(stop);
		int epoch = 0;
		while (!stop.shouldStop()) {
			trainMain.iteration(1);
			System.out.println("Training " + "jojo" + ", Epoch #" + epoch
					+ " Error:" + trainMain.getError());
			epoch++;
		}

		/*int epoch = 1;

		do {
			train.iteration();
			System.out.println("Epoch #" + epoch + " Error:" + train.getError());
			epoch++;
		} while (train.getError() > 0.001);*/

		// test the neural network
		System.out.println("Neural Network Results:");
		for (MLDataPair pair : trainingSet) {
			final MLData output = network.compute(pair.getInput());
			System.out.println(pair.getInput().getData(0) + "," + pair.getInput().getData(1) + ", actual="
					+ output.getData(0) + ",ideal=" + pair.getIdeal().getData(0));

		}
	}
}
