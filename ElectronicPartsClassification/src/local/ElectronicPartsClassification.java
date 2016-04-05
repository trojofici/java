package local;

import java.sql.Date;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import trojo.topcoder.randomforest.Overmind;
import trojo.topcoder.randomforest.Overmind.DoubleEntry;
import trojo.topcoder.randomforest.Overmind.DoubleProblem;
import trojo.topcoder.randomforest.Overmind.EcmaProblem;
import trojo.topcoder.randomforest.Overmind.InputFeature;
import trojo.topcoder.randomforest.Overmind.ProblemEntry;
import trojo.topcoder.randomforest.Overmind.ProblemEntryData;
import trojo.topcoder.randomforest.Overmind.UsageFeature.FeatureDataType;

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
			over.fillTestOutput();
			for (Iterator<ELEEntry> iterator = test.iterator(); iterator.hasNext();) {
				ELEEntry problemEntryData = iterator.next();
				System.out.println("Test output:"+problemEntryData.getOutput());
			}
			over.printUsedFeatures();
			
		}
						
		
		
		
		
		
		return toReturn;
	}

}
