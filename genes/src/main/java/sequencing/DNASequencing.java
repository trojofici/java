package sequencing;

import java.util.HashMap;
import java.util.Map;

public class DNASequencing implements IDNASequencing {
	Map<Integer, String> fullSequences = new HashMap<Integer, String>();
	public static String parseGenome(String[] chromatidSequence) {
		int fullSize = 0;
		for (int i = 0; i < chromatidSequence.length; i++) {
			fullSize+=chromatidSequence[i].length();
		}
		StringBuffer buff = new StringBuffer(fullSize+1);
		for (int i = 0; i < chromatidSequence.length; i++) {
			String part = chromatidSequence[i];
			//System.out.println(string);
			if(i%10000==0) {
				System.out.println((int)(i*100/(double)chromatidSequence.length)+"% done");
			}
			buff.append(part);
			//fullSequence+=part;
		}
		String fullSequence = buff.toString();
		return fullSequence;
	}
	
	
	public int passReferenceGenome(int chromatidSequenceId, String[] chromatidSequence) {
		System.out.println("passReferenceGenome chromatidSequenceId:"+chromatidSequenceId+", length:"+chromatidSequence.length);
		String fullSequence = parseGenome(chromatidSequence);
		fullSequences.put(chromatidSequenceId, fullSequence);
		System.out.println("Full sequence length:"+fullSequence.length());
		return 0;
	}
	 
	public String[] getAlignment(int n, double normA, double normS, String[] readName, String[] readSequence) {
		String[] toReturn = new String[n];
		for (int i = 0; i < readSequence.length; i++) {
			String readN = readName[i];
			String readS = readSequence[i];
			boolean found = false;
			for (Integer sequenceId : this.fullSequences.keySet()) {
				String fullSequence = this.fullSequences.get(sequenceId);
				int index = fullSequence.indexOf(readS);
				System.out.println(readN+":"+readS+":"+index);
				if((i+1)%20==0) {
					System.out.println((int)(i*100/(double)readSequence.length)+"% done");
					System.out.println(readN+":"+readS+":"+index);
				}
				if(index>=0) {
					String toAdd = readN+","+sequenceId+","+(index+1)+","+(index+1+readS.length())+",+,1.00";
					System.out.println(toAdd);
					toReturn[i]=toAdd;
					found = true;
					break;
				}
			}
			if(!found) {
				String toAdd = readN+","+1+","+1+","+1+",+,1.00";
				System.out.println(toAdd);
				toReturn[i]=toAdd;
			}
		}
		
		return toReturn;
	}
		 
	public int initTest(int testDifficulty) {
		System.out.println("initTest testDifficulty:"+testDifficulty);
		return 0;
	}
		 
	public int preProcessing() {
		System.out.println("preProcessing");
		return 0;
	}

}
