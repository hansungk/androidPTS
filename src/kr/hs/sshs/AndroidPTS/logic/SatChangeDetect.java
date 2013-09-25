package kr.hs.sshs.AndroidPTS.logic;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;

import com.googlecode.javacv.cpp.opencv_core.CvSize;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class SatChangeDetect {
	static IplImage prev_rgb, next_rgb, prev, next;
	final static int h_thresh = 100, s_thresh = 160, v_thresh=300, singlethresh=30;
	static int[][] h_subst, s_subst, v_subst; 
	int[][] detect;
	
	static CvSize imgsize;
	static int width;
	static int height;
	
	public void initialize(IplImage prev_in, IplImage next_in) {
		prev_rgb = cvCreateImage(cvSize(prev_in.width(),prev_in.height()),IPL_DEPTH_8U,3);
		next_rgb = cvCreateImage(cvSize(next_in.width(),next_in.height()),IPL_DEPTH_8U,3);
		
		cvCopy(prev_in, prev_rgb);
		cvCopy(next_in, next_rgb);
	}

	public int[][] detectChange(){
		prev = cvCreateImage(cvSize(prev_rgb.width(),prev_rgb.height()),IPL_DEPTH_8U,1);
		next = cvCreateImage(cvSize(next_rgb.width(),next_rgb.height()),IPL_DEPTH_8U,1);
		detect = new int[prev_rgb.width()][prev_rgb.height()];
		
		h_subst = new int[prev_rgb.width()][prev_rgb.height()];
		s_subst = new int[prev_rgb.width()][prev_rgb.height()];
		v_subst = new int[prev_rgb.width()][prev_rgb.height()];
		
		imgsize = cvGetSize(prev);
		width = imgsize.width();
		height = imgsize.height();
		
		cvCvtColor(prev_rgb, prev, CV_RGB2GRAY);
		cvSmooth(prev, prev, CV_GAUSSIAN, 3 );
		cvCvtColor(next_rgb, next, CV_RGB2GRAY);
		cvSmooth(next, next, CV_GAUSSIAN, 3 );

		for (int i=0;i<width;i++){
			for (int j=0;j<height;j++) {
				detect[i][j]=0;				
				v_subst[i][j] = (int) -(cvGetReal2D(prev,j,i)-cvGetReal2D(next,j,i)); // Examine Value difference
			}
		}
		
		for (int i=30;i<width-30;i++) {
			for (int j=30;j<height-60;j++){
				// if(compare_s(i,j)>s_thresh && compare_v(i,j)>v_thresh)
				if(v_subst[i][j]>singlethresh){
					if (compare_v(i,j)>v_thresh)
						detect[i][j]=255;
					else
						detect[i][j]=0;
				}
				else
					detect[i][j]=0;
			}
		}
		
		cvReleaseImage(prev);
		cvReleaseImage(next);
		return detect;
	}
	
	public int compare_h(int x, int y) {
		int result=0;
		for (int i=x-1;i<x+2;i++){
			for (int j=y-1;j<y+2;j++){
				result+=Math.abs(cvGet2D(prev,j,i).getVal(0)-cvGet2D(next,j,i).getVal(0));
			}
		}
		return result;
	}

	public int compare_s(int x, int y) {
		int result=0;
		for (int i=x-1;i<x+2;i++){
			for (int j=y-1;j<y+2;j++){
				result+=s_subst[i][j];
			}
		}
		return result;
	}
	
	public int compare_v(int x, int y) {
		int result=0;
		for (int i=x-1;i<x+2;i++){
			for (int j=y-1;j<y+2;j++){
				result+=v_subst[i][j];
			}
		}
		return result;
	}
}
