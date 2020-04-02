package com.quadram.bubblelibrary

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    private val DRAW_OVER_OTHER_APP_PERMISSION_REQUEST_CODE = 1222

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        createFloatingWidget()
    }

    private fun createFloatingWidget() {
        //Check if the application has draw over other apps permission or not?
        //This permission is by default available for API<23. But for API > 23
        //you have to ask for the permission in runtime.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            //If the draw over permission is not available open the settings screen
            //to grant the permission.
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, DRAW_OVER_OTHER_APP_PERMISSION_REQUEST_CODE)
        } else  //If permission is granted start floating widget service
            startFloatingWidgetService()
    }

    private fun startFloatingWidgetService() {
        startService(Intent(this@MainActivity, FloatService::class.java))
        finish()
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        if (requestCode == DRAW_OVER_OTHER_APP_PERMISSION_REQUEST_CODE) {
            //Check if the permission is granted or not.
            if (resultCode == Activity.RESULT_OK) //If permission granted start floating widget service
                startFloatingWidgetService()
            else  //Permission is not available then display toast
                Toast.makeText(
                    this,
                    resources.getString(R.string.draw_other_app_permission_denied),
                    Toast.LENGTH_SHORT
                ).show()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }


}
