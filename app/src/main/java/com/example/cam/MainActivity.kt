package com.example.cam

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cam.Constants.TAG
import com.example.cam.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor:ExecutorService
    var globalCam = CameraSelector.DEFAULT_BACK_CAMERA


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(  layoutInflater)
        setContentView(binding.root)

        outputDirectory = getOutputDirectory()
        cameraExecutor  = Executors.newSingleThreadExecutor()


        if (allPermissionGranted()){
            startCamera()
        }else{
            ActivityCompat.requestPermissions(this,
                Constants.REQUIRED_PERMISSIONS,
                Constants.REQUEST_CODE_PERMISSIONS)
        }

        binding.btnClick.setOnClickListener {
            takePhoto()
            vibrateCam()
        }

        binding.btnFlipCam.setOnClickListener {
            if(globalCam== CameraSelector.DEFAULT_BACK_CAMERA){
                globalCam= CameraSelector.DEFAULT_FRONT_CAMERA
                startCamera()
                vibrateFlipCam()
            }
            else{
                globalCam= CameraSelector.DEFAULT_BACK_CAMERA
                startCamera()
                vibrateFlipCam()
            }
        }

        binding.btnGallery.setOnClickListener {
            vibrateFlipCam()
            val launchIntent = packageManager.getLaunchIntentForPackage("com.coloros.gallery3d")
            launchIntent?.let { startActivity(it) }
            }

    }

    private fun vibrateCam() {
        val vibrator = this?.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(100)
        }
    }

    private fun vibrateFlipCam() {
        val vibrator = this?.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(50)
        }
    }

    private fun getOutputDirectory(): File{

        val mediaDir = externalMediaDirs.firstOrNull()?.let { mfile->
            File(mfile, resources.getString(R.string.app_name)).apply {
                mkdirs()
            }
        }

        return if (mediaDir != null && mediaDir.exists() )
            mediaDir else filesDir


    }

    private fun takePhoto(){

        val imageCapture = imageCapture?: return
        val photoFile = File(
            outputDirectory, SimpleDateFormat(
                Constants.FILE_NAME_FORMAT,
                Locale.getDefault())
                .format(System
                    .currentTimeMillis()) + ".jpg")

        val outputOption = ImageCapture
            .OutputFileOptions
            .Builder(photoFile)
            .build()

        imageCapture.takePicture(
            outputOption,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback{
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo Saved"
                    Toast.makeText(this@MainActivity,
                        "$msg $savedUri",
                        Toast.LENGTH_SHORT)
                        .show()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "On Error ${exception.message}", exception)
                }
            }
        )
    }

    private fun startCamera(){
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also { mPreview ->

                mPreview.setSurfaceProvider(
                    binding.cvViewFinder.surfaceProvider
                )
            }
            imageCapture = ImageCapture.Builder().build()



            try {
             cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    globalCam,
                    preview, imageCapture
                )


            }catch (e: Exception){
                Log.d(TAG, "startCamera Fail...", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }




    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.REQUEST_CODE_PERMISSIONS){

            if (allPermissionGranted()){
                startCamera()
            }else{
                Toast.makeText(this,
                    "Permissions Denied",
                    Toast.LENGTH_SHORT).show()

                finish()
            }

        }
    }


    private fun allPermissionGranted() =
        Constants.REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                baseContext, it
            ) == PackageManager.PERMISSION_GRANTED
        }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}