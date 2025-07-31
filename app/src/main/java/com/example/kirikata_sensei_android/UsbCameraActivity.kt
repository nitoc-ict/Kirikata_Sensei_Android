package com.example.kirikata_sensei_android

import android.os.Bundle
import android.view.ViewGroup
import com.jiangdg.ausbc.base.CameraActivity

class UsbCameraActivity : CameraActivity() {

    private lateinit var cameraContainer: ViewGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usb_camera)

        // container の View を取得して保存
        cameraContainer = findViewById(R.id.camera_view_container)
    }

    override fun getCameraViewContainer(): ViewGroup {
        return cameraContainer
    }
}
