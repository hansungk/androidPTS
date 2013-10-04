package kr.hs.sshs.AndroidPTS.logic;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;

import java.util.ArrayList;
import java.util.List;

import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;

public class FixingCenterofCatcher {

	static List<List<CvPoint>> centers;
	static int thresh = 15, thresh2=20;
	
	public static CvPoint findCatcher(IplImage img1, IplImage img2, IplImage img3, CvPoint shift, CvPoint ballfinal, int cropsize, IplImage background){
		centers = new ArrayList<List<CvPoint>>();
		centers.add(groupListMembers(CatcherDetect.main(img1),thresh,1));
		centers.add(groupListMembers(CatcherDetect.main(img2),thresh,1));
		centers.add(groupListMembers(CatcherDetect.main(img3),thresh,1));
		
		System.out.println("center1 : " + centers.get(0).size());
		System.out.println("center2 : " + centers.get(1).size());
		System.out.println("center3 : " + centers.get(2).size());
		List<CvPoint> merged = new ArrayList<CvPoint>();
		for(CvPoint pt : centers.get(0)){
			merged.add(pt);
		}
		for(CvPoint pt : centers.get(1)){
			merged.add(pt);
		}
		for(CvPoint pt : centers.get(2)){
			merged.add(pt);
		}
		
		System.out.println("merged size : " + merged.size());
		
		/*for(CvPoint pt : merged){
			//CvPoint p = new CvPoint(pt.x()+ballfinal.x()-cropsize+shift.x(),pt.y()+ballfinal.y()-cropsize+shift.y());
			//cvRectangle(background,new CvPoint(Math.max(p.x()-4,0),Math.max(p.y()-6,0)),new CvPoint(Math.min(p.x()+4,background.width()-1),Math.min(p.y()+6,background.height()-1)),new CvScalar(180,180,180,0),1,8,0);
			cvRectangle(background,new CvPoint(Math.max(pt.x()-4,0),Math.max(pt.y()-6,0)),new CvPoint(Math.min(pt.x()+4,background.width()-1),Math.min(pt.y()+6,background.height()-1)),new CvScalar(180,180,180,0),1,8,0);
			System.out.println("x : " +pt.x() +" y : " + pt.y());
		}*/
		
		merged = groupListMembers(merged,thresh2,1);
		
		System.out.println("grouped size : " + merged.size());
		
		if(merged.size()==0) return null;
		
		CvPoint CenterOfCatcher;
		CenterOfCatcher=merged.get(0);
		CvPoint ball = new CvPoint(cropsize+shift.x(),cropsize+shift.y());
		
		for(CvPoint pt : merged){
			if(dist(pt,ball)<dist(CenterOfCatcher,ball))
				CenterOfCatcher = pt;
			//CvPoint p = new CvPoint(pt.x()+ballfinal.x()-cropsize+shift.x(),pt.y()+ballfinal.y()-cropsize+shift.y());
			//cvRectangle(background,new CvPoint(Math.max(p.x()-4,0),Math.max(p.y()-6,0)),new CvPoint(Math.min(p.x()+4,background.width()-1),Math.min(p.y()+6,background.height()-1)),new CvScalar(180,180,180,0),1,8,0);
			//cvRectangle(background,new CvPoint(Math.max(pt.x()-4,0),Math.max(pt.y()-6,0)),new CvPoint(Math.min(pt.x()+4,background.width()-1),Math.min(pt.y()+6,background.height()-1)),new CvScalar(180,180,180,0),1,8,0);
			//System.out.println("x : " +pt.x() +" y : " + pt.y());
		}
		cvRectangle(background,new CvPoint(Math.max(CenterOfCatcher.x()-10,0),Math.max(CenterOfCatcher.y()-15,0)),new CvPoint(Math.min(CenterOfCatcher.x()+10,background.width()-1),Math.min(CenterOfCatcher.y()+15,background.height()-1)),new CvScalar(255,255,255,0),1,8,0);
		return CenterOfCatcher;
	}
	
	static List<CvPoint> groupListMembers(List<CvPoint> list, double disthresh, int numthresh){
		
		List<Integer> labels = new ArrayList<Integer>();
		List<CvPoint> temp = new ArrayList<CvPoint>();
		List<CvPoint> grouped = new ArrayList<CvPoint>();
		for(int q = 0; q<list.size(); q++){
			labels.add(q);
		}
		
		//Group adjacent points with same label
		for(int i = 0 ; i<list.size()-1; i++){
			CvPoint Pi = list.get(i);
			for(int j = Math.min(i+1,list.size()-1); j<list.size(); j++){
				CvPoint Pj = list.get(j);
				if(dist(Pi,Pj)<disthresh){
					labels.set(j,labels.get(i));
					System.out.println("Setting " + j +" to " + i);
				}
			}
		}
		
		int nthresh = numthresh;
		
		while(true){
			for(int q = 0; q<list.size(); q++){
				if(labels.get(q)!=q) continue;
				for(int i = 0 ; i<list.size(); i++){
					if(labels.get(i)==q) temp.add(list.get(i));
				}
				if(temp.size()>=nthresh)
					grouped.add(averagePoint(temp));
				temp.clear();
			}
			if(grouped.size()!=0 || nthresh==1) break;
			else nthresh--;
		}
		
		return grouped;
		
	}
	
	static CvPoint averagePoint(List<CvPoint> in){
		int x=0,y=0;
		
		for(int i = 0; i<in.size(); i++){
			x+=in.get(i).x();
			y+=in.get(i).y();
		}
		
		x=(int) Math.round(x/(double)in.size());
		y=(int) Math.round(y/(double)in.size());
		
		return new CvPoint(x,y);
		
	}
	
	static double dist(CvPoint a, CvPoint b){
		return Math.sqrt((a.x()-b.x())*(a.x()-b.x())+(a.y()-b.y())*(a.y()-b.y()));
	}
	
}
