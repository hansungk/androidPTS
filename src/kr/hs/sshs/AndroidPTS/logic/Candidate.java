package kr.hs.sshs.AndroidPTS.logic;

import java.util.ArrayList;
import java.util.List;

import com.googlecode.javacv.cpp.opencv_core.CvPoint;

public class Candidate {
	// Coordinates of ball center positions in this Candidate
	public List<BallInfo> centers;

	int numOfMissingBlobs = 0;
	//int disturbed=0;
	public boolean justMissedABlob;

	// The last stored center
	BallInfo currentCenter;
	// The next expected point where the ball should appear in the next frame,
	// when assuming the ball lies in straight line with constant velocity
	BallInfo nextCenter;

	// Amount of x, y center coordinates shifted compared to the previous center
	int x_shift;
	int y_shift;

	// x, y width of the next ROI -- will be updated each time a new center is added
	int x_ROIWidth;
	int y_ROIWidth;

	final static int xR = 65, xL = 4; //R:Max ROI Length, L:Least ROI Length
	final static double k1 = 0.2, k2 = 1.4;
	
	boolean survive = false;

	Candidate() {
		centers = new ArrayList<BallInfo>();
		// Randomly initialize two Centers
		currentCenter = new BallInfo(new CvPoint(0, 0));
		nextCenter = new BallInfo(new CvPoint(0, 0));
		survive = false;
	}

	Candidate(BlobInfo i) {
		this();
		add(new BallInfo(new CvPoint(i.xcenter(),i.ycenter()),i.count));
		update();
	}
	
	/**
	* Clones Candidate cd to this Candidate
	*/
	Candidate(Candidate cd){
		this();

		this.numOfMissingBlobs = cd.numOfMissingBlobs;
		this.justMissedABlob = cd.justMissedABlob;
		this.survive = cd.survive;
		
		// Clone center
		for (BallInfo s : cd.centers) {
			centers.add(new BallInfo(s));
		}
		//
		
		this.currentCenter = new BallInfo(new CvPoint(cd.currentCenter.x(), cd.currentCenter.y()), cd.currentCenter.pixelcount);
		this.nextCenter = new BallInfo(new CvPoint(cd.nextCenter.x(), cd.nextCenter.y()), cd.nextCenter.pixelcount);
		
		this.x_shift = cd.x_shift;
		this.y_shift = cd.y_shift;
		this.x_ROIWidth = cd.x_ROIWidth;
		this.y_ROIWidth = cd.y_ROIWidth;
		// update();
		// new
	}
	
	public void add(BallInfo toAdd) {
		centers.add(toAdd);
		update();
	}

	/**
	* Add 'virtual' point to centers, when a blob is missed in tracking process
	* (will only be called when missing blobs, not by ordinary blobs)
	*/
	public void addMissed() {
		numOfMissingBlobs++;

		// Assume ball is flying in straight line with const speed
		CvPoint cp = new CvPoint(currentCenter.x() + x_shift, currentCenter.y() + y_shift);
		BallInfo toAdd = new BallInfo(cp, 10); // Why 10?????????????
		add(toAdd); // auto-updated
		
		//disturbed++;
		
		// Re-update ROIWidths
		//x_ROIWidth *= 2;
		//y_ROIWidth *= 2;
		//System.out.println("I MISSED " + numOfMissingBlobs + " BLOBS SO FAR");
		
		// Leave ROI variables unchanged
	}

	public void update() {
		if (centers.size() >= 2) {
			int lastIndex = centers.size() - 1;

			currentCenter = centers.get(lastIndex);

			// Update x_shift, y_shift
			// Only when there are 2 or more centers
			x_shift = centers.get(lastIndex).x()
					- centers.get(lastIndex - 1).x();
			y_shift = centers.get(lastIndex).y()
					- centers.get(lastIndex - 1).y();

			// Update nextCenter
			nextCenter.x(currentCenter.x() + x_shift);
			nextCenter.y(currentCenter.y() + y_shift);

			// Update ROI width
			if (Math.abs(x_ROIWidth) > 9)
				x_ROIWidth = Math.abs(x_shift);
			else
				x_ROIWidth = 9;
			if (Math.abs(y_shift) > 3)
				y_ROIWidth = Math.abs(y_shift);
			else
				y_ROIWidth = 3;

		} else { // Else then only initialize the ROIs
			currentCenter = centers.get(0);
			nextCenter.x(currentCenter.x());
			nextCenter.y(currentCenter.y());
			x_ROIWidth = 140;
			y_ROIWidth = 30;
			//System.out.println("ONLY one ");
		}
	}
	
	public int xROImin() {
			return nextCenter.x() - x_ROIWidth/2;
	}
	public int xROImax() {
			return nextCenter.x() + x_ROIWidth/2;
	}
	public int yROImin() {
		if(this.centers.size()<11)
			return (int) (nextCenter.y() - y_ROIWidth*1.8);
		else
			return (int) (Math.max(nextCenter.y() - y_ROIWidth*1.8,currentCenter.y() -1)); //占쏙옙占쏙옙 占쏙옙占쏙옙 占쏙옙占식울옙占쏙옙 占시라가댐옙 占쏙옙占쏙옙 占쏙옙鳴占�占쏙옙占쏙옙
	}
	public int yROImax() {
			return (int) (nextCenter.y() + y_ROIWidth*2.4);
	}

	public int countmin(){
		return (int) (k1*this.centers.get(centers.size()-1).pixelcount);
	}
	public int countmax(){
		//if(k2*this.centers.get(centers.size()-1).count<20)
		return (int) (k2*this.centers.get(centers.size()-1).pixelcount);
	}
}

class BallInfo {
	CvPoint ctr;
	int pixelcount;
	
	BallInfo(BallInfo toClone) {
		ctr = new CvPoint(toClone.ctr.x(), toClone.ctr.y());
		this.pixelcount = toClone.pixelcount;
	}
	
	BallInfo(CvPoint p) {
		this.ctr = p;
		this.pixelcount = 0;
	}

	BallInfo(CvPoint p, int i) {
		this.ctr = p;
		this.pixelcount = i;
	}

	/**
	* Returns x coord
	*/
	public int x() {
		return ctr.x();
	}

	/**
	* Sets x coord
	*/
	public void x(int x_in) {
		ctr.x(x_in);	
	}

	/**
	* Returns y coord
	*/
	public int y() {
		return ctr.y();
	}

	/**
	* Sets y coord
	*/
	public void y(int y_in) {
		ctr.y(y_in);	
	}
}
