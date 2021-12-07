package site.mrysnik.morsecodecommunicator

import java.io.*
import java.lang.Exception
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import kotlin.experimental.and
import kotlin.system.exitProcess

/**
 * WavFile class
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


class WavFile private constructor() {
    private enum class IOState {
        READING, WRITING, CLOSED
    }

    private var file // File that will be read from or written to
            : File? = null
    private var ioState // Specifies the IO State of the Wav File (used for snaity checking)
            : IOState? = null
    private var bytesPerSample // Number of bytes required to store a single sample
            = 0
    var numFrames // Number of frames within the data section
            : Long = 0
        private set
    private var oStream // Output stream used for writting data
            : FileOutputStream? = null
    private var iStream // Input stream used for reading data
            : FileInputStream? = null
    private var floatScale // Scaling factor used for int <-> float conversion
            = 0.0
    private var floatOffset // Offset factor used for int <-> float conversion
            = 0.0
    private var wordAlignAdjust // Specify if an extra byte at the end of the data chunk is required for word alignment
            = false

    // Wav Header
    var numChannels // 2 bytes unsigned, 0x0001 (1) to 0xFFFF (65,535)
            = 0
        private set
    var sampleRate // 4 bytes unsigned, 0x00000001 (1) to 0xFFFFFFFF (4,294,967,295)
            : Long = 0
        private set

    // Although a java int is 4 bytes, it is signed, so need to use a long
    private var blockAlign // 2 bytes unsigned, 0x0001 (1) to 0xFFFF (65,535)
            = 0
    var validBits // 2 bytes unsigned, 0x0002 (2) to 0xFFFF (65,535)
            = 0
        private set

    // Buffering
    private val buffer // Local buffer used for IO
            : ByteArray
    private var bufferPointer // Points to the current position in local buffer
            = 0
    private var bytesRead // Bytes read after last read into local buffer
            = 0
    var frameAlreadyRead // Current number of frames read or written
            : Long = 0
        private set
    val framesRemaining: Long
        get() = numFrames - frameAlreadyRead

    // Sample Writing and Reading
    // --------------------------
    @Throws(IOException::class)
    private fun writeSample(`val`: Long) {
        var `val` = `val`
        for (b in 0 until bytesPerSample) {
            if (bufferPointer == BUFFER_SIZE) {
                oStream!!.write(buffer, 0, BUFFER_SIZE)
                bufferPointer = 0
            }
            buffer[bufferPointer] = (`val` and 0xFF).toByte()
            `val` = `val` shr 8
            bufferPointer++
        }
    }

    @Throws(IOException::class, WavFileException::class)
    private fun readSample(): Long {
        var `val`: Long = 0
        for (b in 0 until bytesPerSample) {
            if (bufferPointer == bytesRead) {
                val read = iStream!!.read(buffer, 0, BUFFER_SIZE)
                if (read == -1) throw WavFileException("Not enough data available")
                bytesRead = read
                bufferPointer = 0
            }
            var v = buffer[bufferPointer].toInt()
            if (b < bytesPerSample - 1 || bytesPerSample == 1) v = v and 0xFF
            `val` += (v shl b * 8).toLong()
            bufferPointer++
        }
        return `val`
    }

    // Integer
    // -------
    @Throws(IOException::class, WavFileException::class)
    fun readFrames(sampleBuffer: IntArray, numFramesToRead: Int): Int {
        return readFrames(sampleBuffer, 0, numFramesToRead)
    }

    @Throws(IOException::class, WavFileException::class)
    fun readFrames(sampleBuffer: IntArray, offset: Int, numFramesToRead: Int): Int {
        var offset = offset
        if (ioState != IOState.READING) throw IOException("Cannot read from WavFile instance")
        for (f in 0 until numFramesToRead) {
            if (frameAlreadyRead == numFrames) return f
            for (c in 0 until numChannels) {
                sampleBuffer[offset] = readSample().toInt()
                offset++
            }
            frameAlreadyRead++
        }
        return numFramesToRead
    }

    @Throws(IOException::class, WavFileException::class)
    fun readFrames(sampleBuffer: Array<IntArray>, numFramesToRead: Int): Int {
        return readFrames(sampleBuffer, 0, numFramesToRead)
    }

    @Throws(IOException::class, WavFileException::class)
    fun readFrames(sampleBuffer: Array<IntArray>, offset: Int, numFramesToRead: Int): Int {
        var offset = offset
        if (ioState != IOState.READING) throw IOException("Cannot read from WavFile instance")
        for (f in 0 until numFramesToRead) {
            if (frameAlreadyRead == numFrames) return f
            for (c in 0 until numChannels) sampleBuffer[c][offset] = readSample().toInt()
            offset++
            frameAlreadyRead++
        }
        return numFramesToRead
    }

    @Throws(IOException::class, WavFileException::class)
    fun writeFrames(sampleBuffer: IntArray, numFramesToWrite: Int): Int {
        return writeFrames(sampleBuffer, 0, numFramesToWrite)
    }

    @Throws(IOException::class, WavFileException::class)
    fun writeFrames(sampleBuffer: IntArray, offset: Int, numFramesToWrite: Int): Int {
        var offset = offset
        if (ioState != IOState.WRITING) throw IOException("Cannot write to WavFile instance")
        for (f in 0 until numFramesToWrite) {
            if (frameAlreadyRead == numFrames) return f
            for (c in 0 until numChannels) {
                writeSample(sampleBuffer[offset].toLong())
                offset++
            }
            frameAlreadyRead++
        }
        return numFramesToWrite
    }

    @Throws(IOException::class, WavFileException::class)
    fun writeFrames(sampleBuffer: Array<IntArray>, numFramesToWrite: Int): Int {
        return writeFrames(sampleBuffer, 0, numFramesToWrite)
    }

    @Throws(IOException::class, WavFileException::class)
    fun writeFrames(sampleBuffer: Array<IntArray>, offset: Int, numFramesToWrite: Int): Int {
        var offset = offset
        if (ioState != IOState.WRITING) throw IOException("Cannot write to WavFile instance")
        for (f in 0 until numFramesToWrite) {
            if (frameAlreadyRead == numFrames) return f
            for (c in 0 until numChannels) writeSample(sampleBuffer[c][offset].toLong())
            offset++
            frameAlreadyRead++
        }
        return numFramesToWrite
    }

    // Long
    // ----
    @Throws(IOException::class, WavFileException::class)
    fun readFrames(sampleBuffer: LongArray, numFramesToRead: Int): Int {
        return readFrames(sampleBuffer, 0, numFramesToRead)
    }

    @Throws(IOException::class, WavFileException::class)
    fun readFrames(sampleBuffer: LongArray, offset: Int, numFramesToRead: Int): Int {
        var offset = offset
        if (ioState != IOState.READING) throw IOException("Cannot read from WavFile instance")
        for (f in 0 until numFramesToRead) {
            if (frameAlreadyRead == numFrames) return f
            for (c in 0 until numChannels) {
                sampleBuffer[offset] = readSample()
                offset++
            }
            frameAlreadyRead++
        }
        return numFramesToRead
    }

    @Throws(IOException::class, WavFileException::class)
    fun readFrames(sampleBuffer: Array<LongArray>, numFramesToRead: Int): Int {
        return readFrames(sampleBuffer, 0, numFramesToRead)
    }

    @Throws(IOException::class, WavFileException::class)
    fun readFrames(sampleBuffer: Array<LongArray>, offset: Int, numFramesToRead: Int): Int {
        var offset = offset
        if (ioState != IOState.READING) throw IOException("Cannot read from WavFile instance")
        for (f in 0 until numFramesToRead) {
            if (frameAlreadyRead == numFrames) return f
            for (c in 0 until numChannels) sampleBuffer[c][offset] = readSample()
            offset++
            frameAlreadyRead++
        }
        return numFramesToRead
    }

    @Throws(IOException::class, WavFileException::class)
    fun writeFrames(sampleBuffer: LongArray, numFramesToWrite: Int): Int {
        return writeFrames(sampleBuffer, 0, numFramesToWrite)
    }

    @Throws(IOException::class, WavFileException::class)
    fun writeFrames(sampleBuffer: LongArray, offset: Int, numFramesToWrite: Int): Int {
        var offset = offset
        if (ioState != IOState.WRITING) throw IOException("Cannot write to WavFile instance")
        for (f in 0 until numFramesToWrite) {
            if (frameAlreadyRead == numFrames) return f
            for (c in 0 until numChannels) {
                writeSample(sampleBuffer[offset])
                offset++
            }
            frameAlreadyRead++
        }
        return numFramesToWrite
    }

    @Throws(IOException::class, WavFileException::class)
    fun writeFrames(sampleBuffer: Array<LongArray>, numFramesToWrite: Int): Int {
        return writeFrames(sampleBuffer, 0, numFramesToWrite)
    }

    @Throws(IOException::class, WavFileException::class)
    fun writeFrames(sampleBuffer: Array<LongArray>, offset: Int, numFramesToWrite: Int): Int {
        var offset = offset
        if (ioState != IOState.WRITING) throw IOException("Cannot write to WavFile instance")
        for (f in 0 until numFramesToWrite) {
            if (frameAlreadyRead == numFrames) return f
            for (c in 0 until numChannels) writeSample(sampleBuffer[c][offset])
            offset++
            frameAlreadyRead++
        }
        return numFramesToWrite
    }

    // Double
    // ------
    @Throws(IOException::class, WavFileException::class)
    fun readFrames(sampleBuffer: DoubleArray, numFramesToRead: Int): Int {
        return readFrames(sampleBuffer, 0, numFramesToRead)
    }

    @Throws(IOException::class, WavFileException::class)
    fun readFrames(sampleBuffer: DoubleArray, offset: Int, numFramesToRead: Int): Int {
        var offset = offset
        if (ioState != IOState.READING) throw IOException("Cannot read from WavFile instance")
        for (f in 0 until numFramesToRead) {
            if (frameAlreadyRead == numFrames) return f
            for (c in 0 until numChannels) {
                sampleBuffer[offset] = floatOffset + readSample().toDouble() / floatScale
                offset++
            }
            frameAlreadyRead++
        }
        return numFramesToRead
    }

    @Throws(IOException::class, WavFileException::class)
    fun readFrames(sampleBuffer: Array<DoubleArray>, numFramesToRead: Int): Int {
        return readFrames(sampleBuffer, 0, numFramesToRead)
    }

    @Throws(IOException::class, WavFileException::class)
    fun readFrames(sampleBuffer: Array<DoubleArray>, offset: Int, numFramesToRead: Int): Int {
        var offset = offset
        if (ioState != IOState.READING) throw IOException("Cannot read from WavFile instance")
        for (f in 0 until numFramesToRead) {
            if (frameAlreadyRead == numFrames) return f
            for (c in 0 until numChannels) sampleBuffer[c][offset] =
                floatOffset + readSample().toDouble() / floatScale
            offset++
            frameAlreadyRead++
        }
        return numFramesToRead
    }

    @Throws(IOException::class, WavFileException::class)
    fun writeFrames(sampleBuffer: DoubleArray, numFramesToWrite: Int): Int {
        return writeFrames(sampleBuffer, 0, numFramesToWrite)
    }

    @Throws(IOException::class, WavFileException::class)
    fun writeFrames(sampleBuffer: DoubleArray, offset: Int, numFramesToWrite: Int): Int {
        var offset = offset
        if (ioState != IOState.WRITING) throw IOException("Cannot write to WavFile instance")
        for (f in 0 until numFramesToWrite) {
            if (frameAlreadyRead == numFrames) return f
            for (c in 0 until numChannels) {
                writeSample((floatScale * (floatOffset + sampleBuffer[offset])).toLong())
                offset++
            }
            frameAlreadyRead++
        }
        return numFramesToWrite
    }

    @Throws(IOException::class, WavFileException::class)
    fun writeFrames(sampleBuffer: Array<DoubleArray>, numFramesToWrite: Int): Int {
        return writeFrames(sampleBuffer, 0, numFramesToWrite)
    }

    @Throws(IOException::class, WavFileException::class)
    fun writeFrames(sampleBuffer: Array<DoubleArray>, offset: Int, numFramesToWrite: Int): Int {
        var offset = offset
        if (ioState != IOState.WRITING) throw IOException("Cannot write to WavFile instance")
        for (f in 0 until numFramesToWrite) {
            if (frameAlreadyRead == numFrames) return f
            for (c in 0 until numChannels) writeSample((floatScale * (floatOffset + sampleBuffer[c][offset])).toLong())
            offset++
            frameAlreadyRead++
        }
        return numFramesToWrite
    }

    @Throws(IOException::class)
    fun close() {
        // Close the input stream and set to null
        if (iStream != null) {
            iStream!!.close()
            iStream = null
        }
        if (oStream != null) {
            // Write out anything still in the local buffer
            if (bufferPointer > 0) oStream!!.write(buffer, 0, bufferPointer)

            // If an extra byte is required for word alignment, add it to the end
            if (wordAlignAdjust) oStream!!.write(0)

            // Close the stream and set to null
            oStream!!.close()
            oStream = null
        }

        // Flag that the stream is closed
        ioState = IOState.CLOSED
    }

    /**
     * Method getInfo()
     *
     * return a String containing audio file informations
     *
     * @autor Matteo Benetti, mathew.benetti@gmail.com
     * @date  2013-12-25
     */
    val info: String
        get() {
            var info = ""
            info += "File: $file\n"
            info += "Channels: $numChannels, Frames: $numFrames\n"
            info += "IO State: $ioState\n"
            info += "Sample Rate: $sampleRate, Block Align: $blockAlign\n"
            info += "Valid Bits: $validBits, Bytes per sample: $bytesPerSample\n"
            return info
        }

    @JvmOverloads
    fun display(out: PrintStream = System.out) {
        out.printf("File: %s\n", file)
        out.printf("Channels: %d, Frames: %d\n", numChannels, numFrames)
        out.printf("IO State: %s\n", ioState)
        out.printf("Sample Rate: %d, Block Align: %d\n", sampleRate, blockAlign)
        out.printf("Valid Bits: %d, Bytes per sample: %d\n", validBits, bytesPerSample)
    }

    companion object {
        private const val BUFFER_SIZE = 4096
        private const val FMT_CHUNK_ID = 0x20746D66
        private const val DATA_CHUNK_ID = 0x61746164
        private const val RIFF_CHUNK_ID = 0x46464952
        private const val RIFF_TYPE_ID = 0x45564157

        @Throws(IOException::class, WavFileException::class)
        fun openWavFile(file: File): WavFile {
            // Instantiate new Wavfile and store the file reference
            val wavFile = WavFile()
            wavFile.file = file

            // Create a new file input stream for reading file data
            wavFile.iStream = FileInputStream(file)

            // Read the first 12 bytes of the file
            var bytesRead = wavFile.iStream!!.read(wavFile.buffer, 0, 12)
            if (bytesRead != 12) throw WavFileException("Not enough wav file bytes for header")

            // Extract parts from the header
            val riffChunkID = getLE(wavFile.buffer, 0, 4)
            var chunkSize = getLE(wavFile.buffer, 4, 4)
            val riffTypeID = getLE(wavFile.buffer, 8, 4)

            // Check the header bytes contains the correct signature
            if (riffChunkID != RIFF_CHUNK_ID.toLong()) throw WavFileException("Invalid Wav Header data, incorrect riff chunk ID")
            if (riffTypeID != RIFF_TYPE_ID.toLong()) throw WavFileException("Invalid Wav Header data, incorrect riff type ID")

            // Check that the file size matches the number of bytes listed in header
            if (file.length() != chunkSize + 8) {
                throw WavFileException("Header chunk size (" + chunkSize + ") does not match file size (" + file.length() + ")")
            }
            var foundFormat = false
            var foundData = false

            // Search for the Format and Data Chunks
            while (true) {
                // Read the first 8 bytes of the chunk (ID and chunk size)
                bytesRead = wavFile.iStream!!.read(wavFile.buffer, 0, 8)
                if (bytesRead == -1) throw WavFileException("Reached end of file without finding format chunk")
                if (bytesRead != 8) throw WavFileException("Could not read chunk header")

                // Extract the chunk ID and Size
                val chunkID = getLE(wavFile.buffer, 0, 4)
                chunkSize = getLE(wavFile.buffer, 4, 4)

                // Word align the chunk size
                // chunkSize specifies the number of bytes holding data. However,
                // the data should be word aligned (2 bytes) so we need to calculate
                // the actual number of bytes in the chunk
                var numChunkBytes = if (chunkSize % 2 == 1L) chunkSize + 1 else chunkSize
                if (chunkID == FMT_CHUNK_ID.toLong()) {
                    // Flag that the format chunk has been found
                    foundFormat = true

                    // Read in the header info
                    bytesRead = wavFile.iStream!!.read(wavFile.buffer, 0, 16)

                    // Check this is uncompressed data
                    val compressionCode = getLE(wavFile.buffer, 0, 2).toInt()
                    if (compressionCode != 1) throw WavFileException("Compression Code $compressionCode not supported")

                    // Extract the format information
                    wavFile.numChannels = getLE(wavFile.buffer, 2, 2).toInt()
                    wavFile.sampleRate = getLE(wavFile.buffer, 4, 4)
                    wavFile.blockAlign = getLE(wavFile.buffer, 12, 2).toInt()
                    wavFile.validBits = getLE(wavFile.buffer, 14, 2).toInt()
                    if (wavFile.numChannels == 0) throw WavFileException("Number of channels specified in header is equal to zero")
                    if (wavFile.blockAlign == 0) throw WavFileException("Block Align specified in header is equal to zero")
                    if (wavFile.validBits < 2) throw WavFileException("Valid Bits specified in header is less than 2")
                    if (wavFile.validBits > 64) throw WavFileException("Valid Bits specified in header is greater than 64, this is greater than a long can hold")

                    // Calculate the number of bytes required to hold 1 sample
                    wavFile.bytesPerSample = (wavFile.validBits + 7) / 8
                    if (wavFile.bytesPerSample * wavFile.numChannels != wavFile.blockAlign) throw WavFileException(
                        "Block Align does not agree with bytes required for validBits and number of channels"
                    )

                    // Account for number of format bytes and then skip over
                    // any extra format bytes
                    numChunkBytes -= 16
                    if (numChunkBytes > 0) wavFile.iStream!!.skip(numChunkBytes)
                } else if (chunkID == DATA_CHUNK_ID.toLong()) {
                    // Check if we've found the format chunk,
                    // If not, throw an exception as we need the format information
                    // before we can read the data chunk
                    if (foundFormat == false) throw WavFileException("Data chunk found before Format chunk")

                    // Check that the chunkSize (wav data length) is a multiple of the
                    // block align (bytes per frame)
                    if (chunkSize % wavFile.blockAlign != 0L) throw WavFileException("Data Chunk size is not multiple of Block Align")

                    // Calculate the number of frames
                    wavFile.numFrames = chunkSize / wavFile.blockAlign

                    // Flag that we've found the wave data chunk
                    foundData = true
                    break
                } else {
                    // If an unknown chunk ID is found, just skip over the chunk data
                    wavFile.iStream!!.skip(numChunkBytes)
                }
            }

            // Throw an exception if no data chunk has been found
            if (foundData == false) throw WavFileException("Did not find a data chunk")

            // Calculate the scaling factor for converting to a normalised double
            if (wavFile.validBits > 8) {
                // If more than 8 validBits, data is signed
                // Conversion required dividing by magnitude of max negative value
                wavFile.floatOffset = 0.0
                wavFile.floatScale = (1 shl wavFile.validBits - 1).toDouble()
            } else {
                // Else if 8 or less validBits, data is unsigned
                // Conversion required dividing by max positive value
                wavFile.floatOffset = -1.0
                wavFile.floatScale = 0.5 * ((1 shl wavFile.validBits) - 1)
            }
            wavFile.bufferPointer = 0
            wavFile.bytesRead = 0
            wavFile.frameAlreadyRead = 0
            wavFile.ioState = IOState.READING
            return wavFile
        }

        // Get and Put little endian data from local buffer
        // ------------------------------------------------
        private fun getLE(buffer: ByteArray, pos: Int, numBytes: Int): Long {
            var ret: Long = buffer[pos + numBytes - 1].toLong()
            for (b in 1 until numBytes)
                ret = (ret shl 8) or (buffer[pos + numBytes - 1 - b].toLong() and 0xFF)
            return ret
        }
    }

    // Cannot instantiate WavFile directly, must either use newWavFile() or openWavFile()
    init {
        buffer = ByteArray(BUFFER_SIZE)
    }
}