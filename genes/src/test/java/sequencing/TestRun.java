package sequencing;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.sound.midi.Sequencer;

class TestRun {
	
	public static final int CUTOFF = 1; 
	
	public int initTest(int testDifficulty) {
		return 0;
	}

	int preProcessing() {
		return 0;
	}

	int passReferenceGenome(int chromatidSequenceId, Vector<String> chromatidSequence) {
		return 0;
	}

	Vector<String> getAlignment(int N, double normA, double normS, Vector<String> readName,
			Vector<String> readSequence) {
		Vector<String> ret = new Vector<String>(N);
		for (int i = 0; i < N; i++) {
			String qname = "sim" + (1 + i / 2) + '/' + ((i % 2 == 0) ? '2' : '1');
			ret.set(i, qname + ",20,1,150,+,0");
		}
		return ret;
	};

	/**
	 * Constants from the problem statement
	 */
	static int MAX_POSITION_DIST = 300;
	static double NORM_A_SMALL = -3.392;
	static double NORM_A_MEDIUM = -3.962;
	static double NORM_A_LARGE = -2.710;
	static double MAX_AUC = 0.999999d;

	/**
	 * Position: describe the position of a read within the genome
	 */
	public static class Position

	{

		public Position(int rname, int from, int to, char strand) {
			super();
			this.rname = rname;
			this.from = from;
			this.to = to;
			this.strand = strand;
		}

		int rname;
		int from;
		int to;
		char strand;
	};

	/**
	 * ReadResult: result of a read alignment
	 */
	static class ReadResult

	{
		
		public ReadResult(double confidence, int r) {
			super();
			this.confidence = confidence;
			this.r = r;
		}
		double confidence;
		int r;

	};

	/**
	 * Split a comma-separated string into a vector of string
	 * 
	 * @param row
	 *            the string to be split
	 * @return the vector of string
	 */
	static String[] tokenize(final String row) {
		String[] tokens = row.split(",");
		return tokens;
	}

	/**
	 * Read a minisam file and build a map of ground truth
	 * 
	 * @param path
	 *            the path of the minisam file storing the ground truth
	 * @return a map[read_name] = read_Position
	 * @throws IOException 
	 * @throws NumberFormatException 
	 */
	static Map<String, Position> parse_truth(String path) throws NumberFormatException, IOException {
		Map<String, Position> res = new HashMap<String, Position>();
		BufferedReader br = new BufferedReader(new FileReader(path));
		String s;
		int cutoffIndex = 0;
		while ((s = br.readLine()) != null) {
			cutoffIndex++;
			if(cutoffIndex>CUTOFF*2) break;
			String[] tokens = tokenize(s);
			String qname = tokens[0];
			int chromatid = Integer.parseInt(tokens[1]);
			int from = Integer.parseInt(tokens[2]);
			int to = Integer.parseInt(tokens[3]);
			char strand = tokens[4].charAt(0);
			res.put(qname, new Position(chromatid, from, to, strand));

		}
		return res;
	}

	/**
	 * For each string of the results vector, build a read result {confidence,
	 * r}
	 * 
	 * @param truth
	 *            the map of ground truth position for each read
	 * @param results
	 *            the vector of results as return by getAlignment
	 * @return a vector of ReadResult, that is {confidence, r}
	 */
	
	static List<ReadResult> build_read_results(final Map<String, Position> truth, final String[] results) {
		List<ReadResult> read_results = new LinkedList<ReadResult>();
		int n = results.length;
		int correct = 0;
		for(int i=0; i<n; ++i) {
			if(results[i]==null) break;
			String[] tokens = tokenize(results[i]);
			Position p = truth.get(tokens[0]);
			Position position = p;
			//const Position& position = p.second;
			int r = 1;
			r = (Integer.parseInt(tokens[1])==position.rname) ? r : 0;
			r = (tokens[4].charAt(0)==position.strand) ? r : 0;
			int start0 = Integer.parseInt(tokens[2]);
			int start1 = position.from;
			if(r!=0 && start0==start1) {
				System.out.println("Exact match:"+tokens[0]);
			}
			
			r = (Math.abs(start0-start1)<MAX_POSITION_DIST) ? r : 0;
			double confidence = Double.parseDouble(tokens[5]);
			read_results.add(new ReadResult(confidence, r));
			correct += r;
		}
		System.out.println("Number of correct answers: " + correct + '/' + n + " = " + (double)correct/(double)n);
		return read_results;
	}
	

