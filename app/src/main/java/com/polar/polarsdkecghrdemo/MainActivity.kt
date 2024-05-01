package com.polar.polarsdkecghrdemo

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "Polar_MainActivity"
        private const val SHARED_PREFS_KEY = "polar_device_id"
        private const val PERMISSION_REQUEST_CODE = 1
    }

    private lateinit var sharedPreferences: SharedPreferences
    private val bluetoothOnActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode != Activity.RESULT_OK) {
            Log.w(TAG, "Bluetooth off")
        }
    }
    private var deviceId1: String? = null
    private var deviceId2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sharedPreferences = getPreferences(MODE_PRIVATE)
        deviceId1 = sharedPreferences.getString("${SHARED_PREFS_KEY}_1", "")
        deviceId2 = sharedPreferences.getString("${SHARED_PREFS_KEY}_2", "")

        val setIdButton: Button = findViewById(R.id.buttonSetID)
        val ecgConnectButton: Button = findViewById(R.id.buttonConnectEcg)
        val hrConnectButton: Button = findViewById(R.id.buttonConnectHr)
        val viewSavedFilesButton: Button = findViewById(R.id.buttonViewSavedFiles)
        checkBT()

        setIdButton.setOnClickListener { onClickChangeID(it) }
        ecgConnectButton.setOnClickListener { onClickConnectEcg() }
        hrConnectButton.setOnClickListener { onClickConnectHr() }
        viewSavedFilesButton.setOnClickListener {
            val intent = Intent(this, SavedFilesActivity::class.java)
            startActivity(intent)
        }
    }

    private fun onClickConnectEcg() {
        checkBT()
        if (deviceId1 == null || deviceId2 == null || deviceId1 == "" || deviceId2 == "") {
            showDialog(true)
        } else {
            val intent = Intent(this, ECGActivity::class.java)
            intent.putExtra("deviceId1", deviceId1)
            intent.putExtra("deviceId2", deviceId2)
            startActivity(intent)
        }
    }

    private fun onClickConnectHr() {
        checkBT()
        if (deviceId1 == null || deviceId2 == null || deviceId1 == "" || deviceId2 == "") {
            showDialog(true)
        } else {
            val intent = Intent(this, HRActivity::class.java)
            intent.putExtra("deviceId1", deviceId1)
            intent.putExtra("deviceId2", deviceId2)
            startActivity(intent)
        }
    }

    private fun onClickChangeID(view: View) {
        showDialog(true)
    }

    private fun showDialog(isTwoDevices: Boolean) {
        val dialog = AlertDialog.Builder(this, R.style.PolarTheme)
        dialog.setTitle("Enter Polar device IDs")

        val viewInflated = LayoutInflater.from(applicationContext).inflate(R.layout.device_id_dialog_layout, null)
        val input1 = viewInflated.findViewById<EditText>(R.id.input1)
        val input2 = viewInflated.findViewById<EditText>(R.id.input2)

        if (deviceId1?.isNotEmpty() == true) input1.setText(deviceId1)
        if (deviceId2?.isNotEmpty() == true) input2.setText(deviceId2)

        input1.inputType = InputType.TYPE_CLASS_TEXT
        input2.inputType = InputType.TYPE_CLASS_TEXT

        dialog.setView(viewInflated)

        dialog.setPositiveButton("OK") { _, _ ->
            deviceId1 = input1.text.toString().uppercase()
            deviceId2 = input2.text.toString().uppercase()
        }
        dialog.setNegativeButton("Cancel") { dialogInterface, _ -> dialogInterface.cancel() }

        dialog.show()
    }

    private fun checkBT() {
        val btManager = applicationContext.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter: BluetoothAdapter? = btManager.adapter
        if (bluetoothAdapter == null) {
            showToast("Device doesn't support Bluetooth")
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetoothOnActivityResultLauncher.launch(enableBtIntent)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT), PERMISSION_REQUEST_CODE)
            } else {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE)
            }
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (index in 0..grantResults.lastIndex) {
                if (grantResults[index] == PackageManager.PERMISSION_DENIED) {
                    Log.w(TAG, "Needed permissions are missing")
                    showToast("Needed permissions are missing")
                    return
                }
            }
            Log.d(TAG, "Needed permissions are granted")
        }
    }

    private fun showToast(message: String) {
        val toast = Toast.makeText(applicationContext, message, Toast.LENGTH_LONG)
        toast.show()
    }
}