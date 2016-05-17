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
		long start = System.currentTimeMillis();
		for (double i = 5; i < 20; i+=0.5) {
			for (double j = 5; j < 20; j+=0.5) {
				for (double k = 5.25; k < 20.25; k+=0.5) {
					double val=0;
					/*if((i+j)*k>10) {
						val = 10;
					} else {
						val = 20;
					}*/
					val = (i+j)*k;
					if(Double.isNaN(val)||Double.isInfinite(val)) {
						val = -10;
					}
					train.add(new DoubleEntry(i, new double[]{i,j,k}, val));
				}
			}
		}

		List<DoubleEntry> test = new ArrayList<DoubleEntry>();
		test.add(new DoubleEntry(666, new double[]{7.1d,8.1d,14.1d}, 13d));
		Overmind<DoubleEntry> over = new Overmind<DoubleEntry>(testProblem, train, test);
		for (int i = 0; i < 4; i++) {
			over.completeEntries();
			over.train();
			over.fillTestOutput();
			for (Iterator<DoubleEntry> iterator = test.iterator(); iterator.hasNext();) {
				DoubleEntry problemEntryData = iterator.next();
				System.out.println("Test output:"+problemEntryData.output);
			}
			over.printUsedFeatures();
			
		}
		System.out.println("Executed in:"+(System.currentTimeMillis()-start)/1000);
	}

}
