package local;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class ElectronicPartsClassificationRun {
	public static void main(String[] args) throws IOException {
		FileInputStream is = new FileInputStream("example_data.csv");
		BufferedReader bis = new BufferedReader(new InputStreamReader(is));
		List<String> lines0 = new LinkedList<String>();
		int i = 0;
		while (true) {
			i++;
			String line = bis.readLine();
			if(i==1) continue;
			if (line == null)
				break;
			lines0.add(line);
			
		}
		bis.close();

		is = new FileInputStream("example_data.csv");
		bis = new BufferedReader(new InputStreamReader(is));
		List<String> lines = new LinkedList<String>();
		i = 0;
		while (i < 20) {
			i++;
			String line = bis.readLine();
			if(i==1) continue;
			if (line == null)
				break;
			lines.add(line);
			
		}
		bis.close();

		String[] train = new String[lines0.size()];
		lines0.toArray(train);

		String[] test = new String[lines.size()];
		lines.toArray(test);
		ElectronicPartsClassification solver = new ElectronicPartsClassification();
		solver.classifyParts(train, test);

	}

}
