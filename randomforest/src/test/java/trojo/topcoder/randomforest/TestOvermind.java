package trojo.topcoder.randomforest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import trojo.topcoder.randomforest.Overmind.DoubleEntry;
import trojo.topcoder.randomforest.Overmind.DoubleProblem;
import trojo.topcoder.randomforest.Overmind.Problem;
import trojo.topcoder.randomforest.Overmind.ProblemEntry;
import trojo.topcoder.randomforest.Overmind.ProblemEntryData;

public class TestOvermind {
	
	public static void main(String[] args) {
		DoubleProblem testProblem = new DoubleProblem();
		List<DoubleEntry> train = new ArrayList<DoubleEntry>();
		for (double i = 0; i < 20; i+=0.5) {
			for (double j = 0; j < 20; j+=0.5) {
				for (double k = 0; k < 20; k+=0.5) {
					//double val = i/j+i/k;
					double val = i*j+i*k;
					if(Double.isNaN(val)||Double.isInfinite(val)) {
						val = -1d;
					}
					train.add(new DoubleEntry(i, new double[]{i,j,k}, val));
				}
			}
		}

		List<DoubleEntry> test = new ArrayList<DoubleEntry>();
		test.add(new DoubleEntry(666, new double[]{7d,8d,7d}, 13d));
		Overmind<DoubleEntry> over = new Overmind<DoubleEntry>(testProblem, train, test);
		for (int i = 0; i < 3; i++) {
			over.completeEntries();
			over.train();
			over.fillTestOutput();
			for (Iterator<DoubleEntry> iterator = test.iterator(); iterator.hasNext();) {
				DoubleEntry problemEntryData = iterator.next();
				System.out.println("Test output:"+problemEntryData.output);
			}
			over.printUsedFeatures();
			
		}
	}

}
