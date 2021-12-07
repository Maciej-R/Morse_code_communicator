package site.mrysnik.morsecodecommunicator

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.*
import kotlin.experimental.and

private const val LOG_TAG = "SoundReceiver"

class SoundReceiver {
    private var recordingThread: Thread? = null
    var fileName: String = ""
        private set
    private var recorder: AudioRecord? = null
    var isRecording: Boolean = false
        private set

    val BufferElements2Rec = 1024 // want to play 2048 (2K) since 2 bytes we use only 1024
    val BytesPerElement = 2 // 2 bytes in 16bit format

    @SuppressLint("WrongConstant", "MissingPermission")
    fun startRecording(outputPath: String) {
        if (recorder != null && isRecording) {
            return
        }
        recorder = AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
            BufferElements2Rec * BytesPerElement)
        fileName = outputPath
        recorder?.startRecording()
        isRecording = true
        recordingThread = Thread({ writeAudioDataToFile() }, "AudioRecorder Thread")
        recordingThread?.start()
    }

    private fun short2byte(sData: ShortArray): ByteArray {
        val shortArrsize = sData.size
        val bytes = ByteArray(shortArrsize * 2)
        for (i in 0 until shortArrsize) {
            bytes[i * 2] = (sData[i] and 0xff).toByte()
            bytes[i * 2 + 1] = (sData[i].toInt() shr 8 and 0xff).toByte()
            sData[i] = 0
        }
        return bytes
    }

    private fun writeAudioDataToFile() {
        // Write the output audio in byte

        // Write the output audio in byte
        val filePath = "${this@SoundReceiver.fileName}.tmp"
        val sData = ShortArray(BufferElements2Rec)

        var os: FileOutputStream? = null
        try {
            os = FileOutputStream(filePath)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

        while (isRecording) {
            // gets the voice output from microphone to byte format
            recorder!!.read(sData, 0, BufferElements2Rec)
            try {
                // // writes the data to file from buffer
                // // stores the voice buffer
                os?.write(short2byte(sData))
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        try {
            os?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun stopRecording() {
        if (recorder != null) {
            isRecording = false
            recorder?.stop()
            recorder?.release()
            recorder = null
            recordingThread?.join()
            recordingThread = null

            System.gc()

            val inFile = File("${this@SoundReceiver.fileName}.tmp")
            
            val file = File(fileName)
            WriteWaveFileHeader(
                file,
                inFile.length(),
                inFile.length() + 44 - 8,
                44100,
                1,
                (44100 * 16 * 1) / 8
            )
            file.appendBytes(inFile.readBytes())
        }
    }

    companion object {
        private var instance: SoundReceiver? = null
        fun getInstance(): SoundReceiver? {
            if (this.instance != null) {
                return this.instance!!
            }
            this.instance = SoundReceiver()
            return this.instance!!
        }
    }

    private fun WriteWaveFileHeader(
        file: File, totalAudioLen: Long,
        totalDataLen: Long, longSampleRate: Long, channels: Int,
        byteRate: Long
    ) {
        val header = ByteArray(44)
        header[0] = 0x52 // RIFF/WAVE header
        header[1] = 0x49
        header[2] = 0x46
        header[3] = 0x46
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.toByte()
        header[9] = 'A'.toByte()
        header[10] = 'V'.toByte()
        header[11] = 'E'.toByte()
        header[12] = 'f'.toByte() // 'fmt ' chunk
        header[13] = 'm'.toByte()
        header[14] = 't'.toByte()
        header[15] = ' '.toByte()
        header[16] = 16 // 4 bytes: size of 'fmt ' chunk
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // format = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (longSampleRate and 0xff).toByte()
        header[25] = (longSampleRate shr 8 and 0xff).toByte()
        header[26] = (longSampleRate shr 16 and 0xff).toByte()
        header[27] = (longSampleRate shr 24 and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte()
        header[31] = (byteRate shr 24 and 0xff).toByte()
        header[32] = (channels * 16 / 8).toByte() // block align
        header[33] = 0
        header[34] = 16 // bits per sample
        header[35] = 0
        header[36] = 'd'.toByte()
        header[37] = 'a'.toByte()
        header[38] = 't'.toByte()
        header[39] = 'a'.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = (totalAudioLen shr 8 and 0xff).toByte()
        header[42] = (totalAudioLen shr 16 and 0xff).toByte()
        header[43] = (totalAudioLen shr 24 and 0xff).toByte()
        file.writeBytes(header)
    }
}