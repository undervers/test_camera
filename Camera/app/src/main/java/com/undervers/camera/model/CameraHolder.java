package com.undervers.camera.model;

import android.hardware.Camera;

public class CameraHolder {

    private Camera photoCamera;
    private Camera.CameraInfo cameraInfo;

    public CameraHolder(int cameraId, Camera photoCamera){

        cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, cameraInfo);

        this.photoCamera = photoCamera;
    }

    public void setDisplayOrientation(int degrees){
        photoCamera.setDisplayOrientation(degrees);
    }

    public void releaseCamera(){
        photoCamera.release();
    }

    public Camera getPhotoCamera() {
        return photoCamera;
    }

    public Camera.CameraInfo getCameraInfo() {
        return cameraInfo;
    }
}
