package kr.hs.sshs.AndroidPTS.logic;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;

import java.util.List;
import java.util.ArrayList;

import kr.hs.sshs.AndroidPTS.ui.MainActivity;

import com.googlecode.javacv.FFmpegFrameGrabber;
import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.FrameGrabber.Exception;
import com.googlecode.javacv.FrameRecorder;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class CPU {
	/// Path to store resources
	public static String PATH = "/mnt/sdcard/ptsmain/";

	/// FFmpeg variables
	static FrameGrabber grabber;
	static FrameRecorder recorder;

	/// IplImage variables
	IplImage imgTmpl;	// template image (RGB)
	IplImage imgTmpl_prev;	// template image - 1 frame ago
	IplImage imgBW;		// template blackwhite image
	IplImage imgBlob;	// Blob detection image
	IplImage imgCandidate;	// Candidate image
	IplImage imgResult;	// result image
	IplImage imgBall; //Ball Image

	/// Width and height of original frame
	static CvSize _size;
	static int width;
	static int height;

	/// Video infos
	public static int framelength;	// Video frame length
	public static int framecount = 1;	// Current frame number

	/// Indicates whether a candidate is found
	static boolean balldetermined = false;
	public static boolean foundBall = false;

	final static int ballthresh = 9;
	
	int[][] binary;

	/// Stores all Candidate objects
	List<Candidate> ballCandidates = new ArrayList<Candidate>();//Candidate Storage
	Candidate detectedball;
	
	/// Indicates current CPU change
	public static final int STATE_FOUND_BALL = 0; // (Most important)
	public static final int STATE_DETECT_VALUE_CHANGE = 1;
	public static final int STATE_BLOB_LABELING = 2;
	public static final int STATE_BLOB_FILTERING = 3;
	public static final int STATE_CANDIDATE_PROCESSING = 4;
	public static final int STATE_BLOB_STAMPING = 5;	
	
	MainActivity.MyHandler mh;
	
	/*
	 * Optimized color threshold examples
	 *  1. (0,0,180,0),(255,64,255,0)
	 */

	/**
	 * Constructor
	 * @throws Exception 
	 */
	public CPU(MainActivity.MyHandler handler_ex) throws Exception {
		mh = handler_ex;
		
		// recorder = new FFmpegFrameRecorder(PATH + "video/trash.mp4", 640, 480);
		// recorder.setFrameRate(30);
		// recorder.start();
		grabber = new FFmpegFrameGrabber(PATH);
		grabber.start();

		// Get frame size and length
		//framelength = grabber.getLengthInFrames();
		framelength = 53;
		_size = cvGetSize(grab());
		width = _size.width();
		height = _size.height();

		// Initialize IplImages
		// (DO NOT RELEASE THESE --- intialized only 1 time, reused)
		imgTmpl_prev = cvCreateImage(_size, IPL_DEPTH_8U, 3);
		imgBall = cvCreateImage(_size,IPL_DEPTH_8U,1);
		cvCopy(grab(), imgTmpl_prev);

		imgCandidate = cvCreateImage(_size, IPL_DEPTH_8U, 1);
	}

	/**
	 * Process 1 frame, and then return the result IplImage.
	 * @return bypass Whether to bypass processing and jump 5 frame or not
	 */
	public IplImage process(boolean bypass) throws InterruptedException, Exception, com.googlecode.javacv.FrameRecorder.Exception {
		System.out.println("Welcome to the world of CPU!!!");
		
		// Refresh
		imgTmpl = cvCreateImage(_size, IPL_DEPTH_8U, 3);
		imgCandidate = cvCreateImage(_size, IPL_DEPTH_8U, 1);
		cvSetImageCOI(imgTmpl, 0);

		cvCopy(grab(), imgTmpl);
		cvSmooth(imgTmpl, imgTmpl, CV_GAUSSIAN, 3);

		if (bypass) {
			return imgTmpl;	// And quit the code immediately, bypassing processImage()
		}
		// Process image!
		System.out.println("Thinking.....");
		processImage();
		cvCopy(imgTmpl, imgTmpl_prev);

		System.out.println("############## FRAME " + framecount
				+ " ##############");

		return imgCandidate;
	}
	
	/**
	 * Process - no bypassing
	 */
	public IplImage process() throws Exception, InterruptedException, com.googlecode.javacv.FrameRecorder.Exception {
		return process(false);
	}
	
	public IplImage getTmpl() {
		return imgTmpl;
	}

	public static IplImage grab() throws com.googlecode.javacv.FrameGrabber.Exception {
		framecount++;
		return grabber.grab();
	}

	public static void pause() throws InterruptedException {
	}

	public void moveToFrame(int frameNumber) throws com.googlecode.javacv.FrameGrabber.Exception {
		int temp = framecount;
		for (int i=1; i<frameNumber-temp; i++)
			grab();
	}

	public void jumpFrames(int frameJump) throws com.googlecode.javacv.FrameGrabber.Exception {
		for (int i=0; i<frameJump-1; i++)
			grab();
	}

	public void loadImage() {
		imgTmpl = cvLoadImage(PATH + "template.jpg");

		// Check if image is present
		if (imgTmpl == null) {
			System.out.println("Failed to load template image!");
			System.exit(0);
		}
	}

	public void stretch() {
		IplImage imgBW = cvCreateImage(_size, IPL_DEPTH_8U, 1);
		cvCvtColor(imgTmpl, imgBW, CV_RGB2GRAY);
		imgResult = cvCreateImage(_size, IPL_DEPTH_8U, 1);

		// CvHistogram hist = new CvHistogram();
		// cvCalcHist(imgBW, hist, 0, null);
		// (int) cvGetMinMaxHistValue(hist, min_value, 0, null, null);
		
		int temp = 0;
		for (int y=0; y<height; y++) {
			for (int x=0; x<width; x++) {
				temp = (int) ( ((int)cvGetReal2D(imgBW, y, x) - 180.0)*255.0/(255.0-180) );
				cvSetReal2D(imgBW, y, x, (temp>=0)?temp:0);
			}
		}

		cvCopy(imgBW, imgResult);

		cvReleaseImage(imgBW);
	}

	/**
	* Process the image!
	*/
	public void processImage() {		
		width = imgTmpl.width();
		height = imgTmpl.height();

		imgBlob = cvCreateImage(_size, IPL_DEPTH_8U, 1);
		imgBW = cvCreateImage(_size, IPL_DEPTH_8U, 1);
		imgResult = cvCreateImage(_size, IPL_DEPTH_8U, 1);
		cvCvtColor(imgTmpl, imgBW, CV_RGB2GRAY);
		
		binary = new int[width][height];

		Blob_Labeling bl;
		List<Info> blobs;
		IplImage imgRecovery;

		mh.sendMessage(mh.obtainMessage(STATE_DETECT_VALUE_CHANGE));
		/// DETECTING VALUE CHANGE		
		SatChangeDetect scd = new SatChangeDetect();
		scd.initialize(imgTmpl_prev, imgTmpl);
		binary = scd.detectChange();

		mh.sendMessage(mh.obtainMessage(STATE_BLOB_LABELING));
		/// BLOB LABELING
		bl = new Blob_Labeling();
		blobs = bl.detectBlob(binary, width, height);// DETECT BLOB
		binary = bl.print;
		/// BLOB LABELING END

		mh.sendMessage(mh.obtainMessage(STATE_BLOB_FILTERING));
		///
		/// BLOB FILTERING
		blobFiltering(blobs, 3);
		///
		///

		mh.sendMessage(mh.obtainMessage(STATE_CANDIDATE_PROCESSING));
		/// CANDIDATE PROCESS START
		// Adding new blobs into existing Candidates --
		// get each Candidate, add a new center at the end of it,
		// and then put it onto the top of the ballCandidates
		for (int q = ballCandidates.size()-1; q>=0; q--) {
			Candidate cc = new Candidate(ballCandidates.get(q));

			boolean addedBlob = false; // Indicates whether any blob is added to Candidate cc 
			for (Info blob : blobs) { // FOUND BLOB

				if (cc.xROImin() < blob.xcenter() && cc.xROImax() > blob.xcenter() && cc.yROImin()<blob.ycenter() && cc.yROImax() > blob.ycenter()) { //ROI Thresholding
					System.out.println("Appending!!!!!!!!!!! in Candidate" + q);
					cc.numOfMissingBlobs = 0;
					// if (cc.centers.get(cc.centers.size()-1).count<20 || cc.countmin() < blob.count && cc.countmax() > blob.count) { //Size Thresholding : if blob is small, no application of size threshold
					addedBlob = true;
					ballCandidates.add(new Candidate(cc));
					System.out.println("THERE ARE " + ballCandidates.size() + " CANDIDATES");
					ballCandidates.get(ballCandidates.size()-1).add(new Simple(new CvPoint(blob.xcenter(),blob.ycenter()),blob.count));
					//}
				}
			}

			if (!addedBlob) { // NOT FOUND BLOB

				if (cc.numOfMissingBlobs > 0) { // If this blob should be removed
					if(balldetermined){
						if(ballCandidates.size()==1){
							// HERE
							mh.sendMessage(mh.obtainMessage(STATE_FOUND_BALL));
							foundBall = true;
							
							cc.centers.remove(cc.centers.size() - 1);
							detectedball = new Candidate(cc);
							drawBall();
							System.out.println("The Speed of Pitch is " + 300/detectedball.centers.size() + "m/s");
						}
					}
					// Do nothing, let this blob removed (not added)
				} else {						

					// SUCCESS!!
					ballCandidates.add(new Candidate(cc)); // auto-updated
					ballCandidates.get(ballCandidates.size()-1).addMissed();


					/*
					// Also SUCCESS!
					cc.addMissed();
					ballCandidates.add(new Candidate(cc));	
					 */	
					System.out.println("遊먯� �잛닔 : " + cc.numOfMissingBlobs);
				}
			}

			ballCandidates.remove(q); // Remove original Candidate
		}

		if(ballCandidates.size()==0){
			balldetermined=false;
		}

		for (Candidate cc : ballCandidates) {
			if(cc.centers.size()>=ballthresh){
				balldetermined=true;
				cc.survive=true;
			}
		}

		if(balldetermined){
			for (int q = ballCandidates.size()-1; q>=0; q--) {
				if(!ballCandidates.get(q).survive){
					ballCandidates.remove(q);
				}
			}
			System.out.println("BALL IS FOUND");
		}
		// Finding the FIRST ball
		if(!balldetermined){
			for (Info blob : blobs) {
				if (blob.count>=65) {
					ballCandidates.add(new Candidate(blob)); //New Candidate
					System.out.println("NEW CANDIDATE WAS CREATED");
				}
			}
		}

		candidateLengthCheck();
		System.out.println("THERE ARE " + ballCandidates.size() + " CANDIDATES NOW"); //Print Candidate Number
		drawCandidate(); //Create IplImage for view
		/// CANDIDATE PROCESS END

		/*
		mh.sendMessage(mh.obtainMessage(STATE_BLOB_STAMPING));
		/// BLOB STAMPING
		// Stamp blobs list onto imgRecovery
		// (Doesn't need any IplImage variable)
		imgRecovery = cvCreateImage(_size, IPL_DEPTH_8U, 1);

		for (int y = 0; y<height; y++) {
			for (int x = 0; x<width; x++) {
				cvSetReal2D(imgRecovery, y, x, 0);
			}
		}

		/*
		for (Info i : blobs) {
			for (CvPoint p : i.points) {
				// System.out.println("Point : " + p.x() + ", " + p.y());
				cvSetReal2D(imgRecovery, p.y(), p.x(), 255);
			}
		}

		cvCopy(imgRecovery, imgResult);
		cvReleaseImage(imgRecovery);
		/// BLOB STAMPING END
		 */
		// Check Blob Detecting -- end
	}
	
	/**
	* 
	* BLOB FILTERING
	* Search for points in the square box near a blob --
	* if there is any, that blob is not likely the ball.
	* @param blobs Blobs list
	* @param adjBlobNumThreshold Minimum number of found adjacent blobs required to remove current blob.
	*/
	public void blobFiltering(List<Info> blobs, int adjBlobNumThreshold) {
		// Thickness of the searching box, wrapping around each blob
		// (set 0 for testing)
		int boxThickness = 20;


		int currentLabel = 0; // Label of the current searching blob
		for (int i = blobs.size() - 1; i > 0 ; i--) { // CAUTION -- No element in blobs.get(0) (background)
			if (blobs.size() > 0) {
				// System.out.println("Searching blob number " + (i+1) + "...");

				Info currentBlob = blobs.get(i);
				int x = currentBlob.xcenter();
				int y = currentBlob.ycenter();

				int boxwidth = boxThickness*2 + currentBlob.bwidth();
				int boxheight = boxThickness*2 + currentBlob.bheight();

				/*
				System.out.println("WIDTH	: " + currentBlob.bwidth() + "\nHEIGHT	: " + currentBlob.bheight());
				System.out.println("SIZE	: " + currentBlob.count);
				System.out.println("POS	: (" + x + ", " + y + ")");
				*/

				currentLabel = i;	

				// Searching inside the box
				List<Integer> detectedLabel = new ArrayList<Integer> (); // Labels detected inside the box
				for (int yi = y - boxheight/2; yi < y + boxheight/2; yi++) {
					for (int xi = x - boxwidth/2; xi < x + boxwidth/2; xi++) {
						int ysearch = (int)((yi<0)?Math.max(yi,0):Math.min(yi,height-1));
						int xsearch = (int)((xi<0)?Math.max(xi,0):Math.min(xi,width-1));

						// int ysearch = 50;
						// int xsearch = -1;
						// System.out.println("Searching (" + xsearch + ", " + ysearch + ")");
						int label = binary[xsearch][ysearch]; // Lable of the current searching point
						if (label > 0) {
							boolean alreadyDetected = false;

							for (Integer l : detectedLabel) {
								if (label == l) {
									alreadyDetected = true;
								}
							}

							if (!alreadyDetected) {
								detectedLabel.add(label);
							}
						}
					}
				}

				if (detectedLabel.size() >= adjBlobNumThreshold) {
					blobs.remove(i);
					System.out.println("!!!!!!!!!!REMOVED!!!!!!!!!!");
				} else {
					System.out.println("-----------SAVED-----------");
				}
			}
		}
	}

	/**
	* Draw centers in all Candidate objects.
	*/
	public void drawCandidate() {
		// Reset to black screen
		for (int i = 0; i < imgCandidate.width(); i++) {
			for (int j = 0; j < imgCandidate.height(); j++) {
				cvSetReal2D(imgCandidate,j,i,0);
			}
		}
		
		for (int k = 0; k < ballCandidates.size(); k++) {
			for (Simple pt : ballCandidates.get(k).centers) {
				for (int i = -1; i <= 1; i++) {
					for (int j = -1; j <= 1; j++) {
						int ydraw = (int)((pt.ctr.y()+i<0)?Math.max(pt.ctr.y()+i,0):Math.min(pt.ctr.y()+i,height-1));
						int xdraw = (int)((pt.ctr.x()+j<0)?Math.max(pt.ctr.x()+j,0):Math.min(pt.ctr.x()+j,width-1));
						
						cvSetReal2D(imgCandidate, ydraw, xdraw, 255);
					}
				}
			}
		
			// Draw ROI box`
			Candidate cc = ballCandidates.get(k);
			int xl = (int)((cc.xROImin()<0)?Math.max(cc.xROImin(),0):Math.min(cc.xROImin(),width-1));
			int xr = (int)((cc.xROImax()<0)?Math.max(cc.xROImax(),0):Math.min(cc.xROImax(),width-1));
			int yu = (int)((cc.yROImax()<0)?Math.max(cc.yROImax(),0):Math.min(cc.yROImax(),height-1));
			int yd = (int)((cc.yROImin()<0)?Math.max(cc.yROImin(),0):Math.min(cc.yROImin(),height-1));
			
			for(int i = xl;i<=xr;i++){
				cvSetReal2D(imgCandidate,yu,i,255);
				cvSetReal2D(imgCandidate,yd,i,255);
			}
			for(int i = yd;i<=yu;i++){
				cvSetReal2D(imgCandidate,i,xl,255);
				cvSetReal2D(imgCandidate,i,xr,255);
			}
			//cvSetReal2D(imgCandidate,penpoint,255);
		}
	}
	
	public void drawBall() {
		// Reset to black screen
		for (int i = 0; i < imgBall.width(); i++) {
			for (int j = 0; j < imgBall.height(); j++) {
				cvSetReal2D(imgBall,j,i,0);
			}
		}
		
		for (Simple pt : detectedball.centers) {
			for (int i = -1; i <= 1; i++) {
				for (int j = -1; j <= 1; j++) {
					int ydraw = (int)((pt.ctr.y()+i<0)?Math.max(pt.ctr.y()+i,0):Math.min(pt.ctr.y()+i,height-1));
					int xdraw = (int)((pt.ctr.x()+j<0)?Math.max(pt.ctr.x()+j,0):Math.min(pt.ctr.x()+j,width-1));
					
					cvSetReal2D(imgBall, ydraw, xdraw, 255);
				}
			}
		}
	}
	
	
	
	/**
	* Check each Candidates' length, and remove too short ones
	*/
	public void candidateLengthCheck() {
		for (int i = ballCandidates.size()-1; i>=0; i--) {
			Candidate cd = ballCandidates.get(i);
			int frames = cd.centers.size(); //Candidate elapsed frame
			if (frames>=3 && frames<=6) {
				int ruler = Math.abs(cd.centers.get(frames-1).ctr.x()-cd.centers.get(frames-2).ctr.x());
				if (Math.abs(cd.centers.get(0).ctr.x()-cd.centers.get(frames-1).ctr.x()) < (frames-1)*ruler*0.9) { //If Track of Candidate is not long enough along the x axis
					ballCandidates.remove(i); //Delete the Candidate
					System.out.println("SHORT CANDIDATE WAS REMOVED");
				}
			}
			if (frames==2) {
				// cd.determineDirection(); //Refer Candidate.java
			}
		}
	}
	
	public void candidateJumpingCheck() {
		for (int i = ballCandidates.size()-1; i>=0; i--) {
			Candidate cd = ballCandidates.get(i);
			int frames = cd.centers.size(); //Candidate elapsed frame
			if (frames>=3) {
				int lastmove = cd.centers.get(frames-1).ctr.x()-cd.centers.get(frames-2).ctr.x();
				int prevmove = cd.centers.get(frames-2).ctr.x()-cd.centers.get(frames-3).ctr.x();
				if(prevmove*lastmove<0){
					if(prevmove>5){
						ballCandidates.remove(i);
						System.out.println("JUMPING CANDIDATE WAS REMOVED");
					}
					else if(prevmove>2){
						if(Math.abs(lastmove)>=(Math.abs(prevmove)/2)){
							ballCandidates.remove(i);
							System.out.println("JUMPING CANDIDATE WAS REMOVED");
						}
					}
					else{
						if(Math.abs(lastmove)>Math.abs(prevmove)){
							ballCandidates.remove(i);
							System.out.println("JUMPING CANDIDATE WAS REMOVED");
						}
					}
				}
				if(prevmove*lastmove>0){
					if(prevmove<5){
						if(Math.abs(lastmove)>=(Math.abs(prevmove)*1.3)){
							ballCandidates.remove(i);
							System.out.println("JUMPING CANDIDATE WAS REMOVED");
						}
					}
				}
				else if(prevmove==0){
					if(Math.abs(lastmove)>=5) {
						ballCandidates.remove(i);
						System.out.println("JUMPING CANDIDATE WAS REMOVED");
					}
				}
			}
		}
	}
	
	public double dis(CvPoint pt1, CvPoint pt2){
		return Math.sqrt( (pt1.x()-pt2.x())*(pt1.x()-pt2.x()) + (pt1.y()-pt2.y())*(pt1.y()-pt2.y()));
	}

	/**
	* Release all redundant resources.
	 * @throws com.googlecode.javacv.FrameRecorder.Exception 
	*/
	public void release() throws com.googlecode.javacv.FrameRecorder.Exception, Exception {
		cvReleaseImage(imgBlob);
		cvReleaseImage(imgResult);
		cvReleaseImage(imgBW);
		cvReleaseImage(imgTmpl);
		cvReleaseImage(imgCandidate);

		recorder.stop();
		recorder.release();
		grabber.stop();
		grabber.release();

		System.out.println("(TERMINATED)");
	}
}
