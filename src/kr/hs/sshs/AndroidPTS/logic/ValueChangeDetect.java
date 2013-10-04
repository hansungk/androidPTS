package kr.hs.sshs.AndroidPTS.logic;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;

import com.googlecode.javacv.cpp.opencv_core.CvSize;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class ValueChangeDetect {

	static IplImage prev, next;
	static int /*h_thresh = 100, s_thresh = 160,*/ v_thresh=350, singlethresh=40;
	static int[][] /*h_subst, s_subst,*/ v_subst; 
	int[][] detect;
	static int mX=0, mY=0;
	
	static CvSize imgsize;
	static int width;
	static int height;
	
	public void initialize(IplImage prev_in, IplImage next_in) {
		prev = cvCreateImage(cvSize(prev_in.width(),prev_in.height()),IPL_DEPTH_8U,1);
		next = cvCreateImage(cvSize(next_in.width(),next_in.height()),IPL_DEPTH_8U,1);
		
		cvCopy(prev_in, prev);
		cvCopy(next_in, next);
	}

	public int[][] detectChange(){
		detect = new int[prev.width()][prev.height()];
		
		//h_subst = new int[prev_rgb.width()][prev_rgb.height()];
		//s_subst = new int[prev_rgb.width()][prev_rgb.height()];
		v_subst = new int[prev.width()][prev.height()];
		
		imgsize = cvGetSize(prev);
		width = imgsize.width();
		height = imgsize.height();
		
		cvSmooth(prev, prev, CV_GAUSSIAN, 3 );
		cvSmooth(next, next, CV_GAUSSIAN, 3 );
		
		for(int x=0; x<width; x++){
			for(int y=0; y<height; y++){
				if((mX>0 && x<mX) || (mY>0 && y<mY) || (mX<0 && x>=width+mX) || (mY<0 && y>=height+mY)) {
					v_subst[x][y]=0;
				}
				else{
					int s = (int) (cvGetReal2D(next,y,x)-cvGetReal2D(prev,y-mY,x-mX));
					v_subst[x][y]=s;
				}
			}
		}
		
		for (int x=30;x<width-30;x++) {
			for (int y=30;y<height-60;y++){
				// if(compare_s(i,j)>s_thresh && compare_v(i,j)>v_thresh)
				if(v_subst[x][y]>singlethresh){
					if (compare_v(x,y)>v_thresh)
						detect[x][y]=255;
					else
						detect[x][y]=0;
				}
				else
					detect[x][y]=0;
			}
		}
		
		cvReleaseImage(prev);
		//cvReleaseImage(prev_rgb);
		cvReleaseImage(next);
		//cvReleaseImage(next_rgb);
		return detect;
	}
	
	/*public int compare_h(int x, int y) {
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
	}*/
	
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
