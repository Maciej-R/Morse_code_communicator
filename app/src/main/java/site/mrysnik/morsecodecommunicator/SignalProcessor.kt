package site.mrysnik.morsecodecommunicator

import com.matthewrussell.trwav.readIntLE
import java.io.File
import java.io.RandomAccessFile

class SignalProcessor: Parameters() {
    private var audioData: ByteArray = ByteArray(0)


    fun test() {
        val file = RandomAccessFile(File("/storage/emulated/0/Music/smbj_20wpm_1000hz.wav"), "r")

        file.seek(40)
        val audioLength = file.readIntLE().toLong()

        audioData = ByteArray(audioLength.toInt())
        file.read(audioData)

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
            if(it.toUByte() > 127u && it.toUByte() < 129u){
                ml.add(1)
            } else {
                ml.add(0)
            }
        }

        var test2 = numStreak(ml)

        var test: MutableList<Int> = mutableListOf<Int>()
        test2.forEach {
            test.add(it[1])
        }

        println(test)

        test = mutableListOf<Int>()
        test2.forEach {
            test.add(it[0])
        }

        println(test)

        test2.forEach {
            if(it[0] == 1) it[0] =0 else it[0] = 1
        }

        var sumbols = num2symbols(test2)
        println(sybols2char(sumbols))

    }

    private fun numStreak(list: List<Int>): List<IntArray> {
        var num: IntArray = intArrayOf(list[0], 0)
        var ans: MutableList<IntArray> = mutableListOf<IntArray>()

        list.forEach {
            if(it == num[0]) {
                ++num[1]
            } else {
                ans.add(num)
                num = intArrayOf(it, 1)
            }
        }
        return reduceShortStreaks(ans)
    }

    private fun reduceShortStreaks(list: List<IntArray>): List<IntArray> {
        var ans: MutableList<IntArray> = mutableListOf<IntArray>()
        var ones = 0
        var zeroes = 0
        var sum = 0
        val limit = 0.9*663 // should be set to shortest symbol???
        list.forEach {
            sum += it[1]
            if (it[0] == 0) zeroes += it[1] else ones += it[1]
            if(sum > limit){
                var num = if (zeroes > ones) 0 else 1
                ans.add(intArrayOf(num, sum))
                ones = 0
                zeroes = 0
                sum = 0
            }
        }
        return ans
    }

    private fun num2symbols(list: List<IntArray>): String {
        var ans = String()
        list.forEach {
            ans += when {
                it[1] < 663 * 2 -> {
                    if( it[0] == 1)  "." else ""
                }
                it[1] < 663 * 5 -> {
                    if( it[0] == 1)  "-" else " "
                }
                else -> {
                    "\t"
                }
            }
        }

        return ans
    }

    private fun sybols2char(symbols: String): String {
        var ans = ""
        val reversed = dict.entries.associate{ (k,v)-> v to k}
        symbols.split('\t').forEach {
            it.split(' ').forEach{
                ans += reversed[it]
            }
            ans += ' '
        }
        return ans
    }
}