package cancer.trojo.preprocess;

import static org.bytedeco.javacpp.opencv_core.BORDER_DEFAULT;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgcodecs.CV_LOAD_IMAGE_COLOR;
import static org.bytedeco.javacpp.opencv_imgcodecs.CV_LOAD_IMAGE_GRAYSCALE;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.bytedeco.javacpp.opencv_imgcodecs.imwrite;
import static org.bytedeco.javacpp.opencv_imgcodecs.*;
import static org.bytedeco.javacpp.opencv_imgproc.ADAPTIVE_THRESH_GAUSSIAN_C;
import static org.bytedeco.javacpp.opencv_imgproc.GaussianBlur;
import static org.bytedeco.javacpp.opencv_imgproc.Laplacian;
import static org.bytedeco.javacpp.opencv_imgproc.THRESH_BINARY;
import static org.bytedeco.javacpp.opencv_imgproc.THRESH_BINARY_INV;
import static org.bytedeco.javacpp.opencv_imgproc.THRESH_OTSU;
import static org.bytedeco.javacpp.opencv_imgproc.THRESH_TRUNC;
import static org.bytedeco.javacpp.opencv_imgproc.adaptiveThreshold;
import static org.bytedeco.javacpp.opencv_imgproc.calcHist;
import static org.bytedeco.javacpp.opencv_imgproc.resize;
import static org.bytedeco.javacpp.opencv_imgproc.threshold;
import static org.bytedeco.javacpp.opencv_imgproc.*;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.WindowConstants;

import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Range;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.indexer.FloatBufferIndexer;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.OpenCVFrameConverter;

import cancer.trojo.execute.Executor;


public class Preprocess {
	public static String BASE_ROOT = "TS";
	public static String PROCESSED_ROOT = "processed";
	public static String OUTPUT_ROOT = "output";
	static AtomicInteger counter = new AtomicInteger(0);

	// public static String OUTPUT_ROOT = "output/";
	public static void main(String[] args) {
		//Mat mat = imread(BASE_ROOT+"i1.png",CV_LOAD_IMAGE_GRAYSCALE);
		// show(mat, "source");
		// deNoiseSelf(mat, 1);
		// imwrite(OUTPUT_ROOT+"i1-denoise.png",mat);
		// show(mat, "denoised");
		// Mat tresh = treshold(mat);
		// show(tresh,"tresh");
		// imwrite(OUTPUT_ROOT+"i1-tresh.png",tresh);
		// Mat sharp = sharpen(mat, 1000000000);
		// imwrite(OUTPUT_ROOT+"i1-sharpen.png",sharp);
		// show(sharp, "sharp");

		processInputs();

	}

