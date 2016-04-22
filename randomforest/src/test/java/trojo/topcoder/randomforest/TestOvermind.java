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
		for (double i = 0; i < 10; i+=0.5) {
			for (double j = 0; j < 10; j+=0.5) {
				for (double k = 0; k < 10; k+=0.5) {
					//double val = i/j+k;
					//double val = i*j+i*k;
					//double val = 10d;
					double val = i/(j-2*k);
					if(Double.isNaN(val)||Double.isInfinite(val)) {
						//System.out.println("NAN");
						val = -10;
					}
					train.add(new DoubleEntry(i, new double[]{i,j,k}, val));
				}
			}
		}

		List<DoubleEntry> test = new ArrayList<DoubleEntry>();
		test.add(new DoubleEntry(666, new double[]{7d,8d,14.5d}, 13d));
		Overmind<DoubleEntry> over = new Overmind<DoubleEntry>(testProblem, train, test);
		for (int i = 0; i < 10; i++) {
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
