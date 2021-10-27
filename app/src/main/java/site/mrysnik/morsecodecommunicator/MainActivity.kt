package site.mrysnik.morsecodecommunicator

import android.content.pm.PackageManager
import android.media.MediaRecorder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var mediaRecorder: MediaRecorder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        SoundReceiver().test()

        val audioFilePath = Environment.getExternalStorageDirectory().absolutePath + "/Music/recording.mp3"

        mediaRecorder = MediaRecorder()
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder.setOutputFile(audioFilePath)

        buttonStop.isEnabled = false

        // Start Recording
        buttonRecord.setOnClickListener{
            if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.WRITE_EXTERNAL_STORAGE),1337)
            } else {
                buttonRecord.isEnabled = false
                buttonStop.isEnabled = true
                startRecording()
            }
        }

        // Stop Recording
        buttonStop.setOnClickListener{
            stopRecording()
            buttonRecord.isEnabled = true
            buttonStop.isEnabled = false
        }
    }

    override fun onResume() {
        super.onResume()
        var st = SoundTransmitter.getInstance()
        //st.sendMessage("test Test TEST")
    }

    private fun startRecording() {
        try {
            mediaRecorder.prepare()
            mediaRecorder.start()
            Toast.makeText(this, "Recording started!", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun stopRecording(){
        mediaRecorder.stop()
        mediaRecorder.release()
        Toast.makeText(this, "Recording stopped!", Toast.LENGTH_SHORT).show()
    }
}