	public static void processInputs() {
		File folder = new File(BASE_ROOT);
		File[] listOfFiles = folder.listFiles();
		ExecutorService pool = Executors.newFixedThreadPool(Executor.threadPoolSize);
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				pool.execute(new ProcessCommand(listOfFiles[i]));
			}
		}
		pool.shutdown();
		try {
			pool.awaitTermination(10000, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static class ProcessCommand implements Runnable {
		File file;

		public ProcessCommand(File file) {
			this.file = file;
		}

		public void run() {
			processInput(file);
		}
	}

	public static String appendFileName(String fileName, String add) {
		return fileName.substring(0, fileName.length()-4)+add+".png";
	}
	
	public static void processInput(File file) {
		//processInputCropColor(file);
		//processHistogram(file);
		processRotateHalfSize(file);
	}
	
	public static void processInputCropGray(File file) {
		int counter0 = counter.incrementAndGet(); 
		System.out.println(counter0+".Processing File " + file.getAbsolutePath());
		Mat image = imread(file.getAbsolutePath(), CV_LOAD_IMAGE_GRAYSCALE);
		
		int width = image.cols();
		int height = image.rows();
		int cropSize = 360;
		
		Mat toSave = image.apply(new Range((height-cropSize)/2,(height+cropSize)/2), new Range((width-cropSize)/2,(width+cropSize)/2));
		resize(toSave, toSave, new Size(cropSize/2, cropSize/2));
		String toSavePath = PROCESSED_ROOT + "/" + appendFileName(file.getName(), "_crop");
		File tmp = new File(toSavePath);
		imwrite(tmp.getAbsolutePath(), toSave);
		toSave.release();
		
		
		/*String toSavePath = OUTPUT_ROOT + "/gray_" + file.getName();
		File tmp = new File(toSavePath);
		System.out.println(counter0+".To save " + tmp.getAbsolutePath());
		imwrite(tmp.getAbsolutePath(), image);*/
		
		
		image.release();

	}
	
	public static void processHistograGray(File file) {
		int counter0 = counter.incrementAndGet(); 
		System.out.println(counter0+".Processing File " + file.getAbsolutePath());
		Mat image = imread(file.getAbsolutePath(), CV_LOAD_IMAGE_GRAYSCALE);
		
		int width = image.cols();
		int height = image.rows();
		System.out.println(width+":" + height+":"+image.channels()+":"+image.depth());
		String toSavePath = PROCESSED_ROOT + "/" + appendFileName(file.getName(), "_hist2");
		
		
		Mat output = new Mat();
		//Mat output = new Mat(0,0,CV_8UC3);
		int channels[] = {1};
		int[] histSize = {33};
		float[] histRange = {0,255f};
		//float[] ranges = {0f,255f,0f,255f,0f,255f};
		//MatOfFl
		
		
		IntPointer intPtrChannels = new IntPointer(1);
		IntPointer intPtrHistSize = new IntPointer(32);
		//val histRange = Array(_minRange, _maxRange)
		PointerPointer<FloatPointer> ptrPtrHistRange = new PointerPointer<FloatPointer>(histRange);
		
		
		
		calcHist(image, 1, intPtrChannels, new Mat(), output, 3, intPtrHistSize, ptrPtrHistRange, true, false );
		//calcHist(image, 1, channels, new Mat(), output, 1, histSize, histRange, true, false );
		System.out.println(output);
		//show(output, "Lolo");
		//File tmp = new File(toSavePath);
		//System.out.println("toSavePath:"+tmp.getAbsolutePath());
		//imwrite(tmp.getAbsolutePath(), output);
		image.release();
		output.release();
		
		
		/*String toSavePath = OUTPUT_ROOT + "/gray_" + file.getName();
		File tmp = new File(toSavePath);
		System.out.println(counter0+".To save " + tmp.getAbsolutePath());
		imwrite(tmp.getAbsolutePath(), image);*/
		
		System.exit(0);
		//image.release();
	}
	
	public static void processHistogram(File file) {
		int counter0 = counter.incrementAndGet(); 
		System.out.println(counter0+".Processing File " + file.getAbsolutePath());
		Mat image = imread(file.getAbsolutePath(), CV_LOAD_IMAGE_COLOR);
		
		int width = image.cols();
		int height = image.rows();
		System.out.println(width+":" + height+":"+image.channels()+":"+image.depth());
		String toSavePath = PROCESSED_ROOT + "/" + appendFileName(file.getName(), "_hist2");
		
		
		Mat output = new Mat();
		//Mat output = new Mat(0,0,CV_8UC3);
		//int channels[] = {0, 1, 2};
		//int[] histSize = {32, 32, 32};
		float[] histRange = {0,255f};
		//float[] ranges = {0f,255f,0f,255f,0f,255f};
		//MatOfFl
		
		
		IntPointer intPtrChannels = new IntPointer(3);
		IntPointer intPtrHistSize = new IntPointer(10,10,10);
		//val histRange = Array(_minRange, _maxRange)
		PointerPointer<FloatPointer> ptrPtrHistRange = new PointerPointer<FloatPointer>(histRange,histRange,histRange);
		//PointerPointer<FloatPointer> ptrPtrHistRange = new PointerPointer<FloatPointer>(histRange);
		
		
		
		//calcH
		
		calcHist(image, 1, intPtrChannels, new Mat(), output, 3, intPtrHistSize, ptrPtrHistRange, true, false );
		System.out.println(output+":"+output.createIndexer().getClass());
		FloatBufferIndexer indx = (FloatBufferIndexer)output.createIndexer();
		int ii = 0;
		while(true) {
			System.out.println(ii+":"+indx.get(ii));
			ii++;
			if(false)break;
		}
		//show(output, "Lolo");
		//File tmp = new File(toSavePath);
		//System.out.println("toSavePath:"+tmp.getAbsolutePath());
		//imwrite(tmp.getAbsolutePath(), output);
		image.release();
		output.release();
		
		
		/*String toSavePath = OUTPUT_ROOT + "/gray_" + file.getName();
		File tmp = new File(toSavePath);
		System.out.println(counter0+".To save " + tmp.getAbsolutePath());
		imwrite(tmp.getAbsolutePath(), image);*/
		
		System.exit(0);
		//image.release();
	}
	
	
	

	public static void processHistogram0(File file) {
		int counter0 = counter.incrementAndGet(); 
		System.out.println(counter0+".Processing File " + file.getAbsolutePath());
		Mat image = imread(file.getAbsolutePath(), CV_LOAD_IMAGE_COLOR);
		
		int width = image.cols();
		int height = image.rows();
		
		String toSavePath = PROCESSED_ROOT + "/" + appendFileName(file.getName(), "_hist");
		
		
		Mat output = new Mat();
		//Mat output = new Mat(0,0,CV_8UC3);
		int channels[] = {0, 1, 2};
		int[] histSize = {32, 32, 32};
		float[] ranges = {0,255f};
		//float[] ranges = {0f,255f,0f,255f,0f,255f};
		//MatOfFl
		calcHist(image, 1, channels, new Mat(), output, 3, histSize, ranges, true, false );
		File tmp = new File(toSavePath);
		
		imwrite(tmp.getAbsolutePath(), output);
		image.release();
		output.release();
		
		
		/*String toSavePath = OUTPUT_ROOT + "/gray_" + file.getName();
		File tmp = new File(toSavePath);
		System.out.println(counter0+".To save " + tmp.getAbsolutePath());
		imwrite(tmp.getAbsolutePath(), image);*/
		
		
		//image.release();
	}
	static int minRows = Integer.MAX_VALUE;
	static int minCols = Integer.MAX_VALUE;
	static Object synco = new Object();
	
	public static void processRotateHalfSize(File file) {
		int counter0 = counter.incrementAndGet(); 
		System.out.println(counter0+".Processing File " + file.getAbsolutePath());
		Mat image = imread(file.getAbsolutePath(), CV_LOAD_IMAGE_COLOR);
		
		int width = image.cols();
		int height = image.rows();
		if(height>width) {
			Mat trans = new Mat();
			transpose(image, trans);
			image = trans;
			width = image.cols();
			height = image.rows();
		}

		resize(image, image, new Size(width/2, height/2));
		String toSavePath = PROCESSED_ROOT + "/" + appendFileName(file.getName(), "_half");
		File tmp = new File(toSavePath);
		imwrite(tmp.getAbsolutePath(), image);
		image.release();
		
		
		/*String toSavePath = OUTPUT_ROOT + "/gray_" + file.getName();
		File tmp = new File(toSavePath);
		System.out.println(counter0+".To save " + tmp.getAbsolutePath());
		imwrite(tmp.getAbsolutePath(), image);*/
		synchronized(synco) {
			if(minRows>height) {
				minRows = height;
			}
			if(minCols>width) {
				minCols = width;
			}
		}
		System.out.println(minRows+":"+minCols);
		
		//image.release();
	}
	
	
	
	public static void processHalfSize(File file) {
		int counter0 = counter.incrementAndGet(); 
		System.out.println(counter0+".Processing File " + file.getAbsolutePath());
		Mat image = imread(file.getAbsolutePath(), CV_LOAD_IMAGE_COLOR);
		
		int width = image.cols();
		int height = image.rows();
		resize(image, image, new Size(width/2, height/2));
		String toSavePath = PROCESSED_ROOT + "/" + appendFileName(file.getName(), "_half");
		File tmp = new File(toSavePath);
		imwrite(tmp.getAbsolutePath(), image);
		image.release();
		
		
		/*String toSavePath = OUTPUT_ROOT + "/gray_" + file.getName();
		File tmp = new File(toSavePath);
		System.out.println(counter0+".To save " + tmp.getAbsolutePath());
		imwrite(tmp.getAbsolutePath(), image);*/
		
		
		//image.release();
	}
	
	public static void processInputRotate(File file) {
		int counter0 = counter.incrementAndGet(); 
		System.out.println(counter0+".Processing File " + file.getAbsolutePath());
		Mat image = imread(file.getAbsolutePath(), CV_LOAD_IMAGE_COLOR);
		
		int width = image.cols();
		int height = image.rows();
		if(width<height) {
			//flip
//			/transpose
		}
		String toSavePath = PROCESSED_ROOT + "/" + appendFileName(file.getName(), "_cropColor");
		File tmp = new File(toSavePath);
		imwrite(tmp.getAbsolutePath(), image);
		image.release();
		/*String toSavePath = OUTPUT_ROOT + "/gray_" + file.getName();
		File tmp = new File(toSavePath);
		System.out.println(counter0+".To save " + tmp.getAbsolutePath());
		imwrite(tmp.getAbsolutePath(), image);*/
		image.release();

	}
	
	
	
	public static void processInputCropColor(File file) {
		int counter0 = counter.incrementAndGet(); 
		System.out.println(counter0+".Processing File " + file.getAbsolutePath());
		Mat image = imread(file.getAbsolutePath(), CV_LOAD_IMAGE_COLOR);
		
		int width = image.cols();
		int height = image.rows();
		int cropSize = 360;
		
		Mat toSave = image.apply(new Range((height-cropSize)/2,(height+cropSize)/2), new Range((width-cropSize)/2,(width+cropSize)/2));
		resize(toSave, toSave, new Size(cropSize/4, cropSize/4));
		String toSavePath = PROCESSED_ROOT + "/" + appendFileName(file.getName(), "_cropColor");
		File tmp = new File(toSavePath);
		imwrite(tmp.getAbsolutePath(), toSave);
		toSave.release();
		
		
		/*String toSavePath = OUTPUT_ROOT + "/gray_" + file.getName();
		File tmp = new File(toSavePath);
		System.out.println(counter0+".To save " + tmp.getAbsolutePath());
		imwrite(tmp.getAbsolutePath(), image);*/
		
		
		image.release();

	}

	public static Mat treshold(Mat imaz) {
		Mat result = imaz.clone();
		// threshold(imaz, result, 110d, Math.pow(2, imaz.depth()-1),
		// THRESH_TOZERO);
		threshold(imaz, result, 200d, 255d, THRESH_TRUNC);
		adaptiveThreshold(result, result, 255d, ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY, 51, 25);
		return result;
	}

	public static Mat treshold0(Mat imaz) {
		Mat result = imaz.clone();
		// threshold(imaz, result, 110d, Math.pow(2, imaz.depth()-1),
		// THRESH_TOZERO);
		threshold(imaz, result, 110d, 255d, THRESH_BINARY_INV + THRESH_OTSU);
		// adaptiveThreshold(imaz, result, 255d,
		// ADAPTIVE_THRESH_MEAN_C,THRESH_BINARY_INV, 71, 0 );
		return result;
	}

	public static Mat deNoise(Mat imaz, int size) {
		Mat result = imaz.clone();
		GaussianBlur(imaz, result, new Size(size, size), size, size, BORDER_DEFAULT);
		return result;
	}

	public static void sharpenSelf(Mat imaz, int depth) {
		Laplacian(imaz, imaz, imaz.depth(), 1, 0.5d, 0d, BORDER_DEFAULT);
	}

	public static Mat sharpen(Mat imaz, int depth) {
		Mat result = imaz.clone();
		Laplacian(imaz, result, depth);
		return result;
	}

	public static void deNoiseSelf(Mat imaz, int size) {
		GaussianBlur(imaz, imaz, new Size(size, size), size, size, BORDER_DEFAULT);
	}

	public static void show(final Mat image, final String title) {
		CanvasFrame canvas = new CanvasFrame(title, 1);
		canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		final OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
		canvas.showImage(converter.convert(image));
	}

}
