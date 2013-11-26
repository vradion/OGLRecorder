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
import java.nio.ShortBuffer;
import java.util.Date;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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
	
	private AudioRecord audioRecord;
	private AudioRecordRunnable audioRecordRunnable;
	private Thread audioThread;
	private Thread videoThread;
	private volatile boolean runAudioThread = true;
	private volatile boolean runVideoThread = true;

	
	private final int sampleAudioRateInHz = 44100;
	private boolean recording = false;
	
	private Button btnStart;
	TextView txtElapsedTime;
	private final Handler handler = new Handler();

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
					btnStart.setText("Init");
				}
			}
		});
		
		txtElapsedTime = (TextView)findViewById(R.id.txtElapsedTime);


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
			yuvIplimage = IplImage.create(imageWidth, imageHeight, IPL_DEPTH_8U, 4);
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
		recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
		recorder.setFrameRate(frameRate);
		recorder.setFormat("mp4");
		recorder.setSampleRate(sampleAudioRateInHz);
		Log.i(TAG, "recorder initialize success");
		videoThread = new Thread(vrr);

		audioRecordRunnable = new AudioRecordRunnable();
		audioThread = new Thread(audioRecordRunnable);
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
		
		runAudioThread = true;
		audioThread.start();
		
		recording = true;
		handler.post(timingProcess);
	}

	public void stopRecording() {
		runVideoThread = false;
		runAudioThread = false;
		recording = false;
		handler.removeCallbacks(timingProcess);
	}
	
	public final Runnable timingProcess = new Runnable()
	{

		@Override
		public void run()
		{
			long elapsedTime = System.currentTimeMillis() - startTime;
			txtElapsedTime.setText("Elapsed time: " + elapsedTime + " miliseconds");
			handler.postDelayed(timingProcess, 500);
		}
	};
	
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
						yuvIplimage.getIntBuffer().put(videoData.data);
						Log.d(TAG, yuvIplimage.toString());
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
	
	//---------------------------------------------
		// audio thread, gets and encodes audio data
		//---------------------------------------------
		class AudioRecordRunnable implements Runnable {

			@Override
			public void run() {
				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

				// Audio
				int bufferSize;
				short[] audioData;
				int bufferReadResult;

				bufferSize = AudioRecord.getMinBufferSize(sampleAudioRateInHz,
						AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
				audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleAudioRateInHz,
						AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

				audioData = new short[bufferSize];

				Log.d(TAG, "audioRecord.startRecording()");
				audioRecord.startRecording();

				/* ffmpeg_audio encoding loop */
				while (runAudioThread) {
					bufferReadResult = audioRecord.read(audioData, 0, audioData.length);
					if (bufferReadResult > 0) {
						// If "recording" isn't true when start this thread, it never get's set according to this if statement...!!!
						// Why?  Good question...

						if (recording) {
							try {
								ShortBuffer[] bufferArray = new ShortBuffer[1];
								bufferArray[0] = ShortBuffer.wrap(audioData, 0, bufferReadResult);
								recorder.record(bufferArray);
							} catch (FFmpegFrameRecorder.Exception e) {
								Log.v(TAG,e.getMessage());
								e.printStackTrace();
							}
						}

					}
				}
				Log.v(TAG,"AudioThread Finished, release audioRecord");

				/* encoding finish, release recorder */
				if (audioRecord != null) {
					audioRecord.stop();
					audioRecord.release();
					audioRecord = null;
					Log.v(TAG,"audioRecord released");
				}
			}
		}

}
