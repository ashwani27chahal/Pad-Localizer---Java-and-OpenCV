package Feb03Wavelets;

import java.io.File;
import java.util.Arrays;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;





public class PadLocalizer {
	
	static final String LOCATION = "E:/Spring 2017 courses/Wavelets - 6810/assignment 4/Images/";
	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		PadLocalizer.localizePadInDir(LOCATION + "OriginalDir/", LOCATION + "GrassDir/", LOCATION + "OutputDir/");
	}
	
	public static void localizePadInDir(String originalDir, String grassDir, String outputDir) 
	{		
//		System.out.println(originalDir);
		File originalFolder = new File(originalDir);
		File grassFolder = new File(grassDir);

		File[] originalImageFile = originalFolder.listFiles();
		File[] grassImageFile = grassFolder.listFiles();
		
//		System.out.println(grassImageFile.length);

		Arrays.sort(originalImageFile);
		Arrays.sort(grassImageFile);

		for (int i = 0; i < grassImageFile.length; i++) {
//			 System.out.println(originalImageFile[i].getAbsolutePath());
			PadLocalizer.localizePad(originalImageFile[i].getAbsolutePath(),grassImageFile[i].getAbsolutePath(), outputDir+ originalImageFile[i].getName());
					
					
		}
	}
	

	public static void localizePad(String originalImageFile, String grassImageFile, String outputImageFile) 
	{

			Mat grass = Highgui.imread(grassImageFile);
			Mat original = Highgui.imread(originalImageFile);
			if (grass.rows() == 0 || grass.cols() == 0) {
				throw new IllegalArgumentException("Failed to read "
						+ grassImageFile);
			}
			
//			System.out.println("PRINTING THE ROWS AND COLUMNS IN THE FILE" + grass.rows() + " cols: " + grass.cols() );
//			rows = 480 and cols = 720
//			taking bin values  as 10 and 30 for rows and columns respectively
			
			int col_offset = 30;  //bins of 40 pixels for column traversal of the image		
			double[] array_vertical = computeVerticalProjection(grass, col_offset);
						
//			converting the signal to the nearest power of 2
			int numOfVerticalBins = (int) Math.pow((int) 2, (int) (Math.log(array_vertical.length) / Math.log(2)));
			
			double[] verticalProjection = new double[numOfVerticalBins];
			System.out.println("signal before HAAR");
			for(int i=0; i<numOfVerticalBins; i++){
				verticalProjection[i] = array_vertical[i];
				System.out.print(verticalProjection[i] + " ");
			}
			System.out.println();

				
				//ID HWT on vertical projection of the image
			   OneDHWT.orderedFastHaarWaveletTransformForNumIters(verticalProjection, 2);
			   System.out.println("signal after HAAR");
			   int padStartCol = 0;
			   int padEndCol = 0;
				for(int i=4; i<8; i++){
					System.out.print(verticalProjection[i] + " ");
					if (verticalProjection[i] > 30.0){
						padStartCol = i-3; //removing first four step function values
					}
					if (verticalProjection[i] < -30.0){
						padEndCol = i-3; //removing first four step function values
					}  
				}
				System.out.println();
				// SIGNAL'S COEFFICIENTS AFTER HAAR 2nd iteration
//				-37.75 53.0 -16.0 -93.0 

				
				padStartCol = (((padStartCol*4) * col_offset) - (3 * col_offset)); //taking the left most bin's for this quarter
				padEndCol = (((padEndCol*4) * col_offset) - col_offset); //taking the right most bin's start for this quarter
//				System.out.println(padStartCol);
//				System.out.println(padEndCol);
				
				
				
				int row_offset = 5; //bins of 10 pixels for row traversal of the image
				double[] array_horizontal = computeHorizontalProjection(grass, row_offset);
				
//				converting the signal to the nearest power of 2
				int numOfHorizontalBins = (int) Math.pow((int) 2, (int) (Math.log(array_horizontal.length) / Math.log(2)));
				
				double[] horizontalProjection = new double[numOfHorizontalBins];
				System.out.println("signal before HAAR");
				for(int i=0; i<numOfHorizontalBins; i++){
					horizontalProjection[i] = array_horizontal[i];
					System.out.print(horizontalProjection[i] + " ");
				}
				System.out.println();
//				System.out.println(horizontalProjection.length);  -- 64

				
				//ID HWT on horizontal projection of the image
				   OneDHWT.orderedFastHaarWaveletTransformForNumIters(horizontalProjection, 2);
				   System.out.println("signal after HAAR");
				   int padStartRow = 0;
				   int padEndRow = 0;
				   int max_index = 0;
				   double max_val = 0;
					for(int i=16; i<32; i++){
						System.out.print(horizontalProjection[i] + " ");
						if (horizontalProjection[i] > max_val){
							max_val = horizontalProjection[i];
							max_index = i-15;
						}
						
					}
					
					padStartRow = max_index*row_offset*4;
					System.out.println();
					System.out.println(padStartRow);
					System.out.println(padEndRow);
			        Point topleft     = new Point(padStartCol, (padStartRow-15));
			        Point topright    = new Point(padEndCol, padStartRow );
			        Point bottomleft  = new Point(padStartCol, (padStartRow+10) );
			        Point bottomright = new Point(padEndCol, (padStartRow+30));
					final double[] EDGE = {0, 255, 0};
			        Scalar color = new Scalar(EDGE);
		    		drawLineInMat(original, topleft, topright, color, 2);
		    		drawLineInMat(original, topright, bottomright, color, 2);
		    		drawLineInMat(original, bottomright, bottomleft, color, 2);
		    		drawLineInMat(original, bottomleft, topleft, color, 2);
			        Highgui.imwrite(outputImageFile, original);
		        	grass.release();
		        	original.release();
				
	}

	private static double[] computeVerticalProjection(Mat grass, int col_offset) {
		
		double[] arrayVertical = new double[grass.cols()/col_offset];
		int index = 0;
		
		for(int i = 0; i <  grass.cols(); i = i+col_offset){
			double total_pixels_in_bin = 0;
			for (int col = i; col < i+col_offset; col++) {
				double sum_col = 0;				
				for (int row = 0; row < grass.rows(); row++) {
					if (grass.get(row, col)[0] >= 200.0){
						sum_col = sum_col + 1.0;
					}
				}
				total_pixels_in_bin = total_pixels_in_bin + sum_col;
			}
			arrayVertical[index] = total_pixels_in_bin;
			index++;
		}
			
		
		return arrayVertical;
	}

	
	private static double[] computeHorizontalProjection(Mat grass, int row_offset) {

		double[] arrayHorizontal = new double[grass.rows()/row_offset];
		int index = 0;
		
		for(int i = 0; i <  grass.rows(); i = i + row_offset){
			double total_pixels_in_bin = 0;
			for (int row = i; row < i + row_offset; row++) {
				double sum_row = 0;				
				for (int col = 0; col < grass.cols(); col++) {
					if (grass.get(row, col)[0] >= 200.0){
						sum_row = sum_row + 1.0;
					}
				}
				total_pixels_in_bin = total_pixels_in_bin + sum_row;
			}
			arrayHorizontal[index] = total_pixels_in_bin;
			index++;
		}
			
		
		return arrayHorizontal;
	}


    public static void drawLineInMat(Mat mat, Point start_point, Point end_point,
            Scalar color, int line_width) 
    {
        Core.line(mat, start_point, end_point, color, line_width);
    }


}
