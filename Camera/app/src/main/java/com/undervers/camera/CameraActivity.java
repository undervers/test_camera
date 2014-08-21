package com.undervers.camera;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.undervers.camera.model.CameraHolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class CameraActivity extends Activity {

    private CameraHolder cameraHolder;

    private FrameLayout cameraLayout;
    private Spinner cameraSwitcher;
    private Spinner resolutionSwitcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.camera_activity);

        cameraLayout = (FrameLayout) findViewById(R.id.camera_layout);
        cameraSwitcher = (Spinner) findViewById(R.id.camera_switcher);
        resolutionSwitcher = (Spinner) findViewById(R.id.resolution_switcher);

        initCameraSwitcher();
    }

    @Override
    protected void onResume(){
        super.onResume();
        initCamera(0);
    }

    @Override
    protected void onPause(){
        super.onPause();

        releaseCamera();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (cameraHolder != null) {
            setCameraDisplayOrientation(cameraHolder);

            View preview = cameraLayout.getChildAt(0);
            preview.setLayoutParams(createScaledPreviewParams());
        }
    }

    private void initCamera(int cameraId){

        if(cameraHolder != null){

            cameraHolder.releaseCamera();
        }

        cameraHolder = createCameraHolder(cameraId);

        if(cameraHolder != null){

            setCameraPreview();
            initResolutionSwitcher(cameraHolder.getPhotoCamera());
            setCameraDisplayOrientation(cameraHolder);
        } else {

            Toast.makeText(CameraActivity.this, getString(R.string.get_access_camera_error), Toast.LENGTH_LONG).show();
            finish();
        }
    }


    private void setCameraPreview(){

        SurfaceView preview = new SurfaceView(this);
        preview.getHolder().addCallback(new SurfaceHolderCallback());
        preview.setLayoutParams(createScaledPreviewParams());

        cameraLayout.removeAllViews();
        cameraLayout.addView(preview);
    }

    private ViewGroup.LayoutParams createScaledPreviewParams(){

        FrameLayout.LayoutParams scaledParams;

        DisplayMetrics display = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(display);
        int displayHeight = display.heightPixels;
        int displayWidth = display.widthPixels;

        float displayAspect = displayWidth / (float) displayHeight;

        Camera.Size bestPreviewSize = findBestPreviewSize(displayAspect);
        int scaledHeight = bestPreviewSize.height * (displayHeight / bestPreviewSize.height + 1);
        int scaledWidth = bestPreviewSize.width * (displayWidth / bestPreviewSize.width + 1);

        scaledParams = new FrameLayout.LayoutParams(scaledWidth, scaledHeight);
        scaledParams.gravity = Gravity.CENTER;

        return scaledParams;
    }

    private Camera.Size findBestPreviewSize(float displayAspect){
        List <Camera.Size> previewSizes = cameraHolder.getPhotoCamera().getParameters().getSupportedPreviewSizes();
        int length = previewSizes.size();

        float minDiff = previewSizes.get(0).width / (float) previewSizes.get(0).height;
        float currentDiff;
        int minDiffIndex = 0;

        Camera.Size currentSize;


        for(int i = 0; i < length; i++){

            currentSize = previewSizes.get(i);

            currentDiff = Math.abs(displayAspect - (currentSize.width / (float) currentSize.height));
            if(minDiff >= currentDiff){
                minDiff = currentDiff;
                minDiffIndex = i;
            }
        }

        return previewSizes.get(minDiffIndex);
    }

    private CameraHolder createCameraHolder(int cameraId){
        CameraHolder cameraHolder = null;

        try {
            Camera photoCamera = Camera.open(cameraId);

            Camera.Parameters parameters = photoCamera.getParameters();
            parameters.setZoom(0);
            photoCamera.setParameters(parameters);

            cameraHolder = new CameraHolder(cameraId, photoCamera);
        }catch (Exception e){
            Log.e(getPackageName(), e.getMessage());
        }

        return cameraHolder;
    }

    private void initCameraSwitcher(){

        cameraSwitcher.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            //Spinner always call onItemSelected when call setOnItemSelectedListener
            private boolean firstTime = true;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                if(!firstTime) {
                    initCamera(position);
                }else{
                    firstTime = false;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        cameraSwitcher.setEnabled(Camera.getNumberOfCameras() > 1);
    }

    private void initResolutionSwitcher(final Camera camera){

        final List <Camera.Size> sizes = camera.getParameters().getSupportedPictureSizes();
        Collections.sort(sizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size lhs, Camera.Size rhs) {

                if(lhs.width >= rhs.width){
                    return -1;
                }else{
                    return 1;
                }
            }
        });

        List <String> resolutions = new ArrayList<String>();
        for(Camera.Size size : sizes){
            resolutions.add(size.width + "x" + size.height);
        }

        ArrayAdapter <String> adapter = new ArrayAdapter<String> (this, android.R.layout.simple_spinner_item, resolutions);
        resolutionSwitcher.setAdapter(adapter);
        resolutionSwitcher.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                Camera.Size size = sizes.get(position);
                Camera.Parameters parameters = camera.getParameters();
                parameters.setPictureSize(size.width, size.height);
                camera.setParameters(parameters);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void releaseCamera(){
        if(cameraHolder != null) {
            cameraHolder.releaseCamera();
            cameraHolder = null;
        }
    }

    public void onMakePhotoClick(View v){

        Camera photoCamera = cameraHolder.getPhotoCamera();
        photoCamera.takePicture(new Camera.ShutterCallback() {
            @Override
            public void onShutter() {
                //need for shutter sound
            }
        },
        null,
        new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {

                Bitmap picture = BitmapFactory.decodeByteArray(data, 0, data.length);

                int angle = getCameraDisplayAngle(cameraHolder);
                if(cameraHolder.getCameraInfo().facing == Camera.CameraInfo.CAMERA_FACING_FRONT){
                    angle *= -1;
                }

                Bitmap rotate = rotatePicture(picture, angle);

                savePicture(rotate);
                camera.startPreview();
            }
        });
    }

    private Bitmap rotatePicture(Bitmap picture, float degrees){



        Matrix matrix = new Matrix();
        matrix.setRotate(degrees);

        return Bitmap.createBitmap(picture, 0, 0, picture.getWidth(), picture.getHeight(), matrix, false);
    }

    private void savePicture(Bitmap data) {
        File picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        picturesDir.mkdirs();
        File pictureFile = new File(picturesDir, "image" + Calendar.getInstance().getTimeInMillis() + ".jpg");

        if(writeDataToFile(pictureFile, data)){
            Toast.makeText(this, getString(R.string.picture_saved, pictureFile.getPath()), Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText(this, getString(R.string.saving_file_error), Toast.LENGTH_LONG).show();
        }
    }

    private boolean writeDataToFile(File file, Bitmap data){

        boolean success;

        try {
            success = data.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(file));
        }catch (IOException e){
            Log.e(getPackageName(), e.getMessage());
            success = false;
        }

        return success;
    }

    public Integer getCameraDisplayAngle(CameraHolder cameraHolder) {

        Integer result = null;

        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
            }

        Camera.CameraInfo info = cameraHolder.getCameraInfo();

        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    private void setCameraDisplayOrientation(CameraHolder holder) {

        Integer result = getCameraDisplayAngle(holder);
        if(result != null) {
            cameraHolder.setDisplayOrientation(result);
        }
    }

    private class SurfaceHolderCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {

            if(cameraHolder != null) {

                Camera photoCamera = cameraHolder.getPhotoCamera();
                photoCamera.startPreview();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            if(cameraHolder != null && holder != null){

                Camera photoCamera = cameraHolder.getPhotoCamera();
                photoCamera.stopPreview();

                try {
                    photoCamera.setPreviewDisplay(holder);
                } catch (IOException e) {
                    Log.e(getPackageName(), e.getMessage());
                    photoCamera = null;
                }

                if(photoCamera != null) {
                    photoCamera.startPreview();
                } else {
                    Toast.makeText(CameraActivity.this, R.string.get_access_camera_error, Toast.LENGTH_LONG).show();
                    CameraActivity.this.finish();
                }
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    }
}
