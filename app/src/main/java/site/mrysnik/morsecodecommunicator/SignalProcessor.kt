package site.mrysnik.morsecodecommunicator

import com.matthewrussell.trwav.readIntLE
import uk.me.berndporr.iirj.Butterworth
import java.io.File
import java.io.RandomAccessFile
import kotlin.concurrent.thread
import kotlin.math.absoluteValue

@ExperimentalUnsignedTypes
class SignalProcessor(audioFs: Int, wpm: Float): Parameters(wpm = 20f) {
    // Ths companion object implements singleton logic for this class
    companion object {
        private var instance: SignalProcessor? = null
        fun getInstance(audioFs: Int = Parameters.samplingRate, wpm: Float = Parameters.default_wpm): SignalProcessor {
            if (this.instance != null)
                return this.instance!!
            return SignalProcessor(audioFs, wpm)
        }
    }

    private var audioData: ByteArray = ByteArray(0)
    //private val delta = 0.03
    private val delta = 1
    private val unitSignalLength = (Parameters.samplingRate * unit_len!!).toInt()/1000
    private val streakFraction = 0.9
//    private val processor: Thread = thread(true, false, null, "Signal processor", -1) { this.process() }

    fun process(inputFilePath: String) {
        val file = RandomAccessFile(File(inputFilePath), "r")
        file.seek(40)
        val audioLength = file.readIntLE().toLong()
        audioData = ByteArray(audioLength.toInt())
        file.read(audioData)

        var audioData2: MutableList<Double> = mutableListOf()

        audioData.forEach {
            audioData2.add(it.toDouble())
        }

        //println(audioData2)
        //val step1 = filter(audioData2)
        val step2 = digitize(audioData2)
        //val step2 = digitize(step1)
        //println(step2)
        val step3 = numStreak(step2)
        var test: MutableList<Int> = mutableListOf()
        step3.forEach {
            test.add(it[1])
        }
        println(test)

        println(test.sorted().takeLast(100))

        test = mutableListOf()
        step3.forEach {
            test.add(it[0])
        }
        //println(test)
        val step4 = reduceShortStreaks(step3)

        test = mutableListOf()
        step4.forEach {
            test.add(it[1])
        }
        println(test)

        test = mutableListOf()
        step4.forEach {
            test.add(it[0])
        }
        println(test)
        val step5 = num2symbols(step4)
        println(step5)
        println(symbols2char(step5))
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
        return numberStreaks
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


/*
            sum += it[1]
            if (it[1] < 500 || sum < limit) {
                if(it[0] == 0) zeroes += it[1] else ones += it[1]
            } else {
                val num = if (zeroes > ones) 0 else 1
                numberStreaks.add(intArrayOf(num, sum))
                ones = 0; zeroes = 0; sum = 0
            }*/
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
    private fun digitize(audioData: List<Double>): List<Int> {
        val quantizedData: MutableList<Int> = mutableListOf()

        audioData.toList().forEach {
            if(it > -128.0 - delta && it < -128.0 + delta){
            //if(it > -delta && it < delta){
                quantizedData.add(0)
            } else {
                quantizedData.add(1)
            }
        }

        return quantizedData
    }

    private fun filter(audioData: List<Double>): List<Double> {
        val butterworth = Butterworth()
        butterworth.bandPass(200, Parameters.samplingRate.toDouble(), 1000.0,500.0)

        val filtered: MutableList<Double> = mutableListOf()
        audioData.forEach {
            val test = butterworth.filter(it)
            filtered.add(test)
        }
        return filtered
    }
}