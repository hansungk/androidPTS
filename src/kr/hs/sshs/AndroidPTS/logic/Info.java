package kr.hs.sshs.AndroidPTS.logic;

import java.util.ArrayList;
import java.util.List;

import com.googlecode.javacv.cpp.opencv_core.CvPoint;

class Info{

	int xmin, ymin, xmax, ymax, count;
	public boolean condition;
	
	Info(int xmin, int ymin, int xmax, int ymax, int count) {
		condition = false;
		this.xmin = xmin;
		this.ymin = ymin;
		this.xmax = xmax;
		this.ymax = ymax;
		this.count = count;
	}
		
	public int bwidth() {
		return xmax-xmin;
	}
	public int bheight() {
		return ymax-ymin;
	}
	
	public int xcenter() {
		return (xmax+xmin)/2;
	}
	
	public int ycenter() {
		return (ymax+ymin)/2;
	}
}
