package com.drokka.emu.symicon.symfascinate

import android.graphics.Bitmap
import java.nio.ByteBuffer


fun Bitmap.convertToByteArray(): ByteArray {
    //minimum number of bytes that can be used to store this bitmap's pixels
    val size = this.byteCount

    //allocate new instances which will hold bitmap
    val buffer = ByteBuffer.allocate(size)
    val bytes = ByteArray(size)

    //copy the bitmap's pixels into the specified buffer
    this.copyPixelsToBuffer(buffer)

    //rewinds buffer (buffer position is set to zero and the mark is discarded)
    buffer.rewind()

    //transfer bytes from buffer into the given destination array
    buffer.get(bytes)

    //return bitmap's pixels
    return bytes
}

fun ByteArray.convertToBitmap(width:Int, height:Int, configBmpName:String): Bitmap{
    val configBmp = Bitmap.Config.valueOf(configBmpName)
    val bitmap = Bitmap.createBitmap(width, height, configBmp)
    val buffer = ByteBuffer.wrap(this)
    bitmap.copyPixelsFromBuffer(buffer)
    return  bitmap
}