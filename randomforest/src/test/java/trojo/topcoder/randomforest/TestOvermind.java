package trojo.topcoder.randomforest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import trojo.topcoder.randomforest.Overmind.DoubleEntry;
import trojo.topcoder.randomforest.Overmind.DoubleProblem;
import trojo.topcoder.randomforest.Overmind.Feature;
import trojo.topcoder.randomforest.Overmind.Problem;
import trojo.topcoder.randomforest.Overmind.ProblemEntry;
import trojo.topcoder.randomforest.Overmind.ProblemEntryData;

public class TestOvermind {
	
	public static void main(String[] args) {
		DoubleProblem testProblem = new DoubleProblem();
		List<DoubleEntry> train = new ArrayList<DoubleEntry>();
		for (double i = 0; i < 100; i+=0.05) {
			train.add(new DoubleEntry(i, new double[]{i}, i));
		}
		
		//train.add(new DoubleEntry(1.0, new double[]{0.1,0.1}, 0.0));
		//train.add(new DoubleEntry(2.0, new double[]{0.1,0.0}, 1.0));
		//train.add(new DoubleEntry(3.0, new double[]{0.0,1.0}, 1.0));
		//train.add(new DoubleEntry(4.0, new double[]{0.0,0.0}, 1.0));
		List<DoubleEntry> test = new ArrayList<DoubleEntry>();
		test.add(new DoubleEntry(666, new double[]{12.3}, 0.5));
		Overmind<DoubleEntry> over = new Overmind<DoubleEntry>(testProblem, train, test);
		over.train();
		over.fillTestOutput();
		for (Iterator<DoubleEntry> iterator = test.iterator(); iterator.hasNext();) {
			DoubleEntry problemEntryData = iterator.next();
			System.out.println("Test output:"+problemEntryData.output);
		}
		
		
		

	}

}
