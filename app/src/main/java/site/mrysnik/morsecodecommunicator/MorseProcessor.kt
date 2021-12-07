package site.mrysnik.morsecodecommunicator

import java.io.File
import java.io.IOException
import java.util.*

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


class MorseProcessor(filename: String?) {
    private val wavFile: WavFile = WavFile.openWavFile(File(filename))
    private val numChannels: Int

    //wav file audio channel number
    private val sampleRate: Int

    // rate of sampling
    private var framesRead = 0
    private var signal_medium_value = 0
    private var silence_medium_value = 0
    private val buffer: DoubleArray
    private var decodedString: String? = null

    /**
     * Close the wav resource
     *
     */
    @Throws(WavFileException::class, IOException::class)
    fun close() {
        wavFile.close()
    }

    /**
     * main method
     *
     */
    @Throws(WavFileException::class, IOException::class)
    fun process() {

        // this is the dictionary needed to translate the dot/dash string into
        // a human readable string
        val md = MorseDictionary()

        // get the sequence of signals (dot, dash) from the wav file
        val signals_list: List<MorseSignal> = signals

        // translate the signals sequence into a string of dot and dash grouped by words
        val cwstring = finalize(signals_list)
        val cwstring_iterator = cwstring.iterator()
        var cw_original = ""
        var cw_translated = ""
        while (cwstring_iterator.hasNext()) {
            val cw_word = cwstring_iterator.next()
            if (cw_word === "CSPACE") {
                cw_original += " "
                cw_translated += ""
            } else if (cw_word === "WSPACE") { // if the space is a word space is longer than normal
                cw_original += "   "
                cw_translated += " "
            } else {
                cw_original += cw_word // original message
                cw_translated += md.translate(cw_word) // translated message
            }
        }
        println("CW MESSAGE: $cw_original")
        println("MESSAGE: $cw_translated")
        decodedString = cw_translated
        close()
    }//continue;// read new signal// test if we have reached the end of the file// next couple of var are needed in order
    // to normalize the duration of signals/silence into
    // the stream
    /**
     * manage the seeking of signal into the wav
     *
     */
    @get:Throws(WavFileException::class, IOException::class)
    val signals: List<MorseSignal>
        get() {
            val signals: MutableList<MorseSignal> = ArrayList()

            // next couple of var are needed in order
            // to normalize the duration of signals/silence into
            // the stream
            var first_silence_flag = false
            var last_silence_flag = false
            while (wavFile.framesRemaining > 0) // test if we have reached the end of the file
            {
                val next_signal: MorseSignal = readNextSignal() // read new signal
                if (!first_silence_flag && next_signal.isSilence) {
                    first_silence_flag = true
                    next_signal.length = 1
                    continue
                }
                if (!last_silence_flag && wavFile.framesRemaining <= 0) {
                    last_silence_flag = true
                    next_signal.length = 50
                    //continue;
                }
                signals.add(next_signal)
            }
            return signals
        }

    /**
     * read a signal from the wav stream
     * a signal is composed by a bounch of sample, each one's length is about 10ms
     *
     */
    @Throws(WavFileException::class, IOException::class)
    fun readNextSignal(): MorseSignal {
        val signal = MorseSignal()
        signal.frame_start = wavFile.frameAlreadyRead
        val sample = readSample() // a positive value between 0 and 255
        signal.length++ //minimum signal length = 1 sample = 10ms
        if (sample > 0.01) { // if the sample value is greater than 0.01 it is a valid signal
            signal.isSignal = true
        } else {
            signal.isSilence = true
        }
        while (wavFile.framesRemaining > 0) {
            val tmp_sample = readSample() // read the next one signal
            signal.length++ // total length increases thanks to the tmp_sample read

            // now check current type of signal and the next type of signal and
            // determine what to do
            if (signal.isSilence && tmp_sample > 0.01 // if we got a silence and now a signal
                || signal.isSignal && tmp_sample <= 0.01
            ) // if we got a signal and now a silence
            {
                // signal changes read, delete last sample length from total length
                signal.length--
                signal.frame_end = wavFile.frameAlreadyRead - sampleRate
                return signal
            }
        }
        if (wavFile.framesRemaining <= 0) {
            signal.frame_end = wavFile.frameAlreadyRead
        }
        return signal
    }