	/**
	 * Compute the accuracy given the {confidence, r} pairs and the
	 * normalization facto
	 * 
	 * @param read_results
	 *            a vector of {confidence, r} results
	 * @param norm_a
	 *            as described in the problem statement
	 * @return a double, the computed accuracy
	 */
	
	
	static double compute_accuracy(List<ReadResult> read_results, double norm_a) {
		int n = read_results.size();
		Collections.sort(read_results, new Comparator<ReadResult>() {

			@Override
			public int compare(ReadResult o1, ReadResult o2) {
				return Double.compare(o1.confidence, o2.confidence);
			}});
		// merge results of equal confidence
		List<Integer> cumul_si = new LinkedList<Integer>();
		cumul_si.add(read_results.get(0).r);
		List<Integer> pos = new LinkedList<Integer>();
		pos.add(0);
		for(int i=1; i<n; ++i) {
			int lastIndex = cumul_si.size()-1;
			if(read_results.get(i).confidence==read_results.get(i-1).confidence) {
				int val = cumul_si.get(lastIndex);
				val+=read_results.get(i).r;
				cumul_si.set(lastIndex,val);
				pos.set(pos.size()-1, i);
			} else {
				
				int cumul = cumul_si.get(lastIndex) + read_results.get(i).r;
				cumul_si.add(cumul);
				pos.add(i);
			}
		}
		// compute the AuC
		double auc = 0.0;
		double invn = 1.0 / (double)n;
		double invnp1 = 1.0 / (double)(n+1);
		double lfmultiplier = 1.0 / Math.log(n+1);
		int m = cumul_si.size();
		for(int i=0; i<m; ++i) {
			double fi = 1.0 * (2+pos.get(i) - cumul_si.get(i))  * invnp1;
			double fi1 = (i==m-1) ? 1.0 : 1.0 * (2+pos.get(i+1) - cumul_si.get(i+1)) * invnp1;
			double lfi = lfmultiplier * Math.log(fi);
			double lfi1 = lfmultiplier * Math.log(fi1);
			auc += cumul_si.get(i) * (lfi1 - lfi) * invn;
		}
		System.out.println("auc = +"+auc);
		double tmp = Math.log(1 - Math.min(auc, MAX_AUC));
		System.out.println("log(1 - min(auc, MAX_AUC)) = "+tmp);
		System.out.println("NormA = "+norm_a);
		double accuracy = tmp / norm_a;
		System.out.println("accuracy = "+accuracy);
		return accuracy;
	}

