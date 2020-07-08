package com.lee.zbardemo;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import net.sourceforge.zbar.Image;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.Button;

import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;

import android.widget.TextView;
import android.graphics.ImageFormat;
import android.widget.Toast;

/* Import ZBar Class files */
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;
import net.sourceforge.zbar.Config;

import java.io.UnsupportedEncodingException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private BeepManager beepManager;
    private Toast toast;

    private static final int DEF_WIDTH = 640;//1280;//640; //320;
    private static final int DEF_HEIGHT = 480;//720;//480;  //240;
    
    private Camera mCamera;
    private CameraPreview mPreview;
    private Handler autoFocusHandler;

    TextView scanText;
    Button scanButton;
    Button beepButton;

    ImageScanner scanner;

    private boolean barcodeScanned = false;
    private boolean previewing = true;
    
    static {
        System.loadLibrary("iconv");
        System.loadLibrary("zbar");
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        /* Instance barcode scanner */
        scanner = new ImageScanner();
        scanner.setConfig(0, Config.X_DENSITY, 3);
        scanner.setConfig(0, Config.Y_DENSITY, 3);

        autoFocusHandler = new Handler();
        mCamera = getCameraInstance();
        mPreview = new CameraPreview(this, mCamera, previewCb, autoFocusCB);
        FrameLayout preview = (FrameLayout)findViewById(R.id.cameraPreview);
        preview.addView(mPreview);

        Camera.Parameters parameters = mCamera.getParameters();
        Camera.Size size;
        //查看默认preview size
        size = parameters.getPreviewSize();
        Log.i(TAG, "getPreviewSize width:" + size.width + ",height:" + size.height);
        //查看相机支持的preview size 列表
        List<Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        if (null != supportedPreviewSizes) {
            for (Camera.Size previewSize : supportedPreviewSizes) {
                Log.i(TAG, "supportedPreviewSizes height:" + previewSize.height + ",width" + previewSize.width);
            }
        }else {
            Log.i(TAG, "not supported getSupportedPreviewSizes");
        }
        //设置preview size 1280 * 720
        parameters.setPreviewSize(DEF_WIDTH, DEF_HEIGHT);
        //查看设置后preview size
        size = parameters.getPreviewSize();
        Log.i(TAG, "getPreviewSize1 width:" + size.width + ",height:" + size.height);

        //查看默认picture size
        size = parameters.getPictureSize();
        Log.i(TAG, "getPictureSize width:" + size.width + ",height:" + size.height);
        //查看相机支持的picture size列表
        List<Camera.Size> supportedPictureSizes = parameters.getSupportedPictureSizes();
        if (null != supportedPictureSizes) {
            for (Camera.Size pictureSize : supportedPictureSizes) {
                Log.i(TAG, "SupportedPictureSizes height:" + pictureSize.height + ",width:" + pictureSize.width);
            }
        }else {
            Log.i(TAG, "not supported getSupportedPictureSizes");
        }
        //设置picture size 1280 * 720
        parameters.setPictureSize(DEF_WIDTH, DEF_HEIGHT);
        //查看设置后picture size
        size = parameters.getPictureSize();
        Log.i(TAG, "getPictureSize1 width:" + size.width + ",height:" + size.height);
        //将参数设置到camera
        mCamera.setParameters(parameters);

        beepManager = new BeepManager(this);
        beepManager.update();

        scanText = (TextView)findViewById(R.id.scanText);

        scanButton = (Button)findViewById(R.id.ScanButton);

        scanButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (barcodeScanned) {
                    barcodeScanned = false;
                    scanText.setText("Scanning...");
                    mCamera.setPreviewCallback(previewCb);
                    mCamera.startPreview();
                    previewing = true;
                    mCamera.autoFocus(autoFocusCB);
                }
            }
        });

        beepButton = (Button) findViewById(R.id.beepButton);
        beepButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                beepManager.close();
                beepManager.update();
                beepManager.playBeepSoundAndVibrate();  //叫一声
            }
        });

    }

    public void onPause() {
        super.onPause();
        releaseCamera();
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e){
        }
        return c;
    }

    private void releaseCamera() {
        if (mCamera != null) {
            previewing = false;
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    private Runnable doAutoFocus = new Runnable() {
        public void run() {
            if (previewing)
                mCamera.autoFocus(autoFocusCB);
        }
    };

    PreviewCallback previewCb = new PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera camera) {
            Camera.Parameters parameters = camera.getParameters();
            Size size = parameters.getPreviewSize();
            Log.i(TAG, "width:" + size.width + ",height:" + size.height);
            Image barcode = new Image(size.width, size.height, "Y800");
            barcode.setData(data);

            String qrCodeString = null;
            ImageScanner mImageScanner = new ImageScanner();

            int result = mImageScanner.scanImage(barcode);
            if (result != 0) {
                SymbolSet symSet = mImageScanner.getResults();
                for (Symbol sym : symSet)
                    qrCodeString = sym.getData();
            }
            
            if (!TextUtils.isEmpty(qrCodeString)) {
                // 成功识别二维码，qrCodeString就是数据。  
                try {
                    String encodeResust = new String(qrCodeString.getBytes("GB2312"), "utf-8");
                    Log.e(TAG, "result:" + encodeResust);
                    beepManager.close();
                    beepManager.update();
                    beepManager.playBeepSoundAndVibrate();  //叫一声
                    //Toast可以用来显示音量改变或者保存更新消息，如果用户一直点击，Toast会排队一个一个的，直到消息队列全部显示完
//                    Toast.makeText(MainActivity.this, encodeResust, Toast.LENGTH_SHORT).show();
                    //优化Toast显示
                    toastShow(MainActivity.this, encodeResust, Toast.LENGTH_SHORT);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
            Log.i(TAG, "complete");
        }
    };

    // Mimic continuous auto-focusing
    AutoFocusCallback autoFocusCB = new AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            autoFocusHandler.postDelayed(doAutoFocus, 1000);
        }
    };
    //优化Toast显示
    private void toastShow(Context context, CharSequence text, int duration) {
        if(toast == null){
            toast = Toast.makeText(context, text, duration);
        }else{
            toast.setText(text);
            toast.setDuration(duration);
        }
        toast.show();
    }
}