package kr.hs.sshs.AndroidPTS.logic;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;

import java.util.List;
import java.util.ArrayList;

import kr.hs.sshs.AndroidPTS.ui.MainActivity;
import kr.hs.sshs.AndroidPTS.logic.BallInfo;
import kr.hs.sshs.AndroidPTS.logic.BlobInfo;
import kr.hs.sshs.AndroidPTS.logic.Blob_Labeling;
import kr.hs.sshs.AndroidPTS.logic.CatcherDetect;
import kr.hs.sshs.AndroidPTS.logic.ValueChangeDetect;
import kr.hs.sshs.AndroidPTS.logic.Candidate;
import kr.hs.sshs.AndroidPTS.logic.OpticalFlow;
import kr.hs.sshs.AndroidPTS.logic.FixingCenterofCatcher;

import com.googlecode.javacv.FFmpegFrameGrabber;
import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.FrameGrabber.Exception;
import com.googlecode.javacv.FrameRecorder;
import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvRect;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.CvSize;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class CPU {
	/// Path to store resources
	public static String PATH = "/mnt/sdcard/ptsmain/";

	/// FFmpeg variables
	static FrameGrabber grabber;
	//static FrameRecorder recorder;
	
	OpticalFlow opflow;
	ValueChangeDetect vcd;

	/// IplImage variables
	IplImage imgTmpl;	// template image (RGB)
	IplImage imgBW;		// template blackwhite image
	IplImage imgBW_prev;
	IplImage imgBlob;	// Blob detection image
	IplImage imgCandidate;	// Candidate image
	//IplImage imgResult;	// result image

	IplImage imgBall; //Ball Image
	IplImage imgSobel; //Sobel Image
	IplImage imgCropped;
	IplImage imgCropped2;
	IplImage imgCropped3;
	IplImage imgTemp;
	//IplImage imgTemp2;
	IplImage imgPyrA;	// PREV pyramid
	IplImage imgPyrB;	// CURR pyramid
	public IplImage imgCatcher; //Image For Catcher
	//IplImage imgMorph;
	//IplImage imgMorphSobel;

	/// Width and height of original frame
	static CvSize _size;
	static CvSize _pyrSize;
	static int width;
	static int height;
	static int cropsize = 70;
	
	//indicate strike-ball
	public int referee_state=0;

	/// Video infos
	public static int framelength;	// Video frame length
	public static int framecount = 1;	// Current frame number
	
	//Flags
	static boolean flag_OpflowInitiated = false;
	static boolean flag_D_Pressed = false;

	/// Indicates whether a candidate is found
	static boolean balldetermined = false;
	public static boolean foundBall = false;

	final static int ballthresh = 9;
	
	int[][] binary;

	/// Stores all Candidate objects
	List<Candidate> ballCandidates = new ArrayList<Candidate>();//Candidate Storage
	Candidate detectedball;
	static BallInfo	ballfinal = new BallInfo(new CvPoint(cropsize,cropsize));
	static CvRect	ballcrop;
	
	/// Final and initial data of the thrown ball
	IplImage		imgFinalCaught;
	int				detectedCandidateSize;
	CvPoint			caughtBallCtr;		// Center point of the finally caught ball	
	// IplImages of the frame in which the ball starts to fly;
	// will store probable imgFT2 and 3 at the same time by packing them into 3-element array
	List<IplImage>				imgFramesBetweenCatches;	// store all frames between ball catches
	IplImage					imgFirstThrown;
	IplImage					imgFirstThrown2;	// imgFirstThrown + 1 frame
	IplImage					imgFirstThrown3;	// imgFirstThrown + 2 frames
	// Optical flow data
	double[]		shiftThrowCatch = {-1, -1};		// -1 means not yet processed
	
	/// Indicates current CPU change
	public static final int STATE_FOUND_BALL = 0; // (Most important)
	public static final int STATE_DETECT_VALUE_CHANGE = 1;
	public static final int STATE_BLOB_LABELING = 2;
	public static final int STATE_BLOB_FILTERING = 3;
	public static final int STATE_CANDIDATE_PROCESSING = 4;
	public static final int STATE_BLOB_STAMPING = 5;
	public static final int STATE_BALL_CAUGHT = 6;
	
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
		
		opflow = new OpticalFlow();
		vcd = new ValueChangeDetect();
		
		grabber = new FFmpegFrameGrabber(PATH);
		grabber.start();

		// Get frame size and length
		framelength = grabber.getLengthInFrames();
		_size = cvGetSize(grab());
		imgTmpl = cvCreateImage(_size, IPL_DEPTH_8U, 3);
		cvCopy(grab(), imgTmpl);
		imgBW = cvCreateImage(_size, IPL_DEPTH_8U, 1);
		cvCvtColor(imgTmpl,imgBW,CV_RGB2GRAY);
		
		width = _size.width();
		height = _size.height();
		_pyrSize = new CvSize(_size.width()+8, _size.height()/3+1);

		// Initialize IplImages
		// (DO NOT RELEASE THESE --- intialized only 1 time, reused)
		imgBW_prev = cvCreateImage(_size, IPL_DEPTH_8U, 1);
		cvCopy(imgBW,imgBW_prev);
		imgBall = cvCreateImage(_size,IPL_DEPTH_8U,1);
		cvCopy(grab(), imgTmpl);
		cvCvtColor(imgTmpl,imgBW,CV_RGB2GRAY);

		imgSobel = cvCreateImage(_size, IPL_DEPTH_8U,1);
		imgPyrA = cvCreateImage(_size,IPL_DEPTH_32F,1);
		imgPyrB = cvCreateImage(_size,IPL_DEPTH_32F,1);
		imgBlob = cvCreateImage(_size, IPL_DEPTH_8U, 1);
		imgTemp = cvCreateImage(_size,IPL_DEPTH_8U,1);
		imgCandidate = cvCreateImage(_size, IPL_DEPTH_8U, 1);
		imgCatcher = cvCreateImage(_size, IPL_DEPTH_8U, 1);
		//m.imgMorphSobel = cvCreateImage(_size, IPL_DEPTH_8U,1);
		//m.imgCropped = cvCreateImage(_size, IPL_DEPTH_8U,1);
		//m.imgMorph = cvCreateImage(_size,IPL_DEPTH_8U,1);
		imgFinalCaught = cvCreateImage(_size, IPL_DEPTH_8U, 1);
		imgFirstThrown = cvCreateImage(_size, IPL_DEPTH_8U, 1);
		imgFirstThrown2 = cvCreateImage(_size, IPL_DEPTH_8U, 1);
		imgFirstThrown3 = cvCreateImage(_size, IPL_DEPTH_8U, 1);
		
		// Initialize variables used in decision
		caughtBallCtr = new CvPoint(-1, -1);	// -1 means no ball was caught yet
		imgFramesBetweenCatches = new ArrayList<IplImage>();
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
		cvCopy(imgBW, imgBW_prev);

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

	/*public void stretch() {
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
	}*/

	/**
	* Process the image!
	*/
	public void processImage() {

		cvCvtColor(imgTmpl, imgBW, CV_RGB2GRAY);
		
		binary = new int[width][height];
		
		Blob_Labeling bl;
		List<BlobInfo> blobs;
//		IplImage imgRecovery;
		
		cvCanny(imgBW,imgSobel,80,200,3);
		CatcherDetect.main(imgSobel);

		IplImage toAdd = cvCreateImage(_size, IPL_DEPTH_8U, 1);
		cvCopy(imgBW, toAdd);
		imgFramesBetweenCatches.add(toAdd);	// Don't forget to release them all later

		mh.sendMessage(mh.obtainMessage(STATE_DETECT_VALUE_CHANGE));
		/// DETECTING VALUE CHANGE		
		if(flag_D_Pressed) {
			imgPyrA = imgPyrB;
		}
		int flag = (flag_OpflowInitiated)?((flag_D_Pressed)?0:1):2;
		System.out.println("FLAG:" + flag);
		double[] shift = opflow.processOpticalFlow(imgBW_prev, imgBW, imgPyrA, imgPyrB, flag);
		flag_OpflowInitiated = true;
		ValueChangeDetect.mX=(int)Math.round(shift[0]);
		ValueChangeDetect.mY=(int)Math.round(shift[1]);
		System.out.println(ValueChangeDetect.mX + " and " + ValueChangeDetect.mY);
		
		//new PyrA is not needed if you process next time
		flag_D_Pressed = true;
		
		/// DETECTING VALUE CHANGE
		vcd.initialize(imgBW_prev, imgBW);
		binary = vcd.detectChange();

		mh.sendMessage(mh.obtainMessage(STATE_BLOB_LABELING));
		/// BLOB LABELING
		bl = new Blob_Labeling();
		blobs = bl.detectBlob(binary, width, height);// DETECT BLOB
		binary = bl.print;
		/// BLOB LABELING END

		mh.sendMessage(mh.obtainMessage(STATE_BLOB_FILTERING));
		///
		/// BLOB FILTERING
		blobFiltering(blobs, 4);
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
			for (BlobInfo blob : blobs) { // FOUND BLOB
				
				if (cc.xROImin() < blob.xcenter() && cc.xROImax() > blob.xcenter() && cc.yROImin()<blob.ycenter() && cc.yROImax() > blob.ycenter()) { //ROI Thresholding
					//System.out.println("Appending!!!!!!!!!!! in Candidate" + q);
					if (cc.centers.get(cc.centers.size()-1).pixelcount<40 || cc.countmin() < blob.count && cc.countmax() > blob.count) { //Size Thresholding : if blob is small, no application of size threshold
						//if(cc.disturbed==2)
							//cc.disturbed=0;
						//else if(cc.disturbed==1)
						// cc.disturbed++;
						cc.numOfMissingBlobs = 0;
						addedBlob = true;
						ballCandidates.add(new Candidate(cc));
						//System.out.println("THERE ARE " + ballCandidates.size() + " CANDIDATES");
						ballCandidates.get(ballCandidates.size()-1).add(new BallInfo(new CvPoint(blob.xcenter(),blob.ycenter()),blob.count));
					}
				}
			}

			if (!addedBlob) { // NOT FOUND BLOB					
				if (balldetermined) { //If this ball is determined
					if(cc.numOfMissingBlobs > 0){// If ball is considered to be caught
						if(ballCandidates.size()==1){
							cc.centers.remove(cc.centers.size() - 1);
							detectedball = new Candidate(cc);
							drawBall();
							System.out.println("BALL WAS CAUGHT /nf");
							//System.out.println("The Speed of Pitch is " + 1503/detectedball.centers.size() + "km/h");
							ballfinal=detectedball.centers.get(detectedball.centers.size()-1); // last ball in the "elected" ball candidate
							detectedCandidateSize = detectedball.centers.size();
							
							// Hookup of Strike-Ball
							preprocessingStrikeBall();
							
							ValueChangeDetect.v_thresh=350;
							ValueChangeDetect.singlethresh=40;
							balldetermined=false;
							
						}
					}
					else {
						// Do nothing, let this blob get removed (not added)
						ballCandidates.add(new Candidate(cc)); // auto-updated
						ballCandidates.get(ballCandidates.size()-1).addMissed();
					}
					
				} else {
					// Non-ball candidate blob jumping : Do nothing, let this blob removed (not added)
					//System.out.println("Candidate deletion by missing blob");			
				}
			}

			ballCandidates.remove(q); // Remove original Candidate
		}
		
		if(ballCandidates.size()==0){
			balldetermined=false;	// Of course!
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
			
			int x1=Math.max(0,ballCandidates.get(0).xROImin());
			int x2=Math.min(width-1,ballCandidates.get(0).xROImax());
			int y1=Math.max(0,ballCandidates.get(0).yROImin());
			int y2=Math.min(height-1,ballCandidates.get(0).yROImax());
			
			int avg = ValAverage(new CvPoint(x1,y1), new CvPoint(x2,y2), imgBW);
			ValueChangeDetect.singlethresh = (255-avg)/8;
			ValueChangeDetect.v_thresh = (255-avg);
			
			//System.out.println("BALL IS DETERMINED");
		}
		
		// Finding the FIRST ball
		if(!balldetermined){
			for (BlobInfo blob : blobs) {
				if (blob.count>=45) {
					ballCandidates.add(new Candidate(blob)); //New Candidate
					//System.out.println("NEW CANDIDATE WAS CREATED");
				}
			}
		}

		//candidateLengthCheck();
		
		//Important but need more improvements...
		/*if(balldetermined)
			ballJumpingCheck();*/
		
		//System.out.println("THERE ARE " + ballCandidates.size() + " CANDIDATES NOW"); //Print Candidate Number
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
	public void blobFiltering(List<BlobInfo> blobs, int adjBlobNumThreshold) {

		// Thickness of the searching box, wrapping around each blob
		// (set 0 for testing)
		int boxThickness = 20;

		//int currentLabel = 0; // Label of the current searching blob
		for (int i = blobs.size() - 1; i > 0 ; i--) { // CAUTION -- No element in blobs.get(0) (background)
			if (blobs.size() > 0) {
				// System.out.println("Searching blob number " + (i+1) + "...");

				BlobInfo currentBlob = blobs.get(i);
				int x = currentBlob.xcenter();
				int y = currentBlob.ycenter();

				int boxwidth = boxThickness*2 + currentBlob.bwidth();
				int boxheight = boxThickness*2 + currentBlob.bheight();

				/*
				System.out.println("WIDTH	: " + currentBlob.bwidth() + "\nHEIGHT	: " + currentBlob.bheight());
				System.out.println("SIZE	: " + currentBlob.count);
				System.out.println("POS	: (" + x + ", " + y + ")");
				*/

				// Remove the current blob, to get it out of the way
				//currentLabel = i;	

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
				} else {
				}

				// Recover the current blob for the next search,
				// using its own original label (currentLabel)
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
			for (BallInfo pt : ballCandidates.get(k).centers) {
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
		
		for (BallInfo pt : detectedball.centers) {
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
	
	public void ballJumpingCheck() {

		for (int i = ballCandidates.size() - 1; i >= 0; i--) {
			boolean caught = false;
			Candidate cd = ballCandidates.get(i);
			int frames = cd.centers.size(); // Candidate elapsed frame
			int lastxmove = cd.centers.get(frames - 1).ctr.x() - cd.centers.get(frames - 2).ctr.x();
			int prevxmove = cd.centers.get(frames - 2).ctr.x() - cd.centers.get(frames - 3).ctr.x();
			int lastymove = cd.centers.get(frames - 1).ctr.y() - cd.centers.get(frames - 2).ctr.y();
			int prevymove = cd.centers.get(frames - 2).ctr.y() - cd.centers.get(frames - 3).ctr.y();
			double angmove = Math.abs(Math.atan2(lastymove,lastxmove)-Math.atan2(prevymove,prevxmove));
			if(angmove>Math.PI)
				angmove=2*Math.PI-angmove;
			//if(Math.atan2(prevymove,prevxmove)>Math.PI /*&& cd.disturbed==0*/){
				if(angmove>Math.PI/6){
					caught = true;
				//System.out.println("ang");
				}
			//}
			if(!caught /*&& cd.disturbed==0*/){
				if (prevxmove * lastxmove < 0) {
					if (Math.abs(lastxmove) > Math.abs(prevxmove)) {
						caught = true;
						//System.out.println("minus");
					}
				}
				if (prevxmove * lastxmove > 0) {
					if (Math.abs(lastxmove) >= (Math.max(Math.abs(prevxmove)*2 , 4))) {
						caught = true;
						//System.out.println("plus");
					}
				}
				else if (prevxmove == 0) {
					if (Math.abs(lastxmove) >= 4) {
						caught = true;
						//System.out.println("zero");
					}
				}
			}
			
			if (caught) {
				if (ballCandidates.size() == 1) {
					ballCandidates.get(0).centers.remove(ballCandidates.get(0).centers.size() - 1);
					detectedball = new Candidate(ballCandidates.get(0));
					drawBall();
					System.out.println("BALL WAS CAUGHT /j");
					//System.out.println("The Speed of Pitch is " + 1080/detectedball.centers.size() + "km/h");
					ballfinal=detectedball.centers.get(detectedball.centers.size()-1);
					detectedCandidateSize = detectedball.centers.size();
					
					/// Another place where ball is caught
					preprocessingStrikeBall();
					
					ballCandidates.remove(0);
					ValueChangeDetect.v_thresh=350;
					ValueChangeDetect.singlethresh=40;
					balldetermined=false;
					
				}
				else {
					ballCandidates.remove(i);
				}
			}
		}
	}
	
	/**
	 * Pre-process for Strike-Ball decision.<br>
	 * - Find final ball position<br>
	 * - Store first and last frame of ball flying<br>
	 * - Store 2 more frames right after the first frame, for accurate catcher detection
	 * @return Background shift between the ball-thrown frame and the ball-caught frame
	 */
	public double[] preprocessingStrikeBall() {
		// Store the last state of the ball
		cvCopy(imgBW, imgFinalCaught);
		caughtBallCtr = ballfinal.ctr;
		// Find ball-thrown frame, "+1, "+2
		int framesago = detectedCandidateSize+1;	// assume there was NOT a ball jumping at the end
													// (therefore last ball caught was currframe-2
		int thrownFrameIndex = (imgFramesBetweenCatches.size()-1) - framesago;
		cvCopy(imgFramesBetweenCatches.get(thrownFrameIndex),imgFirstThrown);
		cvCopy(imgFramesBetweenCatches.get(thrownFrameIndex+1),imgFirstThrown2);
		cvCopy(imgFramesBetweenCatches.get(thrownFrameIndex+2),imgFirstThrown3);
		
		// Clear frames list
		for (IplImage il : imgFramesBetweenCatches)	cvReleaseImage(il);
		imgFramesBetweenCatches.clear();
		
		double[] shift = opflow.processOpticalFlow(imgFirstThrown, imgFinalCaught, imgPyrA, imgPyrB, 2);
		System.out.println("xshift: " + shift[0]);
		System.out.println("yshift: " + shift[1]);
		shiftThrowCatch = shift;
		
		cvCanny(imgFirstThrown,imgFirstThrown,80,200,3);
		cvCanny(imgFirstThrown2,imgFirstThrown2,80,200,3);
		cvCanny(imgFirstThrown3,imgFirstThrown3,80,200,3);
		
		ballcrop = new CvRect(Math.max(ballfinal.x()-cropsize-(int)Math.round(shift[0]),0), Math.max(ballfinal.y()-cropsize-(int)Math.round(shift[1]),0), Math.min(2*cropsize,2*(width-ballfinal.x()-1)), Math.min(2*cropsize,2*(height-ballfinal.y()-1)));
		cvSetImageROI(imgFirstThrown, ballcrop);
		imgCropped = cvCreateImage(cvGetSize(imgFirstThrown),IPL_DEPTH_8U,1);
		cvCopy(imgFirstThrown,imgCropped);
		cvResetImageROI(imgFirstThrown);
		cvSetImageROI(imgFirstThrown2, ballcrop);
		imgCropped2 = cvCreateImage(cvGetSize(imgFirstThrown2),IPL_DEPTH_8U,1);
		cvCopy(imgFirstThrown2,imgCropped2);
		cvResetImageROI(imgFirstThrown2);
		cvSetImageROI(imgFirstThrown3, ballcrop);
		imgCropped3 = cvCreateImage(cvGetSize(imgFirstThrown3),IPL_DEPTH_8U,1);
		cvCopy(imgFirstThrown3,imgCropped3);
		cvResetImageROI(imgFirstThrown3);
		cvCopy(imgBW,imgCatcher);
		cvSetImageROI(imgCatcher,ballcrop);
		CvPoint Catcher = FixingCenterofCatcher.findCatcher(imgCropped, imgCropped2, imgCropped3, new CvPoint((int)Math.round(shift[0]), (int)Math.round(shift[1])), caughtBallCtr, cropsize, imgCatcher);
		if(Catcher == null){
			System.out.println("Sorry. We couldn't recognize the catcher.");
			cvResetImageROI(imgCatcher);
			return null;
		}
		
		Catcher.x(Catcher.x()+ballfinal.x()-cropsize-(int)Math.round(shift[0]));
		Catcher.y(Catcher.y()+ballfinal.y()-cropsize-(int)Math.round(shift[1]));
		System.out.println("x : " + Catcher.x() + " y : " + Catcher.y());
		
		cvResetImageROI(imgCatcher);
		
		if(caughtBallCtr.x()>Catcher.x()-8 && caughtBallCtr.x()<Catcher.x()+8 && caughtBallCtr.y()>Catcher.y()-12 && caughtBallCtr.y()<Catcher.y()+12){
			referee_state=1;
		}
		else{
			referee_state=2;
		}
		
		//Printing Center of Catcher
		//cvRectangle(imgCatcher,new CvPoint(Math.max(Catcher.x()-10,0),Math.max(Catcher.y()-15,0)),new CvPoint(Math.min(Catcher.x()+10,width-1),Math.min(Catcher.y()+15,height-1)),new CvScalar(100,100,100,0),1,8,0);
		
		//Printing Location of Ball
		cvRectangle(imgCatcher,new CvPoint(Math.max(caughtBallCtr.x()-3,0),Math.max(caughtBallCtr.y()-3,0)),new CvPoint(Math.min(caughtBallCtr.x()+3,width-1),Math.min(caughtBallCtr.y()+3,height-1)),new CvScalar(255,255,255,0),1,8,0);
		//cvRectangle(imgCatcher,new CvPoint(50,50),new CvPoint(100,100),new CvScalar(180,180,180,0),1,8,0);
		
		cvReleaseImage(imgCropped);
		cvReleaseImage(imgCropped2);
		cvReleaseImage(imgCropped3);
		
		mh.sendMessage(mh.obtainMessage(STATE_BALL_CAUGHT));
		MainActivity.printCatcher = true;
		
		return shift;
	}
	
	public double dis(CvPoint pt1, CvPoint pt2){
		return Math.sqrt( (pt1.x()-pt2.x())*(pt1.x()-pt2.x()) + (pt1.y()-pt2.y())*(pt1.y()-pt2.y()));
	}

	/**
	* Release all redundant resources.
	 * @throws com.googlecode.javacv.FrameRecorder.Exception 
	*/
	public void release() throws com.googlecode.javacv.FrameRecorder.Exception, Exception {
		cvReleaseImage(imgTmpl);
		cvReleaseImage(imgBW);
		cvReleaseImage(imgBW_prev);
		cvReleaseImage(imgBlob);
		cvReleaseImage(imgCandidate);
		//cvReleaseImage(imgResult);
		cvReleaseImage(imgBall);
		cvReleaseImage(imgSobel);
		if(imgCropped != null)
			cvReleaseImage(imgCropped);
		cvReleaseImage(imgTemp);
		//cvReleaseImage(imgTemp2);
		cvReleaseImage(imgPyrA);
		cvReleaseImage(imgPyrB);
		cvReleaseImage(imgFinalCaught);
		cvReleaseImage(imgFirstThrown);
		cvReleaseImage(imgFirstThrown2);
		cvReleaseImage(imgFirstThrown3);
		cvReleaseImage(imgCatcher);

		//recorder.stop();
		//recorder.release();
		grabber.stop();
		grabber.release();

		System.out.println("(TERMINATED)");
	}
	
	public String doubleArrayToString(double[] ds) {
		String result = "";
		for(int i=0; i<ds.length; i++) {
			result += ds[i] + " ";
		}
		return result;
	}
	
	public int ValAverage(CvPoint a, CvPoint b, IplImage bw){
		
		int sum=0;
		for(int x=a.x(); x<b.x()+1; x++){
			for(int y=a.y(); y<b.y()+1; y++){
				sum+=cvGetReal2D(bw, y, x);
			}
		}
		
		return (int) (sum/((b.x()-a.x()+1)*(b.y()-a.y()+1)));
	}
}
