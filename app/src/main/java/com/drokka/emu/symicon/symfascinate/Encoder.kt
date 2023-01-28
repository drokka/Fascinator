package com.drokka.emu.symicon.symfascinate

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import android.view.Surface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneOffset

class Encoder(val vidFileList: ArrayList<String>, sz: Int = 480, bitRate: Int = 2600000, frameRate: Int = 20) {

    var encoder: MediaCodec? = null
    var mMuxer: MediaMuxer? = null
    var muxerStarted = false
    var encoderStarted = false
    var trackIndex = -1
    var saveFile = false
    var videoFilePath = ""
    var initialising = false

    var format: MediaFormat


    val KEY_I_FRAME_INTERVAL = 1
    var filesDirPath: File? = null
    var vidSurface: Surface? = null
    val paint = Paint()
    val height:Int
    val width:Int
    val encoderType:String

    val KEY_FRAME_RATE:Int
    val KEY_BIT_RATE:Int

    private var presentationTimeUs:Long = 0
    init {
        height = sz
        width = sz
        KEY_BIT_RATE = bitRate
        KEY_FRAME_RATE = frameRate

        // basic set up for format HERE means encoderType only seeked once.
        // Even though format gets reinitialised in initEncoder. It has to be reinitialised
        // each time encoder is otherwise format is not in the correct state for
        // configure call. And you have to call configure on a new or previously stopped encoder.
        format = MediaFormat.createVideoFormat("video/avc", width, height)
        format.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )

        format.setInteger(MediaFormat.KEY_BIT_RATE, KEY_BIT_RATE)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, KEY_FRAME_RATE)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, KEY_I_FRAME_INTERVAL)

        encoderType = MediaCodecList(MediaCodecList.ALL_CODECS).findEncoderForFormat(format)


    }

    fun initEncoder() {
        if ( initialising || filesDirPath == null || !filesDirPath?.exists()!!) return

        initialising = true

        presentationTimeUs = 0
        format = MediaFormat.createVideoFormat("video/avc", width, height)
        format.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )

        format.setInteger(MediaFormat.KEY_BIT_RATE, KEY_BIT_RATE)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, KEY_FRAME_RATE)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, KEY_I_FRAME_INTERVAL)

        encoder = MediaCodec.createByCodecName(encoderType)

        encoder!!.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        vidSurface = encoder!!.createInputSurface()
        try {
            videoFilePath = File(
                filesDirPath, "symVid" + LocalDateTime.now().toEpochSecond(
                    ZoneOffset.of("Z")
                ).toString() + ".MP4"
            ).toString()

            mMuxer = MediaMuxer(videoFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            encoder!!.start()

            encoderStarted = true

            initialising = false
        }catch (xx:Exception){
            Log.d("initEncoder", "exception ${xx}")

          //  encoder.reset()
        }

        return
    }

    fun collectEncodeBitmaps(bitmapFlow: Flow<Pair<Bitmap?, IconDef>?>) {
        CoroutineScope(Dispatchers.IO).launch {
            bitmapFlow.collect {
                if(!encoderStarted){
                    if(!initialising) {
                        initEncoder()
                    }
                }else if(saveFile){
                    //saving file
                    stopAndSave()
                    saveFile = false
                    //  initEncoder() // start up again
                }
                else if (it != null) {
                    if (encoder != null && mMuxer != null && it.first != null && vidSurface != null) {
                        val canv = vidSurface!!.lockCanvas(Rect(0, 0, width, height))
                        canv?.drawBitmap(
                            it.first!!,
                            Rect(0, 0, width, height),
                            Rect(0, 0, width, height),
                            paint
                        )
                        vidSurface!!.unlockCanvasAndPost(canv)

                        val bufferInfo = MediaCodec.BufferInfo()
                        var outBufferId = encoder!!.dequeueOutputBuffer(bufferInfo, 10)

                        while (outBufferId >= 0) {
                            val encodedBuffer = encoder!!.getOutputBuffer(outBufferId)

                            encodedBuffer?.also {
                                if (!muxerStarted) {

                                    format = encoder!!.outputFormat
                                    trackIndex = mMuxer!!.addTrack(format)
                                    mMuxer!!.start()
                                    muxerStarted = true
                                }

                                // muxer ignoring framerate as set. Explicitly set frame timing
                                bufferInfo.presentationTimeUs = presentationTimeUs
                                mMuxer!!.writeSampleData(trackIndex, encodedBuffer, bufferInfo)

                                presentationTimeUs += 1000000/KEY_FRAME_RATE

                                Log.d("collectEncodeBitmaps", "muxer is writing buffer")

                            }
                            encoder!!.releaseOutputBuffer(outBufferId, false)

                            outBufferId = encoder!!.dequeueOutputBuffer(bufferInfo, 10)
                        }

                    }
                }
            }
        }
    }

    fun initiateStopAndSave(){
        saveFile = true
        stopAndSave()

    }

    private fun stopAndSave() {
        Log.d("stopAndSave","no more bitmap files")
        try {
            encoder?.signalEndOfInputStream()
            encoder?.flush()
        } catch (xx: java.lang.Exception) {
            Log.d("stopAndSave", "Exception encoder signalEndofInput: $xx")
           // encoder?.reset()
        }
        encoder?.stop()
        encoderStarted = false

        if ( mMuxer != null) {
            try {
                mMuxer!!.stop()
            }catch (xx: java.lang.Exception) {
                Log.d("stopAndSave", "Exception encoder mMuxer stop: $xx")
            }
            muxerStarted = false
            mMuxer
            mMuxer!!.release()
        }
        encoder?.release()

        if(File(videoFilePath).exists()){
            vidFileList.add(videoFilePath)
            Log.d("Encoder stopAndSave", "video file added $videoFilePath")
        }

        encoder = null
        mMuxer = null

    }


}