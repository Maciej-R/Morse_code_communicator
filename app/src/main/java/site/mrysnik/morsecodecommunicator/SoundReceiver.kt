package site.mrysnik.morsecodecommunicator

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.matthewrussell.trwav.readIntLE
import java.io.File
import java.io.RandomAccessFile
import java.lang.Math.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

import java.nio.charset.StandardCharsets
import java.nio.charset.StandardCharsets.US_ASCII
import java.util.*
import kotlin.math.abs

class SoundReceiver {
    // Sound playing thread, immediately started
    //private val recorder: Thread = thread(true, false, null, "Sound player", -1) { this.record() }
    private var audioByteCount = 0
    private var audioData: ByteArray = ByteArray(0)

    fun record() {
        val audioSource = MediaRecorder.AudioSource.MIC
        val samplingRate = 44100
        val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO
        val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(samplingRate, channelConfig, audioFormat)
        val buffer = ShortArray(bufferSize / 4)
        val myRecord = AudioRecord(audioSource, samplingRate, channelConfig, audioFormat, bufferSize)
        myRecord.startRecording()
        var noAllRead = 0
        while (true) {
            val bufferResults = myRecord.read(buffer, 0, bufferSize / 4)
            noAllRead += bufferResults
            println(buffer.asList())
        }
    }

    fun test() {
        var totalFramesRead = 0
        val fileIn: File = File("/storage/emulated/0/Music/smbj_20wpm_1000hz.wav")
        val file = RandomAccessFile(fileIn, "r")
        readFile(file)
        //var bb = ByteBuffer.wrap(audioData).
        //println()
        //println(audioData.toList())
        var list1 = audioData.toList()

        var fs = 11050
        var unit_len = 60000f / (20 * 50)
        var space_len = unit_len * 7
        var intra_char_gap_len = unit_len
        var inter_char_gap_len = unit_len * 3
        var dot_len = unit_len
        var dash_len = unit_len * 3

        var space_siglen = (fs * space_len!!).toInt() / 1000
        var dot_siglen = (fs * dot_len!!).toInt() / 1000
        var dash_siglen = (fs * dash_len!!).toInt() / 1000
        var inter_character_gap_siglen =
            (fs * inter_char_gap_len!!).toInt() / 1000
        var intra_character_gap_siglen =
            (fs * intra_char_gap_len!!).toInt() / 1000

        println(arrayOf(space_siglen, dot_siglen, dash_siglen, inter_character_gap_siglen, intra_character_gap_siglen).toList())

        // TODO Passband filter

        var ml: MutableList<Int> = mutableListOf<Int>()

        list1.forEach {
            //ml.add(it.toUByte())
            if(it.toUByte() > 127u && it.toUByte() < 129u){
                ml.add(1)
            } else {
                ml.add(0)
            }
        }

        println(numStreak(ml))
    }

    private fun readFile(file: RandomAccessFile) {
        file.seek(40)
        val audioLength = file.readIntLE().toLong()
        audioByteCount = audioLength.toInt()

        //println(file.readByte())

        //Read the audio
        audioData = ByteArray(audioLength.toInt())
        file.read(audioData)
    }

    private fun numStreak(list: List<Int>): List<Int> {
        var current = list[0]
        var num = 0
        var ans: MutableList<Int> = mutableListOf<Int>()

        list.forEach {
            if(it == current) {
                ++num
            } else {
                current = it
                ans.add(num)
                num = 1
            }
        }
        return reduceShortStreaks(ans)
    }

    private fun reduceShortStreaks(list: List<Int>): List<Int> {
        var ans: MutableList<Int> = mutableListOf<Int>()
        var sum = 0
        val limit = 0.9*663 // should be set to shortest symbol???
        list.forEach {
            sum += it
            if(sum > limit){
                ans.add(sum)
                sum = 0
            }
        }
        return ans
    }
}