    /**
     * read a sample of the wav file
     *
     */
    @Throws(WavFileException::class, IOException::class)
    private fun readSample(): Float {
        if (wavFile.framesRemaining > 0) {
            val positiveSamples: MutableList<Float> = ArrayList() // we want only positive value
            framesRead = wavFile.readFrames(buffer, sampleRate)
            if (framesRead != 0) {
                for (s in 0 until framesRead * numChannels) {
                    if (buffer[s] > 0) {
                        positiveSamples.add(buffer[s].toFloat())
                    }
                }
                if (positiveSamples.size <= 0) {
                    //add at least one value
                    positiveSamples.add(0.toFloat())
                }
                return get_list_average(positiveSamples)
            }
        }
        return 0.toFloat()
    }

    /**
     * parse the signals sequence and build words
     *
     */
    private fun finalize(signals: List<MorseSignal>): List<String> {
        var signal_total_value = 0
        var silence_total_value = 0
        val first_iterator: Iterator<MorseSignal> = signals.iterator()
        val second_iterator: Iterator<MorseSignal> = signals.iterator()
        val signals_length: MutableList<Int> = ArrayList()
        val silences_length: MutableList<Int> = ArrayList()
        while (first_iterator.hasNext()) {
            val signal: MorseSignal = first_iterator.next()
            if (!signal.isSilence && !seek(signals_length, signal.length * 10)) {
                signals_length.add(signal.length * 10)
                signal_total_value += signal.length * 10
            } else if (signal.isSilence && !seek(signals_length, signal.length * 10)) {
                silences_length.add(signal.length * 10)
                silence_total_value += signal.length * 10
            }
        }

        signal_medium_value = if (signals_length.size == 0) 0 else signal_total_value / signals_length.size
        silence_medium_value = if (silences_length.size == 0) 0 else silence_total_value / silences_length.size

        //System.out.println("signal med val: " + this.signal_medium_value);
        //System.out.println("silence med val: " + this.silence_medium_value);
        val cwstring: MutableList<String> = ArrayList()
        var cw_word = ""
        while (second_iterator.hasNext()) {
            val signal: MorseSignal = second_iterator.next()
            if (signal.isSignal) {
                cw_word += if (signal.length_ms() < signal_medium_value) {
                    //System.out.println(".");
                    "." //dot
                } else {
                    //System.out.println("-");
                    "-" //dash
                }
            } else if (signal.isSilence !== false) {
                //System.out.println("silence, length: " + signal.length_ms());
                // check if silence is a "long" silence beetween two words
                if (signal.length_ms() >= 2 * silence_medium_value) {
                    cwstring.add(cw_word)
                    cwstring.add("WSPACE")
                    cw_word = ""
                    continue
                }

                // check if silence is a short silence between two signals
                if (signal.length_ms() >= silence_medium_value) {
                    cwstring.add(cw_word)
                    cwstring.add("CSPACE")
                    cw_word = ""
                    continue
                }
            }
        }
        return cwstring // list of cw signals, '.' for dot and '-' for dash, grouped by words
    }

    fun seek(values: List<Int>, value: Int): Boolean {
        val i = values.iterator()
        while (i.hasNext()) {
            if (i.next() == value) {
                return true
            }
        }
        return false
    }

    private fun get_list_average(list: List<Float>): Float {
        var sum = 0f
        for (value in list) {
            sum += value
        }
        return (sum / list.size)
    }

    fun displayInfo() {
        println(wavFile.info)
    }

    fun result(): String? {
        return decodedString
    }

    override fun toString(): String {
        return wavFile.info
    }

    init {
        numChannels = wavFile.numChannels
        sampleRate = wavFile.sampleRate.toInt()
//        sampleRate = 44100 / 1000 * 10 // 44 frames per ms * 10 = 10ms sample length
        buffer = DoubleArray(sampleRate * numChannels)
    }
}