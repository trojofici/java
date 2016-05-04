package sequencing;

import java.beans.FeatureDescriptor;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
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
	public static int NUMBER_OF_FEATURES = 10;
	public static int WINDOW_SIZE = 150;
	public static int MAX_SEQUENCE_SIZE = 100000;
	public static int PAD = 10;
	
	HistogramParameters p1 = new HistogramParameters(4);
	
	

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

	public static class HistogramParameters {
		public int count;
		public float[] limits;
		public float min = Float.POSITIVE_INFINITY;
		public float max = Float.NEGATIVE_INFINITY;
		public HistogramParameters(int count) {
			this.count = count;
		}
		public void calculateLimits() {
			float[] limits = new float[count];
			limits[count - 1] = Float.POSITIVE_INFINITY;
			float step = (max - min) / (float) count;

			for (int i = 0; i < limits.length - 1; i++) {
				limits[i] = min + step * (i + 1);
			}
			
		}
		
		
	}

	public static float[] applyFloatFilter(float[] data0, float[] filter, int pad, HistogramParameters hist) {
		System.out.println("applyFloatFilter start data0.length" + data0.length);
		int half = filter.length / 2;
		float[] toReturn = new float[data0.length - 2 * pad];
		for (int i = pad; i < data0.length - pad; i++) {
			float sum = 0;
			for (int j = 0; j < filter.length; j++) {
				sum += filter[j] * data0[i - half + j];
			}
			toReturn[i - pad] = sum;
			hist.max = Math.max(sum, hist.max);
			hist.min = Math.max(sum, hist.min);

		}
		System.out.println("applyFloatFilter end");

		/*
		 * float[] data = new float[filter.length]; for (int i = 0; i <
		 * data0.length; i++) { int min = Math.max(0, i - half); int max =
		 * Math.min(data0.length - 1, i + half); int index = 0; for (int j = i -
		 * half; j < 0; j++) { data[index] = 0; index++; } for (int j = min; j
		 * <= max && index < filter.length; j++) { data[index] = data0[j];
		 * index++; } for (int j = data0.length; j <= i + half; j++) {
		 * data[index] = 0; index++; }
		 * 
		 * float sum = 0; for (int j = 0; j < data.length; j++) { sum +=
		 * filter[j] * data[j]; } toReturn[i] = sum; }
		 */
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
			// this.features = new float[NUMBER_OF_FEATURES][];
			// for (int i = 0; i < this.features.length; i++) {
			// this.features[i] = new float[source.length()];
			// }
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

	private float[] gaborFilter1 = gaborFilter(5, 3, 2);

	public void createFeatures(Collection<SequenceData> sequenceList) {
		Map<SequenceData, float[]> h1 = new HashMap<SequenceData, float[]>();  
		for (SequenceData sequence : sequenceList) {
			float[] aAndT = new float[sequence.atcg.length + 2 * PAD];
			sequence.features = new float[NUMBER_OF_FEATURES][];
			/*
			 * for (int i = 0; i < this.features.length; i++) { this.features[i]
			 * = new float[source.length()]; }
			 */
			byte[] atcg = sequence.atcg;
			for (int i = 0; i < PAD; i++) {
				aAndT[i] = 0;
			}
			for (int i = aAndT.length - PAD; i < aAndT.length; i++) {
				aAndT[i] = 0;
			}
			for (int i = 0; i < atcg.length; i++) {
				switch (atcg[i]) {
				case A:
					aAndT[i + PAD] = 1;
					break;
				case T:
					aAndT[i + PAD] = 1;
					break;
				default:
					aAndT[i + PAD] = 0;
				}
			}

			float[] datka = applyFloatFilter(aAndT, gaborFilter1, PAD, p1);
			h1.put(sequence, datka);
			
		}
		p1.calculateLimits();
		for (SequenceData sequence : sequenceList) {
			int feaureIndex = 0;
			float[] datka = h1.get(sequence);
			float[][] histo = createHistogramSequence(datka, p1, WINDOW_SIZE);
			for (int i = 0; i < histo.length; i++) {
				sequence.features[feaureIndex] = histo[i];
				feaureIndex++;
			}
			
			
		}

	}

	public float[][] createHistogramSequence(float[] data, HistogramParameters parameters, int window) {
		System.out.println("createHistogramSequence start");
		float[][] toReturn = new float[parameters.count][];
		int[] hist = new int[parameters.count];
		Arrays.fill(hist, 0);
		for (int i = 0; i < parameters.count; i++) {
			toReturn[i] = new float[data.length];
		}
		

		for (int i = 0; i < window; i++) {
			for (int j = 0; j < parameters.limits.length; j++) {
				if (data[i] < parameters.limits[j]) {
					hist[j]++;
					break;
				}
			}
		}

		for (int i = window; i < data.length; i++) {
			for (int j = 0; j < parameters.limits.length; j++) {
				toReturn[j][i - window] = hist[j];
				if (data[i] < parameters.limits[j]) {
					hist[j]++;
					break;
				}
				if (data[i - window] < parameters.limits[j]) {
					hist[j]--;
					break;
				}
			}
		}
		System.out.println("createHistogramSequence end");
		return toReturn;
	}

	float[] aAndT;

	public void assignmentInitArrays() {
		this.aAndT = new float[WINDOW_SIZE + 2 * PAD];
		Arrays.fill(aAndT, 0);
	}

	public float[] createFeatures(byte[] atcg, float[] aAndT) {
		int feaureIndex = 0;

		float[] toReturn = new float[NUMBER_OF_FEATURES];
		for (int i = 0; i < atcg.length; i++) {
			switch (atcg[i]) {
			case A:
				aAndT[i + PAD] = 1;
				break;
			case T:
				aAndT[i + PAD] = 1;
				break;
			default:
				aAndT[i + PAD] = 0;
			}
		}

		float[] datka = applyFloatFilter(aAndT, gaborFilter1, PAD, p1);
		float[][] histo = createHistogramSequence(datka, HISTOGRAM1);
		for (int i = 0; i < histo.length; i++) {
			toReturn[feaureIndex] = histo[i];
			feaureIndex++;
		}
	}

	public float[][] createHistogramSequence(float[] data, int cnt) {
		System.out.println("createHistogramSequence start");
		float[][] toReturn = new float[cnt][];
		int[] hist = new int[cnt];
		Arrays.fill(hist, 0);
		float min = Float.POSITIVE_INFINITY;
		float max = Float.NEGATIVE_INFINITY;
		for (int i = 0; i < data.length; i++) {
			float d = data[i];
			min = Math.min(min, d);
			max = Math.max(max, d);
		}
		for (int i = 0; i < cnt; i++) {
			toReturn[i] = new float[data.length];
		}
		float[] limits = new float[cnt];
		limits[cnt - 1] = Float.POSITIVE_INFINITY;
		float step = (max - min) / (float) cnt;

		for (int i = 0; i < limits.length - 1; i++) {
			limits[i] = min + step * (i + 1);
		}

		for (int i = 0; i < WINDOW_SIZE; i++) {
			for (int j = 0; j < limits.length; j++) {
				if (data[i] < limits[j]) {
					hist[j]++;
					break;
				}
			}
		}

		for (int i = WINDOW_SIZE; i < data.length; i++) {
			for (int j = 0; j < limits.length; j++) {
				toReturn[j][i - WINDOW_SIZE] = hist[j];
				if (data[i] < limits[j]) {
					hist[j]++;
					break;
				}
				if (data[i - WINDOW_SIZE] < limits[j]) {
					hist[j]--;
					break;
				}
			}
		}
		System.out.println("createHistogramSequence end");
		return toReturn;
	}

	@Override
	public int preProcessing() {
		System.out.println("preProcessing start");
		createFeatures(this.sequences.values());
		System.out.println("preProcessing end");
		return 0;
	}

	@Override
	public String[] getAlignment(int nreads, double norm_a, double d, String[] readName, String[] readSequence) {
		String[] toReturn = new String[nreads];
		float[][] probability = new float[this.sequences.size()][MAX_SEQUENCE_SIZE - WINDOW_SIZE];
		for (int i = 0; i < readSequence.length; i++) {
			String readN = readName[i];
			String readS = readSequence[i];

			boolean found = false;
			for (Integer sequenceId : this.sequences.keySet()) {
				SequenceData fullSequence = this.sequences.get(sequenceId);
				int index = fullSequence.indexOf(readS);
				System.out.println(readN + ":" + readS + ":" + index);
				if ((i + 1) % 20 == 0) {
					System.out.println((int) (i * 100 / (double) readSequence.length) + "% done");
					System.out.println(readN + ":" + readS + ":" + index);
				}
				if (index >= 0) {
					String toAdd = readN + "," + sequenceId + "," + (index + 1) + "," + (index + 1 + readS.length())
							+ ",+,1.00";
					System.out.println(toAdd);
					toReturn[i] = toAdd;
					found = true;
					break;
				}
			}
			if (!found) {
				String toAdd = readN + "," + 1 + "," + 1 + "," + 1 + ",+,1.00";
				System.out.println(toAdd);
				toReturn[i] = toAdd;
			}
		}

		return toReturn;
	}

}
