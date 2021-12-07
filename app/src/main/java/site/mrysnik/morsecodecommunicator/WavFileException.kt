package site.mrysnik.morsecodecommunicator

import java.lang.Exception

/**
 * WavFileException class
 *
 * A.Greensted
 * http://www.labbookpages.co.uk
 *
 * File format is based on the information from
 * http://www.sonicspot.com/guide/wavefiles.html
 * http://www.blitter.com/~russtopia/MIDI/~jglatt/tech/wave.htm
 *
 * Version 1.0
 */
class WavFileException(message: String?) : Exception(message) {

    companion object {
        private const val serialVersionUID = 8106597985536609621L
    }
}