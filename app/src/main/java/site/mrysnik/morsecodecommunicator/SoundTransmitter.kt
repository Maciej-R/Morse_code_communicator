package site.mrysnik.morsecodecommunicator

import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import java.lang.RuntimeException
import java.util.*

/**
 * Singleton object responsible for all operation related to sound transmission
 * @constructor
 */
object SoundTransmitter: Parameters() {

    private var dict_numeric: MutableMap<Char, Vector<Byte>>? = null

    init {

        for(e: Map.Entry<Char, String> in this.dict.entries){

            var vec: Vector<Byte> = Vector<Byte>()
            for(c: Char in e.value){
                vec.add((if (c == '-') 1 else 0))
            }
            this.dict_numeric?.put(e.key, vec)

        }

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
            if(i != words.size) res.add(Signal.space)
        }

        return res

    }

    private fun play(){


    }

    public fun sendMessage(message:String){

    }

}