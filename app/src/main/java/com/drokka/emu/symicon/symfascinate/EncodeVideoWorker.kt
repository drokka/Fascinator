package com.drokka.emu.symicon.symfascinate

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.time.LocalDateTime
import java.time.ZoneOffset

class EncodeVideoWorker(context: Context, params: WorkerParameters)
    : CoroutineWorker(context, params) {

    var encoder: MediaCodec
    var mMuxer: MediaMuxer? = null
    var muxerStarted = false
    var trackIndex = -1

    var format: MediaFormat

    val sz = 480 //ouch
    val KEY_BIT_RATE = 1200000
    val KEY_FRAME_RATE = 12
    val KEY_I_FRAME_INTERVAL = 0


    init {

        format = MediaFormat.createVideoFormat("video/avc", sz, sz) //viewModel.sz, viewModel.sz )

        format.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )

        format.setInteger(MediaFormat.KEY_BIT_RATE, KEY_BIT_RATE) //viewModel.sz * viewModel.sz)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, KEY_FRAME_RATE)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, KEY_I_FRAME_INTERVAL)

        val encoderType = MediaCodecList(MediaCodecList.ALL_CODECS).findEncoderForFormat(format)
        encoder = MediaCodec.createByCodecName(encoderType)
        //encoder = MediaCodec.createEncoderByType("video/avc")

      //  encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

    }

    override suspend fun doWork(): Result {
        val cacheDir = inputData.getString("cacheDir")
        val filesDir = inputData.getString("filesDir")
        var count = 0;
        val imagesDirPath = cacheDir?.let { File(it) }
        if(imagesDirPath == null || !imagesDirPath.exists()) return Result.failure()
        val filesDirPath = filesDir?.let { File(it) }
        if(filesDirPath == null || !filesDirPath.exists()) return Result.failure()

        val paint = Paint()
        val saveFile = File(
            filesDir, "symvid" + LocalDateTime.now().toEpochSecond(
                ZoneOffset.of("Z")
            ).toString() + ".mp4"
        ).toString()

         mMuxer = MediaMuxer(saveFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val vidSurface = encoder.createInputSurface()
        encoder.start()

        while(true) { //Loop over all mages
            var imFile = File(imagesDirPath, "bitmap_$count.png")
            count++  //Keeps going until finds next indexed png file.

            if(imFile.exists()){ // image file exists

                val bitmap = BitmapFactory.decodeFile(imFile.absolutePath) ?: continue  //process next
                val canv = vidSurface.lockCanvas(Rect(0, 0, sz, sz))
                canv?.drawBitmap(bitmap, Rect(0, 0, sz, sz), Rect(0, 0, sz, sz), paint)
                vidSurface.unlockCanvasAndPost(canv)

                val bufferInfo = MediaCodec.BufferInfo()
                var outBufferId = encoder.dequeueOutputBuffer(bufferInfo, 10)

                while (outBufferId >= 0) {
                    val encodedBuffer = encoder.getOutputBuffer(outBufferId)

                    encodedBuffer?.also {
                        if (!muxerStarted) {

                            val mformat = encoder.outputFormat
                            trackIndex = mMuxer!!.addTrack(mformat)
                            mMuxer!!.start()
                            muxerStarted = true
                        }

                        mMuxer!!.writeSampleData(trackIndex, encodedBuffer, bufferInfo)
                        Log.d("makeVidWorker", "muxer is writing buffer for cache $cacheDir")

                    }
                    encoder.releaseOutputBuffer(outBufferId, false)

                    outBufferId = encoder.dequeueOutputBuffer(bufferInfo, 10)
                }

                imFile.delete()
                Log.d("makeVidWorker", "deleted ${imFile.absolutePath}")
            }
            else if(imagesDirPath.list()?.isEmpty() == true){ //No more bitmaps
                Log.d("makeVidWorker","no more bitmap files")
                   try {
                        encoder.signalEndOfInputStream()
                        encoder.flush()
                    } catch (xx: java.lang.Exception) {
                        Log.d("makeVidWorker", "Exception encoder signalEndofInput: ${xx.message}")
                        encoder.reset()
                    }
                    encoder.stop()
                  if ( mMuxer != null) {
                      try {
                          mMuxer!!.stop()
                      }catch (xx: java.lang.Exception) {
                          Log.d("makeVidWorker", "Exception encoder mMuxer stop: ${xx.message}")
                      }
                    mMuxer!!.release()
                }
                encoder.release()

                try {
                    imagesDirPath.delete()     //delete the empty directory
                }catch (xx: java.lang.Exception) {
                    Log.d("makeVidWorker", "Exception imagesDirPath.delete: ${xx.message}")
                }
                break
            }
        }
        val returnData = Data.Builder().putString("saveFile", saveFile).build()
        Log.d("makeVidWorker", "success for $saveFile")
            return Result.success(returnData)
    }
}