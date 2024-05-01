package com.polar.polarsdkecghrdemo

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.androidplot.xy.BoundaryMode
import com.androidplot.xy.StepMode
import com.androidplot.xy.XYPlot
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl.defaultImplementation
import com.polar.sdk.api.errors.PolarInvalidArgument
import com.polar.sdk.api.model.PolarDeviceInfo
import com.polar.sdk.api.model.PolarEcgData
import com.polar.sdk.api.model.PolarHrData
import com.polar.sdk.api.model.PolarSensorSetting
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import java.util.*

class ECGActivity : AppCompatActivity(), PlotterListener {
    companion object {
        private const val TAG = "ECGActivity"
    }

    private lateinit var deviceId1: String
    private lateinit var deviceId2: String

    private lateinit var textViewHR1: TextView
    private lateinit var textViewHR2: TextView
    private lateinit var textViewRR1: TextView
    private lateinit var textViewRR2: TextView
    private lateinit var textViewDeviceId1: TextView
    private lateinit var textViewDeviceId2: TextView
    private lateinit var api: PolarBleApi
    private lateinit var textViewBattery: TextView
    private lateinit var textViewFwVersion: TextView

    private lateinit var plot1: XYPlot
    private lateinit var plot2: XYPlot
    private lateinit var ecgPlotter1: EcgPlotter
    private lateinit var ecgPlotter2: EcgPlotter

