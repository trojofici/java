package sequencing;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DNAResearch implements IDNASequencing {
	public static final byte A = 1;
	public static final byte T = 2;
	public static final byte C = 3;
	public static final byte G = 4;
	public static final byte N = 0;
	public static int NUMBER_OF_FEATURES = 2;

	Map<Integer, SequenceData> sequences = new ConcurrentHashMap<Integer, SequenceData>();

	List<float[]> filters = new LinkedList<float[]>();

	public static float[] gaborFilter(int width, float k1, float k2) {
		float[] toReturn = new float[width];
		int half = width / 2;
		float dk1 = 1 / (2 * k1 * k1);
		float dk2 = 2 * (float) Math.PI / k2;
		for (int i = -half; i <= half; i++) {
			toReturn[i + half] = (float) (Math.exp(-i * i * dk1) * Math.cos(i * dk2));
		}
		return toReturn;
	}

	public static float[] identityFilter(int width, float k1, float k2) {
		float[] toReturn = new float[width];
		int half = width / 2;
		for (int i = 0; i < width; i++) {
			toReturn[i] = 0;
			if (i == half) {
				toReturn[i] = 1;
			}
		}
		return toReturn;
	}

	public static float[] applyFloatFilter(float[] data0, float[] filter) {
		int half = filter.length / 2;
		float[] toReturn = new float[data0.length];
		float[] data = new float[filter.length];
		for (int i = 0; i < data0.length; i++) {
			int min = Math.max(0, i - half);
			int max = Math.min(data0.length - 1, i + half);
			int index = 0;
			for (int j = i - half; j < 0; j++) {
				data[index] = 0;
				index++;
			}
			for (int j = min; j <= max && index < filter.length; j++) {
				data[index] = data0[j];
				index++;
			}
			for (int j = data0.length; j <= i + half; j++) {
				data[index] = 0;
				index++;
			}

			float sum = 0;
			for (int j = 0; j < data.length; j++) {
				sum += filter[j] * data[j];
			}
			toReturn[i] = sum;
		}
		return toReturn;
	}

	public static class SequenceData {
		public int id;
		public byte[] atcg;
		public float[][] features;
		// public byte[] at;
		// public byte[] cg;
		// public String source;

		public SequenceData(int id, String source) {
			super();
			this.id = id;
			// this.source = source;
			this.features = new float[NUMBER_OF_FEATURES][];
			for (int i = 0; i < this.features.length; i++) {
				this.features[i] = new float[source.length()];
			}
			atcg = new byte[source.length()];
			for (int i = 0; i < source.length(); i++) {
				char c = source.charAt(i);
				switch (c) {
				case 'A':
					atcg[i] = A;
					break;
				case 'T':
					atcg[i] = T;
					break;
				case 'C':
					atcg[i] = C;
					break;
				case 'G':
					atcg[i] = G;
					break;
				}
			}
		}
	}

	@Override
	public int initTest(int testDifficulty) {
		return 0;
	}

	@Override
	public int passReferenceGenome(int chromatid_seq_id, String[] array) {
		System.out.println("passReferenceGenome start:" + chromatid_seq_id);
		String fullSequence = DNASequencing.parseGenome(array);
		SequenceData data = new SequenceData(chromatid_seq_id, fullSequence);

		System.out.println(fullSequence.length());
		System.out.println(fullSequence.substring(100000, 100200));
		sequences.put(chromatid_seq_id, data);
		System.out.println("passReferenceGenome end:" + chromatid_seq_id);
		return 0;
	}

	private float[] aAndT = new float[150];
	private float[] gaborFilter1  = gaborFilter(5, 3, 2);

	public float[] createFeatures(byte[] atcg, int startIndex, int endIndex) {
		float[] toReturn = new float[NUMBER_OF_FEATURES];
		int index = 0;
		for (int i = startIndex; i < endIndex; i++) {
			switch (atcg[i]) {
			case A:
				aAndT[index]=1;
				break;
			case T:
				aAndT[index]=1;
				break;
			default:
				aAndT[index]=0;
			}
			index++;
		}
		float[] datka = applyFloatFilter(aAndT, gaborFilter1);
		return toReturn;
	}

	@Override
	public int preProcessing() {
		System.out.println("preProcessing start");

		//byte[] data150 = new byte[150];
		for (Integer sid : this.sequences.keySet()) {
			SequenceData data = this.sequences.get(sid);
			for (int i = 0; i < data.atcg.length - 150; i++) {
				//data150 = Arrays.copyOfRange(data.atcg, i, i + data150.length);
				float[] features = createFeatures(data.atcg, i, i+150);
				for (int j = 0; j < features.length; j++) {
					data.features[j][i] = features[j];
				}
				if(i%1000000==0) {
					System.out.println("preprocessed:"+(i*100)/data.atcg.length+"%");
				}
				
			}
			/*
			 * float[] input = new float[data.at.length]; for (int i = 0; i <
			 * input.length; i++) { input[i] = Math.abs(data.at[i]); } float[]
			 * filter = gaborFilter(5, 3, 2); //float[] filter = gaborFilter(9,
			 * 2, 10000000); //'float[] filter = identityFilter(5, 2, 2);
			 * System.out.println("Filter:"+Arrays.toString(filter));
			 * 
			 * float maxValue = 0; float[] filtered = applyFloatFilter(input,
			 * filter); for (int i = 0; i < filtered.length; i++) {
			 * if(filtered[i]>maxValue) maxValue = filtered[i]; }
			 * System.out.println(maxValue);
			 * 
			 * float[] toDisplay = new float[100200-100000]; for (int i = 0; i <
			 * toDisplay.length; i++) { toDisplay[i] = input[i+100000]; }
			 * System.out.println(Arrays.toString(toDisplay)); for (int i = 0; i
			 * < toDisplay.length; i++) { toDisplay[i] = filtered[i+100000]; }
			 * System.out.println(Arrays.toString(toDisplay));
			 * 
			 * 
			 * int bigCount = 0; float sum = 0; float lowValue = 0.8f; float
			 * bigValue = 1.0f; for (int i = 0; i < filtered.length; i++) {
			 * sum+=filtered[i]; if(filtered[i]<bigValue &&
			 * filtered[i]>lowValue) { bigCount++; } if(i>=150) {
			 * sum=-filtered[i-150]; if(filtered[i-150]<bigValue &&
			 * filtered[i-150]>lowValue) { bigCount--; } if(i<100200 &&
			 * i>100000) { System.out.print(bigCount+",");
			 * //System.out.print(sum+","); } } }
			 * 
			 * 
			 * 
			 * 
			 * 
			 * //fullSequence.substring(100000, 100200)
			 */

		}
		System.out.println("preProcessing end");
		return 0;
	}

	@Override
	public String[] getAlignment(int nreads, double norm_a, double d, String[] array, String[] array2) {
		// TODO Auto-generated method stub
		return null;
	}

}
