package com.example.morseflash

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var customText: EditText
    private lateinit var btnSos: Button
    private lateinit var btnCustom: Button
    private lateinit var btnStop: Button

    private lateinit var morseFlasher: MorseFlasher

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            statusText.text = "Camera permission granted"
        } else {
            statusText.text = "Camera permission denied"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        customText = findViewById(R.id.customText)
        btnSos = findViewById(R.id.btnSos)
        btnCustom = findViewById(R.id.btnCustom)
        btnStop = findViewById(R.id.btnStop)

        morseFlasher = MorseFlasher(this, lifecycleScope).apply {
            onStatus = { msg -> runOnUiThread { statusText.text = msg } }
        }

        btnSos.setOnClickListener {
            ensurePermissionThen { morseFlasher.flashText("SOS") }
        }

        btnCustom.setOnClickListener {
            val text = customText.text.toString().trim().ifEmpty { "SOS" }
            ensurePermissionThen { morseFlasher.flashText(text) }
        }

        btnStop.setOnClickListener {
            morseFlasher.stop()
            statusText.text = "Stopped"
        }
    }

    private fun ensurePermissionThen(action: () -> Unit) {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            action()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        morseFlasher.stop()
    }
}
