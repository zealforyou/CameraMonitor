package org.apache.ftpserver.filesystem.tfp.impl

import me.jahnen.libaums.core.fs.UsbFile
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer

/**
 * <p>Created by ZhangZhuo on 2022/7/13.</p>
 */
class UsbFtpOutputStream @JvmOverloads constructor(private val file: UsbFile, offset: Long = 0L) : OutputStream() {
    private var currentByteOffset: Long = 0

    init {
        if (file.isDirectory) {
            throw UnsupportedOperationException("UsbFileOutputStream cannot be created on directory!")
        }
        currentByteOffset = offset
    }

    @Throws(IOException::class)
    override fun write(oneByte: Int) {
        val byteBuffer = ByteBuffer.wrap(byteArrayOf(oneByte.toByte()))
        file.write(currentByteOffset, byteBuffer)

        currentByteOffset++
    }

    @Throws(IOException::class)
    override fun close() {
        file.length = currentByteOffset
        file.close()
    }

    @Throws(IOException::class)
    override fun flush() {
        file.flush()
    }

    @Throws(IOException::class)
    override fun write(buffer: ByteArray) {
        val byteBuffer = ByteBuffer.wrap(buffer)
        file.write(currentByteOffset, byteBuffer)

        currentByteOffset += buffer.size.toLong()
    }

    @Throws(IOException::class)
    override fun write(buffer: ByteArray, offset: Int, count: Int) {
        val byteBuffer = ByteBuffer.wrap(buffer)

        byteBuffer.position(offset)
        byteBuffer.limit(count + offset)

        file.write(currentByteOffset, byteBuffer)

        currentByteOffset += count.toLong()
    }
}
