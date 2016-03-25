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
		for (double i = 1; i < 50; i+=0.1) {
			for (double j = 1; j < 50; j+=0.1) {
				train.add(new DoubleEntry(i, new double[]{i,j}, i*j+j));
			}
		}
		
		//train.add(new DoubleEntry(1.0, new double[]{0.1,0.1}, 0.0));
		//train.add(new DoubleEntry(2.0, new double[]{0.1,0.0}, 1.0));
		//train.add(new DoubleEntry(3.0, new double[]{0.0,1.0}, 1.0));
		//train.add(new DoubleEntry(4.0, new double[]{0.0,0.0}, 1.0));
		List<DoubleEntry> test = new ArrayList<DoubleEntry>();
		test.add(new DoubleEntry(666, new double[]{4d,5d}, 25d));
		Overmind<DoubleEntry> over = new Overmind<DoubleEntry>(testProblem, train, test);
		over.completeEntries();
		over.train();
		over.fillTestOutput();
		for (Iterator<DoubleEntry> iterator = test.iterator(); iterator.hasNext();) {
			DoubleEntry problemEntryData = iterator.next();
			System.out.println("Test output:"+problemEntryData.output);
		}
		over.writeUsedFeatures();
		//System.out.println(over.forest.getForestInfo());
		
		
		

	}

}
