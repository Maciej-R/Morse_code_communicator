package site.mrysnik.morsecodecommunicator

import java.util.HashMap

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


class MorseDictionary {
    var dictionary: MutableMap<String, String?> = HashMap()
    fun translate(cw_char: String?): String? {
        return if (dictionary.containsKey(cw_char as Any?)) {
            dictionary[cw_char]
        } else {
            "NULL"
        }
    }

    init {
        dictionary[".-"] = "A"
        dictionary["-..."] = "B"
        dictionary["-.-."] = "C"
        dictionary["-.."] = "D"
        dictionary["."] = "E"
        dictionary["..-."] = "F"
        dictionary["--."] = "G"
        dictionary["...."] = "H"
        dictionary[".."] = "I"
        dictionary[".---"] = "J"
        dictionary["-.-"] = "K"
        dictionary[".-.."] = "L"
        dictionary["--"] = "M"
        dictionary["-."] = "N"
        dictionary["---"] = "O"
        dictionary[".--."] = "P"
        dictionary["--.-"] = "Q"
        dictionary[".-."] = "R"
        dictionary["..."] = "S"
        dictionary["-"] = "T"
        dictionary["..-"] = "U"
        dictionary["...-"] = "V"
        dictionary[".--"] = "W"
        dictionary["-..-"] = "X"
        dictionary["-.--"] = "Y"
        dictionary["--.."] = "Z"
        dictionary["-----"] = "0"
        dictionary[".----"] = "1"
        dictionary["..---"] = "2"
        dictionary["...--"] = "3"
        dictionary["....-"] = "4"
        dictionary["....."] = "5"
        dictionary["-...."] = "6"
        dictionary["--..."] = "7"
        dictionary["---.."] = "8"
        dictionary["----."] = "9"
        dictionary[".-.-.-"] = "."
        dictionary["--..--"] = ","
        dictionary["---..."] = ":"
        dictionary["..--.."] = "?"
        dictionary["-...-"] = "="
        dictionary["-....-"] = "-"
        dictionary["-.--."] = "("
        dictionary["-.--.-"] = ")"
        dictionary[".-..-."] = "\""
        dictionary[".----."] = "'"
        dictionary["-..-."] = "/"
        dictionary[".--.-."] = "@"
        dictionary["-.-.--"] = "!"
    }
}