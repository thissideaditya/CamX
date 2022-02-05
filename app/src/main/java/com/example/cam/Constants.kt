package com.example.cam

import android.Manifest


object Constants {
    const val TAG = "CAM"
    const val FILE_NAME_FORMAT = "dd-mm-yy-HH-mm-ss-SSS"
    const val REQUEST_CODE_PERMISSIONS = 123
    val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
}