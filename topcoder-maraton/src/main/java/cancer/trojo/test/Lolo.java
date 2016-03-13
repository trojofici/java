package cancer.trojo.test;
//import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgcodecs.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;

import java.io.File;

import javax.swing.WindowConstants;

import org.bytedeco.javacpp.indexer.*;
import org.bytedeco.javacv.*;

import static org.bytedeco.javacpp.opencv_imgcodecs.CV_LOAD_IMAGE_COLOR;
import static org.bytedeco.javacpp.opencv_imgcodecs.CV_LOAD_IMAGE_GRAYSCALE;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.bytedeco.javacpp.opencv_imgcodecs.imwrite;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.WindowConstants;

import org.apache.commons.math3.analysis.function.Minus;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Range;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.indexer.FloatBufferIndexer;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.OpenCVFrameConverter;

import cancer.trojo.preprocess.Preprocess;



public class Lolo {
	public static String RES_ROOT = "src/main/resources/";
	public static String INPUT_FOLDER = "TS";
	public static String GRAYSCALE_FOLDER = "GRAY";
	public static String OUTPUT_ROOT = "output/";
	
	public static void show(final IplImage image, final String title) {
	    final IplImage image1 = cvCreateImage(cvGetSize(image), IPL_DEPTH_8U, image.nChannels());
	    cvCopy(image, image1);
	    CanvasFrame canvas = new CanvasFrame(title, 1);
	    canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	    final OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
	    canvas.showImage(converter.convert(image1));
	}
	
