package sequencing;

public interface IDNASequencing {
	public int initTest(int testDifficulty);

	public int passReferenceGenome(int chromatid_seq_id, String[] array);

	public int preProcessing();

	public String[] getAlignment(int nreads, double norm_a, double d, String[] array, String[] array2);

}
