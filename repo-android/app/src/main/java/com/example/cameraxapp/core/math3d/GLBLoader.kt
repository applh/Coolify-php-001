package com.example.cameraxapp.core.math3d

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Adapted from net.mgsx.gltf.loaders.glb.GLBLoader
 */
object GLBLoader {
    const val MAGIC = 0x46546C67 // "glTF"
    const val CHUNK_TYPE_JSON = 0x4E4F534A // "JSON"
    const val CHUNK_TYPE_BIN = 0x004E4942 // "BIN\0"

    data class GLBData(
        val version: Int,
        val length: Int,
        val json: String,
        val binMap: ByteArray?
    )

    fun parse(inputStream: InputStream): GLBData {
        val bytes = inputStream.readBytes()
        if (bytes.size < 20) throw Exception("Invalid GLB: File too small")

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val magic = buffer.int
        if (magic != MAGIC) {
            throw Exception("Invalid GLB magic: ${magic.toString(16)}")
        }

        val version = buffer.int
        val length = buffer.int

        // Search for JSON chunk because the file could be corrupted by UTF-8 conversion
        var jsonStartIndex = -1
        for (i in 8 until bytes.size - 4) {
            if (bytes[i] == 'J'.code.toByte() && bytes[i+1] == 'S'.code.toByte() && bytes[i+2] == 'O'.code.toByte() && bytes[i+3] == 'N'.code.toByte()) {
                jsonStartIndex = i - 4 // preceding 4 bytes is length
                break
            }
        }

        if (jsonStartIndex == -1) {
            throw Exception("Invalid GLB: Missing JSON chunk")
        }

        buffer.position(jsonStartIndex)
        val chunk0Length = buffer.int
        val chunk0Type = buffer.int
        
        // It's possible that the length itself is corrupted. We try to read until the next chunk or EOF.
        val jsonEndIndex = Math.min(bytes.size, jsonStartIndex + 8 + chunk0Length)
        val actualJsonLength = if (jsonEndIndex < bytes.size && chunk0Length > 0 && chunk0Length < 100000000) chunk0Length else {
            // Find next BIN string
            var nextBin = bytes.size
            for (i in jsonStartIndex + 8 until bytes.size - 3) {
                if (bytes[i] == 'B'.code.toByte() && bytes[i+1] == 'I'.code.toByte() && bytes[i+2] == 'N'.code.toByte() && bytes[i+3] == 0.toByte()) {
                    nextBin = i - 4
                    break
                }
            }
            nextBin - (jsonStartIndex + 8)
        }

        val jsonBytes = ByteArray(actualJsonLength)
        System.arraycopy(bytes, jsonStartIndex + 8, jsonBytes, 0, actualJsonLength)
        val jsonString = String(jsonBytes, Charsets.UTF_8).trimEnd { it.code == 0 || it.isWhitespace() }

        // Find BIN chunk
        var binBytes: ByteArray? = null
        var binStartIndex = -1
        for (i in jsonStartIndex + 8 until bytes.size - 3) {
             if (bytes[i] == 'B'.code.toByte() && bytes[i+1] == 'I'.code.toByte() && bytes[i+2] == 'N'.code.toByte() && bytes[i+3] == 0.toByte()) {
                 binStartIndex = i - 4
                 break
             }
        }

        if (binStartIndex != -1) {
            buffer.position(binStartIndex)
            val chunk1Length = buffer.int
            if (binStartIndex + 8 + chunk1Length <= bytes.size) {
                 binBytes = ByteArray(chunk1Length)
                 System.arraycopy(bytes, binStartIndex + 8, binBytes, 0, chunk1Length)
            } else {
                 val safeLen = bytes.size - (binStartIndex + 8)
                 if(safeLen > 0) {
                     binBytes = ByteArray(safeLen)
                     System.arraycopy(bytes, binStartIndex + 8, binBytes, 0, safeLen)
                 }
            }
        }

        return GLBData(version, length, jsonString, binBytes)
    }
}
