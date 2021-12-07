package site.mrysnik.morsecodecommunicator

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import androidx.annotation.RequiresApi
import java.lang.Thread.sleep
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread


/**
 * Singleton object responsible for all operation related to sound transmission
 * @constructor Reference SoundTransmitter.get(), if needed constructor will be called by it
 */
class SoundTransmitter private constructor(wpm: Float, freq: Short): Parameters() {

    // Ths companion object implements singleton logic for this class
    companion object {
        private var instance: SoundTransmitter? = null
        fun getInstance(wpm: Float = default_wpm, freq: Short = default_frequency): SoundTransmitter {
            if (this.instance != null)
                return this.instance!!
            return SoundTransmitter(wpm, freq)
        }
    }

    // Mapping characters to vector of bytes representing dots (0) and dashes (1)
    private var dict_numeric: MutableMap<Char, Vector<Byte>> = mutableMapOf<Char, Vector<Byte>>()
    // Used to queue messages that are to be transmitted
    private var message_queue: LinkedBlockingQueue<Vector<Signal>> = LinkedBlockingQueue<Vector<Signal>>()
    // Sound playing thread, immediately started
    @RequiresApi(Build.VERSION_CODES.S)
    private val player: Thread = thread(true, false, null, "Sound player", -1) { this.play() }
    // Value indicating when signal playing thread should quit
    private var kill: Boolean = false
    // Sampling frequency
    private var fs = 44100
    // Characters that can be transmitted using Morse code
    private val allowed_chars = Regex("[a-z0-9 ]*")

    init {
        // Transform signals from character representation to numeric representation
        for(e: Map.Entry<Char, String> in this.dict.entries){
            var vec: Vector<Byte> = Vector()
            for(c: Char in e.value){
                vec.add((if (c == '-') 1 else 0))   // Dash to 1, dot to 0
            }
            this.dict_numeric[e.key] = vec
        }
    }

    /**
     * Represents signals available in Morse code
     */
    enum class Signal{
        dot, dash, inter_character_gap, intra_character_gap, space
    }

    /**
     * Translates string messages to vectors of SoundTransmitter::Signal values
     * @param[message] String that is to be translated
     * @throws RuntimeException It does not perform input validation, expects clean data, if unexpected character is encountered then exception is thrown
     */
    private fun translate(message: String): Vector<Signal> {

        // Separate words
        val words: List<String> = message.split(' ')
        // Output of this function
        val res = Vector<Signal>()

        // Iterate through words
        for((i, word: String) in words.withIndex()){
            // Iterate through characters
            for((j, c: Char) in word.withIndex()) {
                val code: Vector<Byte> = this.dict_numeric[c] ?: throw RuntimeException("Wrong character: $c")
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

    /**
     * Function used by sound playing thread.
     * It awaits until translated messages appear in message_queue and then transmitts them.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun play(){

        // Variables declarations
        var use_fs: Int = 0
        var use_f: Short = 0
        var dot_sound: ShortArray? = null
        var dash_sound: ShortArray? = null
        var intra_char_gap_sound: ShortArray? = null
        var inter_char_gap_sound: ShortArray? = null
        var dot_siglen: Int? = null
        var dash_siglen: Int? = null
        var inter_character_gap_siglen: Int? = null
        var intra_character_gap_siglen: Int? = null
        var audio: AudioTrack? = null

        // Run until variable indicate end of process
        while(!this.kill){

            // Take next message or wait until it will be available
            var msg: Vector<Signal> = message_queue.take()

            // If necessary create sound samples to use
            if (this.fs != use_fs || this.freqency != use_f) {

                // Params influenced only by fs change
                if (this.fs != use_fs) {
                    // Update sampling frequency for which current sound samples are generated
                    use_fs = this.fs
                    // Signal length in ms to signal length in number of samples
                    dot_siglen = (this.fs * this.dot_len!!).toInt() / 1000
                    dash_siglen = (this.fs * this.dash_len!!).toInt() / 1000
                    inter_character_gap_siglen =
                        (this.fs * this.inter_char_gap_len!!).toInt() / 1000
                    intra_character_gap_siglen =
                        (this.fs * this.intra_char_gap_len!!).toInt() / 1000

                    // When sampling rate is changed new audio player is needed
                    audio = AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING)
                                .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(this.fs)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(inter_character_gap_siglen * 3)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build()
                    audio.setVolume(0.25F)
                }

                // Generate sound samples - influenced by both fs or f change
                dot_sound = dot_siglen?.let {
                    ShortArray(it) { i -> ((kotlin.math.sin(2 * Math.PI * i * this.freqency / this.fs)) * Short.MAX_VALUE).toShort()} }
                dash_sound = dash_siglen?.let {
                    ShortArray(it) { i -> ((kotlin.math.sin(2 * Math.PI * i * this.freqency / this.fs)) * Short.MAX_VALUE).toShort()} }
                intra_char_gap_sound = intra_character_gap_siglen?.let { ShortArray(it) }
                inter_char_gap_sound = inter_character_gap_siglen?.let { ShortArray(it) }

            }

            val s_len = msg.size
            // Iterates through signals and transmits them
            for((i, s: Signal) in msg.withIndex()){

                var next: Signal? = null
                if (i < s_len-1) next = msg[i+1]
                when(s){

                    Signal.dot -> {
                        if (dot_sound != null) {
                            audio!!.flush()
                            audio.write(dot_sound, 0, dot_sound.size)
                            audio.play()
                        }
                    }
                    Signal.dash -> {
                        if (dash_sound != null) {
                            audio!!.flush()
                            audio.write(dash_sound, 0, dash_sound.size)
                            audio.play()
                        }
                    }
                    Signal.inter_character_gap -> {
                        if (inter_char_gap_sound != null) {
                            audio!!.flush()
                            audio.write(inter_char_gap_sound, 0, inter_char_gap_sound.size)
                            audio.play()
                        }
                    }
                    Signal.space -> {
                        this.space_len?.let { sleep(it.toLong()) }
                    }

                }
                // If currently signals from within one character are transmitted then
                // leave appropriate silent gap between them
                if ((s == Signal.dot || s == Signal.dash) && next != Signal.space && next != Signal.inter_character_gap)
                    if (intra_char_gap_sound != null) {
                        // Using sleep() for such a short breaks introduces distortions
                        audio!!.write(intra_char_gap_sound, 0, intra_char_gap_sound.size)
                        audio.play()
                    }

            }

        }
    }

    /**
     *  Function called by external agents to queue given message for transmission
     *  All characters from @param[message] are converted to lower case and all whitespaces to space character
     *  @param[message] Message to be sent
     *  @throws RuntimeException When string does not match expected format
     */
    fun sendMessage(message:String){

        // Morse code does not differentiate lower and upper case
        var msg = message.toLowerCase()
        // Convert all whitespaces different than space to space
        msg = msg.replace("[\t\n\r]", " ")
        if (allowed_chars.matches(msg))
            this.message_queue.put(this.translate(msg)) // Queue message
        else
            throw java.lang.RuntimeException("Unexpected string format")
    }

}