package kr.hs.sshs.AndroidPTS.ui;

import static com.googlecode.javacv.cpp.opencv_highgui.cvSaveImage;

import java.io.File;

import kr.hs.sshs.AndroidPTS.logic.CPU;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.androidpts.R;
import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.FrameGrabber.Exception;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class MainActivity extends Activity implements View.OnClickListener {
	
	CPU ARMv7;
	FrameGrabber grabber;

	TextView tvCPU;
	TextView referee;
	TextView speed;
	TextView setDistance;
	ImageView iv;
	ImageView movieplay;
	Button btnProcess;
	//Button btnBypass;
	Button btnGetVideo;
	Button btnPass1Frame;
	Button btnPass5Frame;
	Button btnPass10Frame;
	Button btnStop;
	ProcessThread thread;
	//EditText etJumpFrame;
	
	IplImage result;
	IplImage movieFrame;
	
	MyHandler mh;
	
	static double progress;
	float distance;
	
	//public static boolean printCatcher = false;
	
	static int referee_flag;
	static final float ADULT=-1;
	static final float LITTLE=-2;
	boolean processingFlag = false;

	public TextView gettvCPU() {
		return this.tvCPU;
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Log.d("PASS", "Program started");
		
		SharedPreferences pref = getSharedPreferences("Preferences", 0);
		
		float distype = pref.getFloat("distance", ADULT);
		switch(Math.round(distype)){
		
		case -1:
			distance = 18.44f;
			break;
			
		case -2:
			distance = 14.02f;
			break;
			
		default:
			distance = distype;
			break;
		}

		mh = new MyHandler();
		
		iv = (ImageView) findViewById(R.id.imageView2);
		movieplay = (ImageView) findViewById(R.id.imageView1);
		tvCPU = (TextView) findViewById(R.id.textView_CPUState);
		referee = (TextView) findViewById(R.id.referee);
		speed = (TextView) findViewById(R.id.speedgun);
		setDistance = (TextView) findViewById(R.id.setDistance);
		//btnBypass = (Button) findViewById(R.id.button_Bypass);
		btnProcess = (Button) findViewById(R.id.button_Process);
		btnPass1Frame = (Button) findViewById(R.id.button_1Frame);
		btnPass5Frame = (Button) findViewById(R.id.button_5Frame);
		btnPass10Frame = (Button) findViewById(R.id.button_10Frame);
		btnGetVideo = (Button) findViewById(R.id.button_getVideo);
		btnStop = (Button) findViewById(R.id.button_stop);
		//btnBypass.setOnClickListener(this);
		btnProcess.setOnClickListener(this);
		btnGetVideo.setOnClickListener(this);
		btnPass1Frame.setOnClickListener(this);
		btnPass5Frame.setOnClickListener(this);
		btnPass10Frame.setOnClickListener(this);
		btnStop.setOnClickListener(this);
		//etJumpFrame = (EditText) findViewById(R.id.editText_JumpFrame);

		setDistance.setText(Float.toString(distance)+"m");
	}

	public void onClick(View v) {
		// IplImage read = cvCreateImage(new CvSize(640, 480), IPL_DEPTH_8U, 4);
		
		try {
			switch (v.getId()) {
			case R.id.button_getVideo:
				if(processingFlag){
					processingFlag = false;
					thread.flag=false;
				}
				Intent intent = new Intent(
                        Intent.ACTION_GET_CONTENT,      // 또는 ACTION_PICK
                        android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                intent.setType("video/*");              // 모든 이미지
                startActivityForResult(intent, 0);
                System.out.println("Loaded");
                
                break;
                
                
			case R.id.button_Process:
				Log.d("PASS", "Processing...");
				
				if(!processingFlag){
					thread = new ProcessThread();
					thread.flag = true;
					thread.start();
					processingFlag = true;
				}
				
				break;
				
			case R.id.button_1Frame:
				try {
					if(processingFlag){
						processingFlag = false;
						thread.flag=false;
					}
					movieFrame = ARMv7.jump1Frame();
					showState("");
					mh.sendMessage(mh.obtainMessage(101));
				} catch (java.lang.Exception e) {
					e.printStackTrace();
				}
				break;
				
			case R.id.button_5Frame:
				try {
					if(processingFlag){
						processingFlag = false;
						thread.flag=false;
					}
					movieFrame = ARMv7.jump5Frame();
					showState("");
					mh.sendMessage(mh.obtainMessage(101));
				} catch (java.lang.Exception e) {
					e.printStackTrace();
				}
				break;
				
			case R.id.button_10Frame:
				try {
					if(processingFlag){
						processingFlag = false;
						thread.flag=false;
					}
					movieFrame = ARMv7.jump10Frame();
					showState("");
					mh.sendMessage(mh.obtainMessage(101));
				} catch (java.lang.Exception e) {
					e.printStackTrace();
				}
				break;
				
			case R.id.button_stop:
				if(processingFlag){
					processingFlag = false;
					thread.flag=false;
				}
				showState("");
				break;

			/*case R.id.button_Bypass:		
				int jump = Integer.valueOf(etJumpFrame.getText().toString());
				ARMv7.jumpFrames(jump);
				cvSaveImage("/mnt/sdcard/result.jpg", ARMv7.process(true));
				File pic2 = new File("/mnt/sdcard", "result.jpg");		
				iv.setImageBitmap(BitmapFactory.decodeFile(pic2.getAbsolutePath()));
				
				Log.d("PASS", "Bypassed " + jump + " frames");
				tvCPU.setText(CPU.framecount + "개의 프레임을 건너뛰었습니다");
				break;*/
			}
		} catch (java.lang.Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e("STOP", e.getMessage());
		}

		// IplImage read = cvLoadImage("/mnt/sdcard/437.jpg");
		// IplImage gray = cvCreateImage(cvSize(read.width(), read.height()),
		// IPL_DEPTH_8U, 1);
		// cvCvtColor(read, gray, CV_RGB2GRAY);
		/*
		IplImage readB = cvCreateImage(cvGetSize(read), IPL_DEPTH_8U, 1);
		IplImage readG = cvCreateImage(cvGetSize(read), IPL_DEPTH_8U, 1);
		IplImage readR = cvCreateImage(cvGetSize(read), IPL_DEPTH_8U, 1);
		cvSplit(read, readR, readG, readB, null);
		IplImage readA = cvCreateImage(cvGetSize(read), IPL_DEPTH_8U, 1); // alpha
		cvSet(readA, new CvScalar(256), null);

		IplImage readBGRA = cvCreateImage(cvGetSize(read), IPL_DEPTH_8U, 4);
		cvMerge(readB, readG, readR, readA, readBGRA);

		Bitmap temp = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888);
		temp.copyPixelsFromBuffer(readBGRA.getByteBuffer());
		iv.setImageBitmap(temp);

		Log.w("FFmpeg", "Reached end FROM BUTTON");
		*/
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 0)
			if (resultCode == Activity.RESULT_OK) {
				Uri selectedVideo = data.getData();
	      // TODO Do something with the select image URI
				CPU.PATH=getRealPathFromURI(selectedVideo);
			}
		try {
			ARMv7 = new CPU(mh);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e("STOP", e.getMessage());
		}
	}
	
	public String getRealPathFromURI(Uri contentUri){
		String[] proj = {MediaStore.Video.Media.DATA };
		Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
		int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
		cursor.moveToFirst();
		return cursor.getString(column_index);
	}

	public void showState(String state) {
		tvCPU.setText("(" + CPU.framecount + " frame, " + Math.round((float)CPU.framecount*100.0/CPU.framelength) + "%) - " + state);
	}
	
	public void refereeState() {
		
		speed.setText("Speed : " + 1350/ARMv7.detectedball.centers.size() + " km/h");
		
		switch (ARMv7.referee_state) {
		
		case 0 : 
			referee.setText("");
			break;
		
		case 1:
			referee.setText("STRIKE");
			break;
			
		case 2:
			referee.setText("BALL");
			break;
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	public boolean onPrepareOptionsMenu(Menu menu){
		
		SharedPreferences pref = getSharedPreferences("Preferences", 0);
		
		switch(Math.round(pref.getFloat("distance", ADULT))){
		
		case -1:
			menu.findItem(R.id.adult).setChecked(true);
			return true;
			
		case -2:
			menu.findItem(R.id.little).setChecked(true);
			return true;
		
		default:
			menu.findItem(R.id.custom).setChecked(true);
			return true;
		}
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		
		SharedPreferences pref = getSharedPreferences("Preferences", 0);
		final SharedPreferences.Editor editor = pref.edit();
	
		switch(item.getItemId()){
		
		case R.id.adult:
			distance = 18.44f;
			editor.putFloat("distance", ADULT);
			editor.commit();
			setDistance = (TextView) findViewById(R.id.setDistance);
			setDistance.setText(Float.toString(distance)+"m");
			return true;
			
		case R.id.little:
			distance = 14.02f;
			editor.putFloat("distance", LITTLE);
			editor.commit();
			setDistance = (TextView) findViewById(R.id.setDistance);
			setDistance.setText(Float.toString(distance)+"m");
			return true;
		
		case R.id.custom:
			AlertDialog.Builder bld = new AlertDialog.Builder(MainActivity.this);
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			final View customedit = inflater.inflate(R.layout.edit_text, null);
			bld.setTitle("사용자 지정").setView(customedit).
			setPositiveButton("확인", new DialogInterface.OnClickListener() {
				
				public void onClick(DialogInterface dialog, int which) {

					EditText edit_text = (EditText)customedit.findViewById(R.id.edit_text);
					if(edit_text.getText().toString()!=""){
						distance = Float.parseFloat(edit_text.getText().toString());
						editor.putFloat("distance", distance);
						editor.commit();
						setDistance = (TextView) findViewById(R.id.setDistance);
						setDistance.setText(Float.toString(distance)+"m");
					}
					
				}
			}).
			setNegativeButton("취소", new DialogInterface.OnClickListener() {
				
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					
				}
			}).show();
			return true;
		}
			
		return false;
		
	}
	
	public class MyHandler extends Handler {
		public static final int TEST = 0;
		
		@Override
		public void handleMessage(Message msg) {
			File pic2 = null;
			File pic3 = null;
			switch(msg.what) {
			
			case CPU.STATE_DETECT_VALUE_CHANGE:
				showState("DVC");
				Log.d("PASS", "DVC.....");
				break;
				
			case CPU.STATE_BLOB_LABELING:
				showState("BL");
				Log.d("PASS", "BL.....");
				break;
				
			case CPU.STATE_BLOB_FILTERING:
				showState("BF");
				Log.d("PASS", "BF.....");
				break;
				
			case CPU.STATE_CANDIDATE_PROCESSING:
				showState("CP");
				Log.d("PASS", "CP.....");
				break;
				
			case CPU.STATE_BLOB_STAMPING:
				showState("BS");
				Log.d("PASS", "BS.....");
				break;
				
			case CPU.STATE_FOUND_BALL:
				showState("BALLFOUND");
				Log.d("PASS", "BALL FOUND");
				break;
				
			case CPU.STATE_BALL_CAUGHT:
				showState("BALLCAUGHT");
				refereeState();
				Log.d("PASS", "BALL CAUGHT");
				break;
			
			case 101:
				if(CPU.catcherprinted){
					cvSaveImage("/mnt/sdcard/result.jpg", result);
					pic2 = new File("/mnt/sdcard", "result.jpg");		
					iv.setImageBitmap(BitmapFactory.decodeFile(pic2.getAbsolutePath()));
					CPU.catcherprinted=false;
				}
				cvSaveImage("/mnt/sdcard/movieFrame.jpg", movieFrame);
				pic3 = new File("/mnt/sdcard", "movieFrame.jpg");		
				movieplay.setImageBitmap(BitmapFactory.decodeFile(pic3.getAbsolutePath()));
				break;
				
			case 100:				
				if(CPU.catcherprinted){
					cvSaveImage("/mnt/sdcard/result.jpg", result);
					pic2 = new File("/mnt/sdcard", "result.jpg");		
					iv.setImageBitmap(BitmapFactory.decodeFile(pic2.getAbsolutePath()));
					CPU.catcherprinted=false;
				}
				cvSaveImage("/mnt/sdcard/movieFrame.jpg", movieFrame);
				pic3 = new File("/mnt/sdcard", "movieFrame.jpg");		
				movieplay.setImageBitmap(BitmapFactory.decodeFile(pic3.getAbsolutePath()));
				
				tvCPU.setText("Video Finished");
				Log.d("PASS", "Done! saved result image");
				break;
			}
		}
	}
	
	class ProcessThread extends Thread {
		
		public boolean flag = true;
		
		public void run(){
			try {
				while (flag && CPU.framecount < CPU.framelength) {
					//printCatcher = false;
					result = ARMv7.process();
					//if(printCatcher) result = ARMv7.imgCatcher;
					movieFrame = ARMv7.getTmpl();
					mh.sendMessage(mh.obtainMessage(101));	// When not done
				}
				
				if(!flag) return;
				
				ARMv7.release();//why 주석처리했었음?
				processingFlag = false;
				mh.sendMessage(mh.obtainMessage(100));	// When done	
				
			} catch (java.lang.Exception e) {
				e.printStackTrace();
				//Log.e("STOP", e.getMessage());
			}
			
		}
	}
	
}