	/**
	 * Perform a single test
	 * 
	 * @param testDifficulty
	 *            define the test type (SMALL=0, MEDIUM=1, LARGE=2)
	 * @return alignments in format specified in the problem statement
	 * @throws IOException
	 */
	static String[] perform_test(int testDifficulty, double norm_a, IDNASequencing dna_sequencing) throws IOException {
		// test data path and description
		String fa1_path = null, fa2_path = null;
		List<Integer> chr_ids = new LinkedList<Integer>();
		if (testDifficulty == 0) {
			fa1_path = "data/TrainingSmall/small5.fa1";
			fa2_path = "data/TrainingSmall/small5.fa2";
			chr_ids.add(20);
		} else if (testDifficulty == 1) {
			fa1_path = "data/TrainingMedium/medium5.fa1";
			fa2_path = "data/TrainingMedium/medium5.fa2";
			// chr_ids = vector<int>{1,11,20};
			chr_ids.add(1);
			chr_ids.add(11);
			chr_ids.add(20);
		} else if (testDifficulty == 2) {
			fa1_path = "../data/large5.fa1";
			fa2_path = "../data/large5.fa2";
			for (int i = 1; i <= 24; ++i)
				chr_ids.add(i);
		}
		// call the MM DNASequencing methods
		dna_sequencing.initTest(testDifficulty);
		// load chromatid
		for (int chromatid_seq_id : chr_ids) {
			List<String> chromatid_seq = new LinkedList<String>();
			String path = "data/chromatids/chromatid" + chromatid_seq_id + ".fa";

			BufferedReader br = new BufferedReader(new FileReader(path));
			String s;
			// skip header
			s = br.readLine();
			System.out.println("Skip header: " + s);
			// pack all lines in chromatid_seq
			while ((s = br.readLine()) != null) {
				// if(s.back()=='\r') s.pop_back();
				chromatid_seq.add(s);
			}
			dna_sequencing.passReferenceGenome(chromatid_seq_id, chromatid_seq.toArray(new String[0]));
			br.close();
		}
		dna_sequencing.preProcessing();

		// load reads
		List<String> read_seq = new LinkedList<String>();
		List<String> read_id = new LinkedList<String>();
		BufferedReader br1 = new BufferedReader(new FileReader(fa1_path));
		BufferedReader br2 = new BufferedReader(new FileReader(fa2_path));
		String s1, s2;
		int cutoffIndex = 0;
		
		while ((s1 = br1.readLine()) != null && (s2 = br2.readLine()) != null) {
			cutoffIndex++;
			if(cutoffIndex>CUTOFF) break;
			// if (s1.back() == '\r')
			// s1.pop_back();
			// if (s2.back() == '\r')
			// s2.pop_back();
			read_id.add(s1.substring(1, s1.length()));
			read_id.add(s2.substring(1, s2.length()));
			s1 = br1.readLine();
			s2 = br2.readLine();
			// if (s1.back() == '\r')
			// s1.pop_back();
			// if (s2.back() == '\r')
			// s2.pop_back();
			read_seq.add(s1);
			read_seq.add(s2);
		}
		br1.close();br2.close();
		int nreads = read_id.size();
		// compute alignments
		String[] results = dna_sequencing.getAlignment(nreads, norm_a, 0.5, read_id.toArray(new String[0]),
				read_seq.toArray(new String[0]));
		return results;

	}

	/**
	 * Main function: read the data, perform the DNA alignments and score
	 * results
	 * 
	 * @throws IOException
	 */

	public static void main(String[] args) throws IOException {
		if(true) {
			researchMain(args);
		} else {
			sequencingMain(args);
		}
	}
	
	public static void researchMain(String[] args) throws IOException {
		final int testDifficulty = 0;
		String minisam_path = null;
		double norm_a;
		if (testDifficulty == 0) {
			minisam_path = "data/TrainingSmall/small5.minisam";
			norm_a = NORM_A_SMALL;
		} else if (testDifficulty == 1) {
			minisam_path = "data/TrainingMedium/medium5.minisam";
			norm_a = NORM_A_MEDIUM;
		} else if (testDifficulty == 2) {
			minisam_path = "../data/large5.minisam";
			norm_a = NORM_A_LARGE;
		}
		String[] results = perform_test(testDifficulty, norm_a, new DNAResearch());
		//Map<String, Position> truth = parse_truth(minisam_path);
		
		
		
		
		
	}
	
	
	public static void sequencingMain(String[] args) throws IOException {
		final int testDifficulty = 0;
		String minisam_path = null;
		double norm_a;
		if (testDifficulty == 0) {
			minisam_path = "data/TrainingSmall/small5.minisam";
			norm_a = NORM_A_SMALL;
		} else if (testDifficulty == 1) {
			minisam_path = "data/TrainingMedium/medium5.minisam";
			norm_a = NORM_A_MEDIUM;
		} else if (testDifficulty == 2) {
			minisam_path = "../data/large5.minisam";
			norm_a = NORM_A_LARGE;
		}
		// perform test
		String[] results = perform_test(testDifficulty, norm_a, new DNASequencing());
		// load truth
		Map<String, Position> truth = parse_truth(minisam_path);
		List<ReadResult> read_results = build_read_results(truth, results);
		// scoring
		double accuracy = compute_accuracy(read_results, norm_a);
	}
}
