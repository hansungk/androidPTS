package kr.hs.sshs.AndroidPTS.logic;

import static com.googlecode.javacv.cpp.opencv_core.*;

import java.util.ArrayList;
import java.util.List;

import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.CvSize;

public class CatcherDetect {
	
	final static int rW=30, rH=33;
	static int[][] roi = new int[rW][rH];
	static int linethresh=9;
	static List<CvPoint> centers;
	
	public static List<CvPoint> main(IplImage bw) {
		
		centers = new ArrayList<CvPoint>();
		int pX=0,pY=0;
		int lMax=0, rMax=0, hMax=0;

		int lEnd[] = new int[rH];
		int rEnd[] = new int[rH];
		
		IplImage marked;
		CvSize _size;
		
		_size = cvGetSize(bw);
		marked = cvCreateImage(_size,IPL_DEPTH_8U,1);
		
		int[][] bin = new int[_size.width()][_size.height()];
		
		for(int i = 0; i < _size.height() ; i++){
			for(int j = 0; j < _size.width(); j++){
				if(cvGetReal2D(bw,i,j)>50){
					bin[j][i]=1;
					//cvSetReal2D(bw,i,j,255);
				}
				else{
					bin[j][i]=0;
					//cvSetReal2D(bw,i,j,0);
				}
			}
		}
		
		//Line ignoring
		for(int y=1; y<_size.height()-1; y++){
			int lx=0, x=0;
			while(x!=_size.width()){
				if(bin[x][y]==0) {x++; lx=x;}
				else x++;
				if(x-lx>=linethresh && (x==_size.width() || bin[x][y]==0)){
					for(int i=lx; i<x; i++){
						//bin[i][y-1]=0;
						bin[i][y]=0;
						//bin[i][y+1]=0;
						//cvSetReal2D(bw,y-1,i,0);
						//cvSetReal2D(bw,y,i,0);
						//cvSetReal2D(bw,y+1,i,0);
					}
				}
			}
		}
		
		cvCopy(bw,marked);
		
		for(;;pX++){
			
			//moving pivot((0,0) point of the ROI))
			if(pX==_size.width()-rW+1){
				pX=0;
				pY++;
			}
			if(pY==_size.height()-rH) {break;}
			
			//initiate condition
			if(bin[pX+rW/2][pY]!=1||(pY>0 && bin[pX+rW/2][pY-1]==1)) { continue;}
			else {lEnd[0]=rW/2; rEnd[0]=rW/2;}

			//System.out.println("pivot : ("+pX+","+pY+")" );
			
			//movingg ROI data
			for(int i = pX; i < pX+rW; i++){
				for(int j = pY; j < pY+rH; j++){
					roi[i-pX][j-pY]=bin[i][j];
				}
			}
			
			//temporary variable used for many purposes
			int k;
			
			//finding ends of the first line
			k = rW/2-1;
			for(int num=0; k!=-1; k--){
				if(roi[k][0]==0) {num++; if(num>=2) break; else continue;}
				bin[pX+k][pY]=2;
				lEnd[0]=k;
				lMax=k;
			}
			k = rW/2+1;
			for(int num=0; k!=rW; k++){
				if(roi[k][0]==0) {num++; continue;}
				if(num>=2) break;
				rEnd[0]=k;
				rMax=k;
			}
			
			//don't regard as head if too long
			if(lEnd[0] < rW/2-5 || rEnd[0]>rW/2+5){
				//System.out.println("toolong");
				continue;
			}
			
			boolean notCatcher = false;
			
			//default=-1
			for(k=1;k<rH;k++){
				lEnd[k]=-1;
				rEnd[k]=-1;
			}
			
			//now k means current line
			for(k=1;k<rH;k++){
				
				int m=k;
				
				//check upper line
				while(m>0 && lEnd[m-1]==-1){
					m--;
				}
				
				//too much missing upper line -> notCatcher
				if(m<=k-5){
					notCatcher=true;
					k-=5;
					//System.out.println("cut");
					break;
				}
				
				int c= lEnd[m-1]+1;
				if(c>-1 && c<rW && roi[c][k]>0) lEnd[k]=c;
				c-=4;
				if(c>-1 && c<rW && roi[c][k]>0) lEnd[k]=c;
				c++;
				if(c>-1 && c<rW && roi[c][k]>0) lEnd[k]=c;
				c+=2;
				if(c>-1 && c<rW && roi[c][k]>0) lEnd[k]=c;
				c--;
				if(c>-1 && c<rW && roi[c][k]>0) lEnd[k]=c;
				
				m=k;
				
				//check upper line
				while(m>0 && rEnd[m-1]==-1){
					m--;
				}
				
				//too much missing upper line -> notCatcher
				if(m<=k-5){
					notCatcher=true;
					k-=5;
					//System.out.println("cut");
					break;
				}
				
				c= rEnd[m-1]-1;
				if(c<rW && c>-1 && roi[c][k]>0) rEnd[k]=c;
				c+=4;
				if(c<rW && c>-1 && roi[c][k]>0) rEnd[k]=c;
				c--;
				if(c<rW && c>-1 && roi[c][k]>0) rEnd[k]=c;
				c-=2;
				if(c<rW && c>-1 && roi[c][k]>0) rEnd[k]=c;
				c++;
				if(c<rW && c>-1 && roi[c][k]>0) rEnd[k]=c;
			}
			
			//if too short in vertical
			if(notCatcher && k<2*rH/3) {
				//System.out.println("not long enough");
				continue;
			}
			hMax = k;
			
			k=rH/2;
			int n=rH/2;
			while(lEnd[k]==-1){k++; if(k==rH) {k--; break;}}
			while(rEnd[n]==-1){n++; if(n==rH) {n--; break;}}
			
			//not good shape
			/*if(lEnd[0]<lEnd[k]+2 || rEnd[0]>rEnd[n]-2) {
				//System.out.println("shape not catcher");
				continue;
			}
			
			if(Math.abs(rEnd[n]-rEnd[0]-lEnd[0]+lEnd[k])>5){
				//System.out.println("shape not so symmetric");
				continue;
			}
			
			if(rEnd[n]-lEnd[k]<10) {
				//System.out.println("shape not catcher");
				continue;
			}*/
			
			for(int i = pX+rW/4; i < pX+3*rW/4; i++){
				for(int j = pY+1; j < pY+rH/2; j++){
					bin[i][j]=2;
				}
			}
			
			//if(pX==1 && pY==0){
			for(int i = 1; i<Math.max(k, n); i++){
				if(lEnd[i]!=-1){
					cvSetReal2D(marked,i+pY,lEnd[i]+pX,120);
					if(lMax>lEnd[i]) lMax=lEnd[i];
					//if(cvGetReal2D(bw,i+pY,lEnd[i]+pX)<50) System.out.println("("+lEnd[i]+pX+","+i+pY+")");
				}
				if(rEnd[i]!=-1){
					cvSetReal2D(marked,i+pY,rEnd[i]+pX,120);
					if(rMax<rEnd[i]) rMax=rEnd[i];
					//if(cvGetReal2D(bw,i+pY,rEnd[i]+pX)<50) System.out.println("("+(rEnd[i]+pX)+","+(i+pY)+")");
				}
			}
			//}
			//cvRectangle(bw,new CvPoint(pX+lMax,pY),new CvPoint(pX+rMax,pY+hMax),new CvScalar(180,180,180,0),1,8,0);
		
			centers.add(new CvPoint(((2*pX+lMax+rMax)/2),((2*pY+hMax)/2)));
		}
	
		return centers;
		
	}
	
}
