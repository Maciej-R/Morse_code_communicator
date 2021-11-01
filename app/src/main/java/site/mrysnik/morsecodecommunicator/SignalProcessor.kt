package site.mrysnik.morsecodecommunicator

import com.matthewrussell.trwav.readIntLE
import java.io.File
import java.io.RandomAccessFile

@ExperimentalUnsignedTypes
class SignalProcessor(audioFs: Int, wpm: Float): Parameters() {
    private var audioData: ByteArray = ByteArray(0)
    private val delta = 1u
    private val unitSignalLength = (this.samplingRate?.times(unit_len!!))?.toInt()?.div(1000)
    private val streakFraction = 0.9

    // Ths companion object implements singleton logic for this class
    companion object {
        private var instance: SignalProcessor? = null
        fun getInstance(audioFs: Int = samplingRate, wpm: Float = default_wpm): SignalProcessor {
            if (this.instance != null)
                return this.instance!!
            return SignalProcessor(audioFs, wpm)
        }
    }

    fun process() {
        // mocked data - beginning
        val file = RandomAccessFile(File("/storage/emulated/0/Music/20wpm_1000hz.wav"), "r")
        file.seek(40)
        val audioLength = file.readIntLE().toLong()
        audioData = ByteArray(audioLength.toInt())
        file.read(audioData)
        // mocked data - ending

        // TODO Bandpass filter

        val ml = quantize(audioData)

        val test2 = numStreak(ml)

        // TODO Better synchronisation
        test2.forEach { if(it[0] == 1) it[0] =0 else it[0] = 1 }

        val symbols = num2symbols(test2)
        println(symbols2char(symbols))

    }

    private fun numStreak(list: List<Int>): List<IntArray> {
        var num: IntArray = intArrayOf(list[0], 0)
        val numberStreaks: MutableList<IntArray> = mutableListOf()

        list.forEach {
            if(it == num[0]) {
                ++num[1]
            } else {
                numberStreaks.add(num)
                num = intArrayOf(it, 1)
            }
        }
        return reduceShortStreaks(numberStreaks)
    }

    private fun reduceShortStreaks(list: List<IntArray>): List<IntArray> {
        val numberStreaks: MutableList<IntArray> = mutableListOf()
        var ones = 0; var zeroes = 0 ; var sum = 0
        val limit = streakFraction * unitSignalLength!!

        list.forEach {
            sum += it[1]
            if (it[0] == 0) zeroes += it[1] else ones += it[1]
            if(sum > limit){
                val num = if (zeroes > ones) 0 else 1
                numberStreaks.add(intArrayOf(num, sum))
                ones = 0; zeroes = 0; sum = 0
            }
        }

        return numberStreaks
    }

    private fun num2symbols(list: List<IntArray>): String {
        var symbols = String()

        if (unitSignalLength != null) {
            list.forEach {
                symbols += when {
                    it[1] < unitSignalLength * 2 -> { if( it[0] == 1)  "." else "" }
                    it[1] < unitSignalLength * 5 -> { if( it[0] == 1)  "-" else " " }
                    else -> { "\t" }
                }
            }
        }

        return symbols
    }

    private fun symbols2char(symbols: String): String {
        var characters = ""

        val reversedDictionary = dict.entries.associate{ (k,v)-> v to k}
        symbols.split('\t').forEach { word ->
            word.split(' ').forEach{ character ->
                characters += reversedDictionary[character]
            }
            characters += ' '
        }

        return characters
    }

    @ExperimentalUnsignedTypes
    private fun quantize(audioData: ByteArray): List<Int> {
        val quantizedData: MutableList<Int> = mutableListOf()

        audioData.toList().forEach {
            if(it.toUByte() > 128u - delta && it.toUByte() < 128u + delta){
                quantizedData.add(1)
            } else {
                quantizedData.add(0)
            }
        }

        return quantizedData
    }
}