package site.mrysnik.morsecodecommunicator

/**
 * MorseDecoder
 *
 * This is a free software, you can do whatever you want
 * with this code. Credentials are appreciated.
 *
 * Files whitout this header are under specified license
 * and/or limits.
 *
 * This software can translate any morse code audio signal (.wav file)
 * into a human-readable text
 *
 * Donation (Litecoin): LYmQC9AMcvZq8dxQNkUBxngF6B2S2gafyX
 * Donation (Bitcoin):  1GEf7b2FjfCVSyoUdovePhKXe5yncqNZs7
 *
 * @author  Matteo Benetti, mathew.benetti@gmail.com
 * @version 2013-12-23
 */
class MorseSignal {
    var length = 0
    var frame_start: Long = 0
    var frame_end: Long = 0
    var isSignal = false
    var isSilence = false
    fun length_ms(): Int {
        return length * 10
    }

    val isValidSignal: Boolean
        get() = isSignal

}