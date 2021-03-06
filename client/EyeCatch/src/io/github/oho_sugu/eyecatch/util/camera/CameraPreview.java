package io.github.oho_sugu.eyecatch.util.camera;

import io.github.oho_sugu.eyecatch.textrecognition.TextRecognitionClient;
import io.github.oho_sugu.eyecatch.textrecognition.result.RecognitionJobResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraPreview extends SurfaceView implements
		SurfaceHolder.Callback, Camera.PreviewCallback {
	private SurfaceHolder mHolder;
	private Camera mCamera;
	static String TAG = "CameraPreview.cls";
	Handler mHandler = new Handler();
	private TextRecognitionClient mClient;
	int mFormatPreviewCallback;

	Thread mThread;

	public interface RecognitionResultCallback {
		public void onRecognitionResult(List<String> words);
	}

	RecognitionResultCallback mCallback;

	public CameraPreview(Context context, Camera camera,
			RecognitionResultCallback callback) {
		// TODO Auto-generated constructor stub
		super(context);
		// mContext = context;
		mCamera = camera;
		mCallback = callback;
		//mFormatPreviewCallback = mCamera.getParameters().getPreviewFormat();
		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = getHolder();
		mHolder.addCallback(this);
		// deprecated setting, but required on Android versions prior to 3.0
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		mClient = new TextRecognitionClient();

	}

	public void surfaceCreated(SurfaceHolder holder) {
		// The Surface has been created, now tell the camera where to draw the
		// preview.
		try {
			mCamera.setPreviewDisplay(holder);
			mCamera.startPreview();
		} catch (IOException e) {
			Log.d(TAG, "Error setting camera preview: " + e.getMessage());
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		// empty. Take care of releasing the Camera preview in your activity.
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		// If your preview can change or rotate, take care of those events here.
		// Make sure to stop the preview before resizing or reformatting it.

		if (mHolder.getSurface() == null) {
			// preview surface does not exist
			return;
		}

		// stop preview before making changes
		try {
			mCamera.stopPreview();
		} catch (Exception e) {
			// ignore: tried to stop a non-existent preview
		}

		// set preview size and make any resize, rotate or
		// reformatting changes here

		// start preview with new settings
		try {
			mCamera.setPreviewDisplay(mHolder);
			mCamera.setPreviewCallback(this);
			mCamera.startPreview();

		} catch (Exception e) {
			Log.d(TAG, "Error starting camera preview: " + e.getMessage());
		}
	}

	@Override
	public void onPreviewFrame(final byte[] data, final Camera camera) {
		// TODO Auto-generated method stub
		if (TextRecognitionClient.canRequest()) {
			mThread = new Thread(new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					RecognitionJobResult result = mClient.request(camera, data);
					final List<String>words = new ArrayList<String>();
					Bundle bundle = new Bundle();
					for (RecognitionJobResult.Word word : result.words.word) {
						bundle.putString(word.text, word.category);
						words.add(word.text);
					}
					Message message = new Message();
					message.setData(bundle);
					mHandler.post(new Runnable() {

						@Override
						public void run() {
							// TODO Auto-generated method stub
							mCallback.onRecognitionResult(words);
						}
					});

				}
			});
			mThread.start();
		}

	}
}