	public static void main(String[] args) {
		System.out.println("Lolo");
		//makeGrayScales();
		//IplImage image =  cvLoadImage(RES_ROOT+"i1.png",CV_LOAD_IMAGE_GRAYSCALE);
		//Mat image =  imread(RES_ROOT+"i1-tresh.png",CV_LOAD_IMAGE_COLOR);
		Mat image =  imread(RES_ROOT+"i1.png",CV_LOAD_IMAGE_COLOR);
		if (image != null) {
			
			Mat df_dy = new Mat();
			int depth = CV_32F;
			Sobel(image, df_dy, depth,0 , 1);
			Mat newImage = new Mat(df_dy.rows(),df_dy.cols(), CV_8UC3);
			//TODO calcultate mint and max and add to output
			convertScaleAbs(df_dy, newImage);
			resize(image, image, new Size(image.cols()*4, image.rows()*4));
			Preprocess.show(image, "image");
			//Preprocess.show(df_dy, "sobel_y");
			//Preprocess.show(newImage, "sobel_yScaled");
			//if(true) System.exit(0);
			
			//Mat output1 =  image.clone();
			Mat output1 =  new Mat();
			Mat output2 =  image.clone();
			Mat output3 =  image.clone();
			Mat output4 =  image.clone();
			Mat gaus1 = getGaussianKernel(91, 0.5);
			Mat gaus2 = getGaussianKernel(91, 1);
			Mat gaus3 = getGaussianKernel(91, 2);
			Mat gaus4 = getGaussianKernel(91, 4);
			Mat gaus5 = getGaussianKernel(91, 8);
			//Mat gaus = gaus1.clone();
			Mat gaus = new Mat();
			subtract(gaus1, gaus2, gaus);
			filter2D(image, output1, 8,gaus);
			subtract(gaus2, gaus3, gaus);
			filter2D(image, output2, 8,gaus);
			subtract(gaus3, gaus4, gaus);
			filter2D(image, output3, 8,gaus);
			subtract(gaus4, gaus5, gaus);
			filter2D(image, output4, 8,gaus);
			MatExpr output1e = multiply(10, output1);
			output1 = output1e.asMat();
			MatExpr output2e = multiply(10, output2);
			output2 = output2e.asMat();
			MatExpr output3e = multiply(10, output3);
			output3 = output3e.asMat();
			MatExpr output4e = multiply(10, output4);
			output4 = output4e.asMat();
			resize(output1, output1, new Size(image.cols(), image.rows()));
			resize(output2, output2, new Size(image.cols(), image.rows()));
			resize(output3, output3, new Size(image.cols(), image.rows()));
			resize(output4, output4, new Size(image.cols(), image.rows()));
			Preprocess.show(output1, "gaus1");
			Preprocess.show(output2, "gaus2");
			Preprocess.show(output3, "gaus3");
			Preprocess.show(output4, "gaus3");
			/*
			Mat output1 =  image.clone();
			Mat output2 =  image.clone();
			resize(image, image, new Size(image.cols()*4, image.rows()*4));
										//kernel, sirka g, uhol na g, wavelength, elypiscity, sin shift
		  //Mat kernel = getGaborKernel(new Size(131,131), 200 , 0, 45, 250,0,CV_32F);
		  //Mat kernel = getGaborKernel(new Size(131,131), 5d, 0d, 18d, 1d,2.0d,CV_32F);	
			Mat kernel1 = getGaborKernel(new Size(131,131), 2d, 0d, 9d, 100d,Math.PI/2d,CV_32F);
			Mat kernel2 = getGaborKernel(new Size(131,131), 20d, 0d, 72d, 10d,Math.PI,CV_32F);
			System.out.println(image);
			//split
			//Mat invertcolormatrix= new Mat(image.rows(),image.cols(), image.type(), new Scalar(255,255,255,255));
			//subtract(invertcolormatrix, image, image);
		
			filter2D(image, output1, 8,kernel1);
			filter2D(image, output2, 8,kernel2);
			output1.convertTo(output1,CV_8UC3);
			output2.convertTo(output2,CV_8UC3);
			Preprocess.show(image, "source");
			Preprocess.show(output1, "gabored1");
			Preprocess.show(output2, "gabored2");
			image.release();
			output1.release();
			output2.release();*/
		}
		
	}
	
	
	public static void main0(String[] args) {
		System.out.println("Lolo");
		//makeGrayScales();
		//IplImage image =  cvLoadImage(RES_ROOT+"i1.png",CV_LOAD_IMAGE_GRAYSCALE);
		//IplImage image =  cvLoadImage(RES_ROOT+"i1-tresh.png",CV_LOAD_IMAGE_UNCHANGED);
		IplImage image =  cvLoadImage(RES_ROOT+"castle.png",CV_LOAD_IMAGE_COLOR);
		if (image != null) {
			//Mat matt = new Mat(3,3,CV_8U);
			//UByteBufferIndexer ind = (UByteBufferIndexer)matt.createIndexer();
			//CvMat kernelM  = new CvMat(matt);
			
			//CANNY
			System.out.println("before canny");
			IplImage canny =  cvCreateImage(cvGetSize(image), image.depth(), 1);
			cvCanny(image, canny, 100d, 200d);
			
			System.out.println("before sobel");
			//SOBEL
			IplImage df_dx = cvCreateImage(cvGetSize(image),image.depth(),image.nChannels()); 
			IplImage df_dy = cvCreateImage(cvGetSize(image),image.depth(),image.nChannels()); 

			cvSobel(image, df_dx, 2,0,1 );
			cvSobel(image, df_dy, 0,2,1 );
			double maxVal = Math.pow(2, image.depth());
			//cvFilter2D(image,  image2, kernelM);
			
			//detect
			//Scalar lower = new Scalar(0,0,0);
			System.out.println("before range");
			/*double[] min = new double[image.nChannels()];
			double[] max = new double[image.nChannels()];
			cvMinMaxLoc(image, min, max);
			
			System.out.println(java.util.Arrays.toString(min)+":"+java.util.Arrays.toString(max));
			
			
			CvScalar lower = new CvScalar(min[0]);
			CvScalar upper = new CvScalar((max[0]-min[0])/3d);
			System.out.println("depth:"+image.depth()+":"+image.nChannels());
			IplImage range = cvCreateImage(cvGetSize(image),image.depth(),image.nChannels());
			IplImage white = cvCreateImage(cvGetSize(image),image.depth(),image.nChannels());
			cvNot(white, white);
			IplImage range2 = cvCreateImage(cvGetSize(image),image.depth(),image.nChannels());
			cvInRangeS(image, lower, upper, range);
			cvAbsDiff(white, range, range2);*/
			//cvSubS(range, lower, range2);
	
			//show
			//show(df_dx, "imazX");
			//show(df_dy, "imazY");
			//show(canny, "canny");
			//show(range2, "range");
			//save
            cvSaveImage(OUTPUT_ROOT+"i1_dx.png", df_dx);
            cvSaveImage(OUTPUT_ROOT+"i1_dy.png", df_dy);
            cvSaveImage(OUTPUT_ROOT+"i1_canny.png", canny);
            //cvSaveImage(OUTPUT_ROOT+"i1_range.png", range2);
            cvReleaseImage(image);
            System.out.println("Smoothie");
        }
	}
	
	public static void makeGrayScales() {
		File folder = new File(INPUT_FOLDER);
		File[] listOfFiles = folder.listFiles();

		    for (int i = 0; i < listOfFiles.length; i++) {
		      if (listOfFiles[i].isFile()) {
		        //System.out.println("File " + listOfFiles[i].getName());
		    	String toSavePath = GRAYSCALE_FOLDER+"/"+listOfFiles[i].getName();
		        System.out.println("File " + listOfFiles[i].getAbsolutePath());
		        File tmp = new File(toSavePath);
		        System.out.println("To save " + tmp.getAbsolutePath());
		        IplImage image =  cvLoadImage(listOfFiles[i].getAbsolutePath(),CV_LOAD_IMAGE_GRAYSCALE);
		        
		        cvSaveImage(tmp.getAbsolutePath(), image);
		      } /*else if (listOfFiles[i].isDirectory()) {
		        System.out.println("Directory " + listOfFiles[i].getName());
		      }*/
		    }
		
	}

}
