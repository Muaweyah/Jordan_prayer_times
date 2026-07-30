package com.jo.prayertimes

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** بوصلة قبلة تفاعلية حقيقية تعتمد على حساسات الجهاز (تسارع + مجال مغناطيسي) وموقعه الجغرافي.
 *  أيقونة الكعبة الصغيرة تدور مع السهم على محيط البوصلة لتوضيح اتجاهها الفعلي بشكل مباشر. */
class QiblaActivity : AppCompatActivity(), SensorEventListener {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    private var qiblaBearing = 0.0
    private var lastRotation = 0f

    private lateinit var needleContainer: View
    private lateinit var tvQiblaAngle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qibla)

        HomeNavigator.wire(this)
        needleContainer = findViewById(R.id.needle_container)
        tvQiblaAngle = findViewById(R.id.tv_qibla_angle)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (accelerometer == null || magnetometer == null) {
            needleContainer.visibility = View.GONE
        }

        determineLocationAndBearing()
    }

    private fun determineLocationAndBearing() {
        var lat = 31.9539  // موقع افتراضي: عمّان، يُستخدم إن تعذّر تحديد الموقع الفعلي
        var lng = 35.9106

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            val lastKnown: Location? = try {
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } catch (e: SecurityException) {
                null
            }
            if (lastKnown != null) {
                lat = lastKnown.latitude
                lng = lastKnown.longitude
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 200)
        }

        qiblaBearing = calculateQiblaBearing(lat, lng)
        tvQiblaAngle.text = "${qiblaBearing.toInt()}°"
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 200) {
            determineLocationAndBearing()
        }
    }

    /** يحسب زاوية اتجاه القبلة (bearing) من موقع المستخدم نحو الكعبة المشرفة عبر معادلة الدائرة العظمى */
    private fun calculateQiblaBearing(lat: Double, lng: Double): Double {
        val kaabaLat = Math.toRadians(21.4225)
        val kaabaLng = Math.toRadians(39.8262)
        val myLat = Math.toRadians(lat)
        val myLng = Math.toRadians(lng)
        val deltaLng = kaabaLng - myLng

        val y = sin(deltaLng) * cos(kaabaLat)
        val x = cos(myLat) * sin(kaabaLat) - sin(myLat) * cos(kaabaLat) * cos(deltaLng)
        var bearing = Math.toDegrees(atan2(y, x))
        bearing = (bearing + 360) % 360
        return bearing
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        magnetometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, gravity, 0, 3)
                hasGravity = true
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                hasGeomagnetic = true
            }
        }

        if (hasGravity && hasGeomagnetic) {
            val rotationMatrix = FloatArray(9)
            val success = SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)
            if (success) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientation)
                val azimuthDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                val normalizedAzimuth = (azimuthDeg + 360) % 360

                val targetRotation = (qiblaBearing - normalizedAzimuth).toFloat()
                rotateNeedle(targetRotation)
            }
        }
    }

    /** يدور السهم وأيقونة الكعبة معاً كوحدة واحدة على محيط البوصلة */
    private fun rotateNeedle(degrees: Float) {
        val anim = RotateAnimation(
            lastRotation, degrees,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        anim.duration = 150
        anim.fillAfter = true
        needleContainer.startAnimation(anim)
        lastRotation = degrees
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
