package site.mrysnik.morsecodecommunicator

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

private const val REQUEST_PERMISSIONS_CODE = 1337

class MainActivity : AppCompatActivity() {
    // Requesting permission to RECORD_AUDIO
    private var permissionToRecordAccepted = false
    private var permissions: Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE)

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionToRecordAccepted = if (requestCode == REQUEST_PERMISSIONS_CODE) {
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
        if (!permissionToRecordAccepted) finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Init singletons
        SignalProcessor.getInstance()
        SoundReceiver.getInstance()

        buttonStop.isEnabled = false

        ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS_CODE)

        // Start Recording
        buttonRecord.setOnClickListener{
            buttonRecord.isEnabled = false
            buttonStop.isEnabled = true
            val uniqueString: String = UUID.randomUUID().toString()
            SoundReceiver.getInstance()?.startRecording(applicationContext.cacheDir.absolutePath + "/" + uniqueString)
        }

        // Stop Recording
        buttonStop.setOnClickListener{
            SoundReceiver.getInstance()?.stopRecording()
            buttonRecord.isEnabled = true
            buttonStop.isEnabled = false

            val mp = MorseProcessor(SoundReceiver.getInstance()?.fileName)
            mp.process()
            this.textView4.text = mp.result()
        }

        buttonSend.setOnClickListener{
            SoundTransmitter.getInstance().sendMessage(messageTextBox.text.toString())
        }
    }

    override fun onResume() {
        super.onResume()
        SoundTransmitter.getInstance()
        SoundReceiver.getInstance()
    }
}
