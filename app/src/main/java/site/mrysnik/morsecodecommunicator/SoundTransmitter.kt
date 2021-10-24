package site.mrysnik.morsecodecommunicator

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import androidx.annotation.RequiresApi
import java.lang.Math.sin
import java.lang.Thread.sleep
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread


/**
 * Singleton object responsible for all operation related to sound transmission
 * @constructor
 */
class SoundTransmitter: Parameters() {

    private var dict_numeric: MutableMap<Char, Vector<Byte>> = mutableMapOf<Char, Vector<Byte>>()    // Mapping characters to vector of bytes representing dots and dashes
    private var message_queue: LinkedBlockingQueue<Vector<Signal>> = LinkedBlockingQueue<Vector<Signal>>()  // Used to queue messages that are to be transmitted
    private val player: Thread = thread(true, false, null, "Sound player", -1) { this.play() } //Thread(play, "Sound player") // Sound playing thread
    private var kill: Boolean = false   // Value indicating when signal playing thread should quit
    private var fs = 44100  // Sampling frequency

    init {

        for(e: Map.Entry<Char, String> in this.dict.entries){

            var vec: Vector<Byte> = Vector<Byte>()
            for(c: Char in e.value){
                vec.add((if (c == '-') 1 else 0))
            }
            this.dict_numeric?.put(e.key, vec)
            print(e.key)

        }

        //this.player.run()

    }

    enum class Signal{
        dot, dash, inter_character_gap, intra_character_gap, space
    }

    private fun translate(message: String): Vector<Signal> {

        val words: List<String> = message.split(' ')
        var res = Vector<Signal>()

        // Iterate through words
        for((i, word: String) in words.withIndex()){
            // Iterate through characters
            for((j, c: Char) in word.withIndex()) {
                var code: Vector<Byte>? = this.dict_numeric?.get(c)
                if (code == null) throw RuntimeException("Wrong character: $c")
                // Iterate through signals in code
                for(k: Byte in code){
                    // Gaps between characters are not represented -
                        // each character is one value in vector so they can be easily distinguished
                    if(k.toInt() == 1) res.add(Signal.dash)
                    else res.add(Signal.dot)
                }
                // If not end of word
                if(j != word.length) res.add(Signal.inter_character_gap)
            }
            // If not end of sentence
            if(i != words.size - 1) res.add(Signal.space)
        }

        return res

    }


    @RequiresApi(Build.VERSION_CODES.S)
    private fun play(){

        var use_fs = 0
        var inter_character_gap_siglen: Int = 0
        var intra_character_gap_siglen: Int = 0
        var space_siglen: Int = 0
        var dot_sound: ShortArray? = null
        var dash_sound: ShortArray? = null
        var intra_char_gap_sound: ShortArray? = null
        var inter_char_gap_sound: ShortArray? = null
//        var dot_sound: Array<Short>? = null
//        var dash_sound: Array<Short>? = null
//        var intra_char_gap_sound: Array<Short>? = null

        val audio = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING)
                    .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(44100)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(100000)//AudioTrack.getMinBufferSize(this.fs, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        audio.setVolume(0.25F)

        while(!this.kill){

            // Take next message or wait until it will be available
            var msg: Vector<Signal> = message_queue.take()
            var signal: Vector<Short> = Vector<Short>()

            // If necessary create sound samples to use
            if (this.fs != use_fs) {
                use_fs = this.fs
                var dot_siglen: Int = (this.fs * this.dot_len!!).toInt()/1000
                var dash_siglen: Int = (this.fs * this.dash_len!!).toInt()/1000
                inter_character_gap_siglen = (this.fs * this.inter_char_gap_len!!).toInt()/1000
                intra_character_gap_siglen = (this.fs * this.intra_char_gap_len!!).toInt()/1000
                space_siglen = (this.fs * this.space_len!!).toInt()/1000

                dot_sound = ShortArray(dot_siglen) {i -> ((kotlin.math.sin(2 * Math.PI * i * this.freqency / this.fs)) * Short.MAX_VALUE).toShort()}
                dash_sound = ShortArray(dash_siglen) {i -> ((kotlin.math.sin(2 * Math.PI * i * this.freqency / this.fs)) * Short.MAX_VALUE).toShort()}
                intra_char_gap_sound = ShortArray(intra_character_gap_siglen)
                inter_char_gap_sound = ShortArray(inter_character_gap_siglen)
//                dot_sound = Array(dot_siglen) {i -> ((kotlin.math.sin(2 * Math.PI * i * this.freqency / this.fs)) * Short.MAX_VALUE).toShort()}
//                dash_sound = Array(dash_siglen) {i -> ((kotlin.math.sin(2 * Math.PI * i * this.freqency / this.fs)) * Short.MAX_VALUE).toShort()}
//                intra_char_gap_sound = Array(intra_character_gap_siglen) { 0.toShort() }
                //var cnt = audio.setStartThresholdInFrames(dot_siglen)
            }

            var s_len = msg.size
            for((i, s: Signal) in msg.withIndex()){

                //var arr: Array<Short>? = null
                var next: Signal? = null
                if (i < s_len-1) next = msg[i+1]
                when(s){

                    Signal.dot -> {
                        if (dot_sound != null) {
                            //signal.addAll(dot_sound)
                            audio.write(dot_sound, 0, dot_sound.size)
                            audio.play()
                        }
                    }
                    Signal.dash -> {
                        if (dash_sound != null) {
                            //signal.addAll(dash_sound)
                            audio.write(dash_sound, 0, dash_sound.size)
                            audio.play()
                        }
                    }
                    Signal.inter_character_gap -> {
                        if (inter_char_gap_sound != null) {
                            audio.write(inter_char_gap_sound, 0, inter_char_gap_sound.size)
                            audio.play()
                        }
                    //this.inter_char_gap_len?.let { sleep(it.toLong()) }
                    }
                    Signal.space -> {
                        //audio.write(signal.toShortArray(), 0, signal.size)
                        //audio.play()
                        //signal = Vector()
                        this.space_len?.let { sleep(it.toLong()) }
                    }

                }
                if ((s == Signal.dot || s == Signal.dash) && next != Signal.space && next != Signal.inter_character_gap)
                    if (intra_char_gap_sound != null) {
                        audio.write(intra_char_gap_sound, 0, intra_char_gap_sound.size)
                        audio.play()
                    }//signal.addAll(intra_char_gap_sound) //
                    //sleep(intra_character_gap_siglen)
                if (i == msg.size - 1) {
                    //audio.write(signal.toShortArray(), 0, signal.size)
                    //signal = Vector()
                    //audio.play()
                }

            }

        }
    }

    public fun sendMessage(message:String){

        this.message_queue.put(this.translate(message))

    }

}