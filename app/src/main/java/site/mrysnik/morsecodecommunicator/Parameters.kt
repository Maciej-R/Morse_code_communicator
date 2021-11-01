package site.mrysnik.morsecodecommunicator

import java.util.*

open class Parameters(wpm: Float = Parameters.default_wpm, freq: Short = Parameters.default_frequency) {

    companion object {  // Default parameters
        const val default_wpm: Float = 5f
        const val default_frequency: Short = 500
        const val samplingRate: Int = 11050
    }
    private val s_w_len: Byte = 50  // Standard word "Paris " length
    private var wpm: Float  = wpm  // Words per minute
    protected var samplingRate: Int? = null // Audio sampling rate
    protected var unit_len: Float? = null // Basic unit length in milliseconds
    protected var freqency: Short = freq // Transmission frequency
    protected var space_len: Float? = null    // Word space length in milliseconds
    protected var intra_char_gap_len: Float? = null   // Gap between signals in one character length in milliseconds
    protected var inter_char_gap_len: Float? = null   // Gap between characters of one word length in milliseconds
    protected var dot_len: Float? = null  // Dot length in milliseconds
    protected var dash_len: Float? = null // Dash length in milliseconds
    // Dictionary translating characters to Morse code sequences
    protected val dict: Map<Char, String> = mapOf('a' to ".-", 'b' to "-...", 'c' to "-.-.", 'd' to "-..", 'e' to ".", 'f'
            to "..-.", 'g' to "--.", 'h' to "....", 'i' to "..", 'j' to ".---", 'k' to "-.-", 'l' to ".-..", 'm' to "--", 'n'
            to "-.", 'o' to "---", 'p' to ".--.", 'q' to "--.-", 'r' to ".-.", 's' to "...", 't' to "-", 'u' to "..-", 'v' to "...-", 'w' to ".--", 'x'
            to "-..-", 'y' to "-.--", 'z' to "--..", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-", '5' to ".....", '6' to "-....", '7'
            to "--...", '8' to "---..", '9' to "----.", '0' to "-----")

    init {
        calulate_unit_length()
    }

    private fun calulate_unit_length() {
        this.unit_len = 60000f / (this.wpm * s_w_len)
        this.space_len = this.unit_len!! * 7
        this.intra_char_gap_len = this.unit_len
        this.inter_char_gap_len = this.unit_len!! * 3
        this.dot_len = this.unit_len
        this.dash_len = this.unit_len!! * 3
    }

    fun set_wpm(wpm: Float){
        this.wpm = wpm
        this.calulate_unit_length()
    }

    fun set_frequency(f: Short){
        this.freqency
    }

}