    private var ecgDisposable1: Disposable? = null
    private var ecgDisposable2: Disposable? = null
    private var hrDisposable1: Disposable? = null
    private var hrDisposable2: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ecg)
        deviceId1 = intent.getStringExtra("deviceId1") ?: throw Exception("ECGActivity couldn't be created, no deviceId1 given")
        deviceId2 = intent.getStringExtra("deviceId2") ?: throw Exception("ECGActivity couldn't be created, no deviceId2 given")

        textViewHR1 = findViewById(R.id.hr1)
        textViewHR2 = findViewById(R.id.hr2)
        textViewRR1 = findViewById(R.id.rr1)
        textViewRR2 = findViewById(R.id.rr2)
        textViewDeviceId1 = findViewById(R.id.deviceId1)
        textViewDeviceId2 = findViewById(R.id.deviceId2)
        textViewDeviceId1.text = "ID1: $deviceId1"
        textViewDeviceId2.text = "ID2: $deviceId2"
        textViewBattery = findViewById(R.id.battery_level)
        textViewFwVersion = findViewById(R.id.fw_version)
        plot1 = findViewById(R.id.plot1)
        plot2 = findViewById(R.id.plot2)

        api = defaultImplementation(
            applicationContext,
            setOf(
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO,
                PolarBleApi.PolarBleSdkFeature.FEATURE_DEVICE_INFO
            )
        )
        api.setApiCallback(object : PolarBleApiCallback() {
            override fun blePowerStateChanged(powered: Boolean) {
                Log.d(TAG, "BluetoothStateChanged $powered")
            }

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "Device connected " + polarDeviceInfo.deviceId)
                Toast.makeText(applicationContext, R.string.connected, Toast.LENGTH_SHORT).show()
            }

            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "Device connecting ${polarDeviceInfo.deviceId}")
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "Device disconnected ${polarDeviceInfo.deviceId}")
            }

            override fun bleSdkFeatureReady(identifier: String, feature: PolarBleApi.PolarBleSdkFeature) {
                Log.d(TAG, "feature ready $feature")

                when (feature) {
                    PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING -> {
                        streamECG()
                        streamHR()
                    }
                    else -> {}
                }
            }

            override fun disInformationReceived(identifier: String, uuid: UUID, value: String) {
                if (uuid == UUID.fromString("00002a28-0000-1000-8000-00805f9b34fb")) {
                    val msg = "Firmware: " + value.trim { it <= ' ' }
                    Log.d(TAG, "Firmware: " + identifier + " " + value.trim { it <= ' ' })
                    textViewFwVersion.append(msg.trimIndent())
                }
            }

            override fun batteryLevelReceived(identifier: String, level: Int) {
                Log.d(TAG, "Battery level $identifier $level%")
                val batteryLevelText = "Battery level: $level%"
                textViewBattery.append(batteryLevelText)
            }

            override fun hrNotificationReceived(identifier: String, data: PolarHrData.PolarHrSample) {
                // deprecated
            }

            override fun polarFtpFeatureReady(identifier: String) {
                // deprecated
            }

            override fun streamingFeaturesReady(identifier: String, features: Set<PolarBleApi.PolarDeviceDataType>) {
                // deprecated
            }

            override fun hrFeatureReady(identifier: String) {
                // deprecated
            }
        })
        try {
            api.connectToDevice(deviceId1)
            api.connectToDevice(deviceId2)
        } catch (a: PolarInvalidArgument) {
            a.printStackTrace()
        }

        ecgPlotter1 = EcgPlotter("ECG - Device 1 ($deviceId1)", 130)
        ecgPlotter1.setListener(this)

        ecgPlotter2 = EcgPlotter("ECG - Device 2 ($deviceId2)", 130)
        ecgPlotter2.setListener(this)

        plot1.addSeries(ecgPlotter1.getSeries(), ecgPlotter1.formatter)
        plot1.setRangeBoundaries(-1.5, 1.5, BoundaryMode.FIXED)
        plot1.setRangeStep(StepMode.INCREMENT_BY_FIT, 0.25)
        plot1.setDomainStep(StepMode.INCREMENT_BY_VAL, 130.0)
        plot1.setDomainBoundaries(0, 650, BoundaryMode.FIXED)
        plot1.linesPerRangeLabel = 2

        plot2.addSeries(ecgPlotter2.getSeries(), ecgPlotter2.formatter)
        plot2.setRangeBoundaries(-1.5, 1.5, BoundaryMode.FIXED)
        plot2.setRangeStep(StepMode.INCREMENT_BY_FIT, 0.25)
        plot2.setDomainStep(StepMode.INCREMENT_BY_VAL, 130.0)
        plot2.setDomainBoundaries(0, 650, BoundaryMode.FIXED)
        plot2.linesPerRangeLabel = 2
    }

    public override fun onDestroy() {
        super.onDestroy()
        ecgDisposable1?.let {
            if (!it.isDisposed) it.dispose()
        }
        ecgDisposable2?.let {
            if (!it.isDisposed) it.dispose()
        }
        api.shutDown()
    }

    fun streamECG() {
        val isDisposed1 = ecgDisposable1?.isDisposed ?: true
        val isDisposed2 = ecgDisposable2?.isDisposed ?: true

        if (isDisposed1) {
            ecgDisposable1 = api.requestStreamSettings(deviceId1, PolarBleApi.PolarDeviceDataType.ECG)
                .toFlowable()
                .flatMap { sensorSetting: PolarSensorSetting -> api.startEcgStreaming(deviceId1, sensorSetting.maxSettings()) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { polarEcgData: PolarEcgData ->
                        Log.d(TAG, "ecg update")
                        for (data in polarEcgData.samples) {
                            ecgPlotter1.sendSingleSample((data.voltage.toFloat() / 1000.0).toFloat())
                        }
                    },
                    { error: Throwable ->
                        Log.e(TAG, "Ecg stream failed for device 1: $error")
                        ecgDisposable1 = null
                    },
                    {
                        Log.d(TAG, "Ecg stream complete for device 1")
                    }
                )
        } else {
            ecgDisposable1?.dispose()
            ecgDisposable1 = null
        }

        if (isDisposed2) {
            ecgDisposable2 = api.requestStreamSettings(deviceId2, PolarBleApi.PolarDeviceDataType.ECG)
                .toFlowable()
                .flatMap { sensorSetting: PolarSensorSetting -> api.startEcgStreaming(deviceId2, sensorSetting.maxSettings()) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { polarEcgData: PolarEcgData ->
                        Log.d(TAG, "ecg update")
                        for (data in polarEcgData.samples) {
                            ecgPlotter2.sendSingleSample((data.voltage.toFloat() / 1000.0).toFloat())
                        }
                    },
                    { error: Throwable ->
                        Log.e(TAG, "Ecg stream failed for device 2: $error")
                        ecgDisposable2 = null
                    },
                    {
                        Log.d(TAG, "Ecg stream complete for device 2")
                    }
                )
        } else {
            ecgDisposable2?.dispose()
            ecgDisposable2 = null
        }
    }

    fun streamHR() {
        val isDisposed1 = hrDisposable1?.isDisposed ?: true
        val isDisposed2 = hrDisposable2?.isDisposed ?: true

        if (isDisposed1) {
            hrDisposable1 = api.startHrStreaming(deviceId1)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { hrData: PolarHrData ->
                        for (sample in hrData.samples) {
                            Log.d(TAG, "HR ${sample.hr}")
                            if (sample.rrsMs.isNotEmpty()) {
                                val rrText = "(${sample.rrsMs.joinToString(separator = "ms, ")}ms)"
                                textViewRR1.text = rrText
                            }
                            textViewHR1.text = sample.hr.toString()
                        }
                    },
                    { error: Throwable ->
                        Log.e(TAG, "HR stream failed for device 1. Reason $error")
                        hrDisposable1 = null
                    },
                    { Log.d(TAG, "HR stream completed for device 1") }
                )
        } else {
            hrDisposable1?.dispose()
            hrDisposable1 = null
        }

        if (isDisposed2) {
            hrDisposable2 = api.startHrStreaming(deviceId2)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { hrData: PolarHrData ->
                        for (sample in hrData.samples) {
                            Log.d(TAG, "HR ${sample.hr}")
                            if (sample.rrsMs.isNotEmpty()) {
                                val rrText = "(${sample.rrsMs.joinToString(separator = "ms, ")}ms)"
                                textViewRR2.text = rrText
                            }
                            textViewHR2.text = sample.hr.toString()
                        }
                    },
                    { error: Throwable ->
                        Log.e(TAG, "HR stream failed for device 2. Reason $error")
                        hrDisposable2 = null
                    },
                    { Log.d(TAG, "HR stream completed for device 2") }
                )
        } else {
            hrDisposable2?.dispose()
            hrDisposable2 = null
        }
    }

    override fun update() {
        runOnUiThread {
            plot1.redraw()
            plot2.redraw()
        }
    }
}