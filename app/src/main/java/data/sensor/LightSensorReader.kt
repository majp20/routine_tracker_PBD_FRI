package data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LightSensorReader(
    private val context: Context
) {
    companion object {
        private const val TAG = "LightSensorReader"
    }

    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager


    //callback flow connects coroutine flows with callback functions used by sensors
    fun lightSensorValues(): Flow<Float> = callbackFlow {
        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        if (lightSensor == null) {
            close(IllegalArgumentException("Light sensor is null"))//closes the flow with an error
            return@callbackFlow //returns from callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return

                val lux = event.values[0]
                trySend(lux) //trySend sends value into the flow, trysend is not suspend function, meaning it does not pause the callback
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // not needed
            }
        }

        //here listener becomes active
        sensorManager.registerListener(
            listener,
            lightSensor, //what sensor we use
            SensorManager.SENSOR_DELAY_NORMAL//sets how often sensor updates the listener
        )

        //keeps the listener until the flow is being collected, after that it unregisters the listener
        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
    //first() means: Start collecting the flow, wait for the 1st value,
    //return it and stop collecting
    suspend fun readOnce(): Float {
        return lightSensorValues().first() //we return lux value here
    }

}