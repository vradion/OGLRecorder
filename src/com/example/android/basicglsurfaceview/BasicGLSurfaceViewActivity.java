/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.basicglsurfaceview;

import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.android.basicglsurfaceview.GLES20TriangleRenderer.VideoData;
import com.googlecode.javacv.FFmpegFrameRecorder;
import com.googlecode.javacv.cpp.avcodec;
import com.googlecode.javacv.cpp.opencv_core.IplImage;


public class BasicGLSurfaceViewActivity extends Activity {

	private BasicGLSurfaceView mView;
	public int imageWidth = 0;
	public int imageHeight = 0;
	public int frameRate = 24;
	private IplImage yuvIplimage = null;
	private volatile FFmpegFrameRecorder recorder;
	private long startTime;
	private volatile boolean runVideoThread = true;
	private Thread videoThread;
	private Button btnStart;

	public static String TAG = "BasicGLSurfaceView";

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		//mView = new BasicGLSurfaceView(getApplication());
		//setContentView(mView);
		setContentView(R.layout.main);
		mView = (BasicGLSurfaceView)findViewById(R.id.glSurfaceView);


		btnStart = (Button)findViewById(R.id.btnStart);
		btnStart.setText("Init");
		btnStart.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(btnStart.getText().equals("Init")){
					imageWidth = mView.getWidth();
					imageHeight = mView.getHeight();
					btnStart.setText("Start");
					initRecorder();

				} else if(btnStart.getText().equals("Start")){
					startRecording();
					btnStart.setText("Stop");
				} else if(btnStart.getText().equals("Stop")) {
					stopRecording();
					btnStart.setText("Start");
				}
			}
		});


	}

	@Override
	protected void onPause() {
		super.onPause();
		mView.onPause();
		//stopRecording();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mView.onResume();
	}

	public void initRecorder() {
		imageWidth = mView.getWidth();
		imageHeight = mView.getHeight();

		Log.d(TAG, "image width, image heigh:" + imageWidth +":" + imageHeight);

		if (yuvIplimage == null) {
			yuvIplimage = IplImage.create(imageWidth, imageHeight, IPL_DEPTH_8U, 3);
			Log.i(TAG, "create yuvIplimage");
		}

		String fName = Environment.getExternalStorageDirectory() + "/OpenGlRecorder/" + DateFormat.format("MMddyyhhmmss", (new Date().getTime())) + ".mp4";
		File file = new File(Environment.getExternalStorageDirectory() + "/OpenGlRecorder/", fName);
		if(!file.exists()) {
			File parent = file.getParentFile();
			if(parent != null) {
				if(!parent.exists()) {
					if(!parent.mkdirs()) {
						try {
							file.createNewFile();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		}

		recorder = new FFmpegFrameRecorder(fName, imageWidth, imageHeight, 1);
		//recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
		recorder.setVideoCodec(avcodec.AV_CODEC_ID_MPEG4);
		//recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
		recorder.setFrameRate(frameRate);
		recorder.setFormat("mp4");
		//recorder.setSampleRate(sampleAudioRateInHz);
		Log.i(TAG, "recorder initialize success");
		videoThread = new Thread(vrr);

	} 

	public void startRecording() {
		try {
			recorder.start();
			startTime = System.currentTimeMillis(); // di cung nhau
		} catch (Exception e) {
			e.printStackTrace();
		}
		runVideoThread = true;
		videoThread.start();
		
	}

	public void stopRecording() {
		runVideoThread = false;
	}

	final VideoRecordRunnable vrr = new VideoRecordRunnable();

	class VideoRecordRunnable implements Runnable {
		int size = 0;
		VideoData videoData;
		public VideoRecordRunnable() {

		}

		@Override
		public void run() {
			Log.d(TAG, "Start recording 00000000000000000000000000000");
			do {
				videoData = mView.getCurrentVideoData();
				if(videoData != null) {
					try {
						int h = imageHeight;
						int w = imageWidth;
						for(int i=0; i < h; ++i)  
						{  
							for(int j=0; j < w; ++j)  
							{  
								int ch1 = i*yuvIplimage.widthStep() + j*3 + 0;
								int ch2 = i*yuvIplimage.widthStep() + j*3 + 1;
								int ch3 = i*yuvIplimage.widthStep() + j*3 + 2;
								yuvIplimage.getByteBuffer().put(ch1, videoData.data[(h-i-1)*3*w + j*3+0]);
								yuvIplimage.getByteBuffer().put(ch2, videoData.data[(h-i-1)*3*w + j*3+1]);
								yuvIplimage.getByteBuffer().put(ch3, videoData.data[(h-i-1)*3*w + j*3+2]);

							}  
						}  
						long ts = (System.currentTimeMillis() - startTime);
						recorder.setTimestamp(1000L*ts);
						recorder.record(yuvIplimage);
						Log.d(TAG, "record: 1111111111111111: " + ts);
					} catch (FFmpegFrameRecorder.Exception e) {
						Log.v(TAG,e.getMessage());
						e.printStackTrace();
					}
				} else {
					Log.d(TAG, "Data null 9999999999999999999999999999999");
				}
			} while (runVideoThread);
			
			Log.d(TAG, "End recording thread AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");

			if (recorder != null ) {
				try {
					recorder.stop();
					recorder.release();
				} catch (FFmpegFrameRecorder.Exception e) {
					e.printStackTrace();
				}
				recorder = null;
			}

			return;
		}
	}

}
