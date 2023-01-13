package com.drokka.emu.symicon.symfascinate

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.work.WorkInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.util.UUID
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue
import kotlinx.coroutines.flow.transform
import java.lang.Thread.sleep
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicBoolean


@SuppressLint("StaticFieldLeak")
class MainViewModel(var filesDir: File) : ViewModel(), SurfaceHolder.Callback {


    val onImageViewSurfaceDestroyed: SurfaceHolder.Callback? = this
    var paused: Boolean =false
    var iconDef: IconDef = IconDef()
    val sz = 480
    val startIters = 5000
    var iters = startIters


    var bgClr = doubleArrayOf(0.0, 0.08, 0.2, 1.0)
    var bgClrF = floatArrayOf(0.0f, 0.08f, 0.2f, 1.0f)
    var minClr = doubleArrayOf(0.5, 0.0, 0.1, 1.0)
    var maxClr = doubleArrayOf(0.99, 0.6, 0.7, 1.0)
    var clrFunction: String = "default"

    var clrFunctionExp: Double = 0.270000000107

    val bitmapArrayList = mutableListOf<Bitmap?>()
    val BUFFER_SIZE = 48
    val iterBump = 16
    val DELAY_SIZE = 3
    var currentIndex = 0
    var reversing = false
    var sameAsCount = 0
    var count = 0
    var bump = 0

    val paint = Paint()

    var alphaDir = 1.0
    var gammaDir = 1.0
    var lambdaDir = 1.0
    var maDir = -1.0
    var omegaDir = -1.0
    var betaDir = -1.0
    val jump = 0.01

    var resetting = AtomicBoolean(false)
    var muxerStarted = false
    var trackIndex = -1

    lateinit var encoder: MediaCodec
    var encoderStarted = false
    lateinit var format: MediaFormat

    lateinit var vidSurface: Surface
    lateinit var mMuxer: MediaMuxer
    val encoderType: String

    val KEY_BIT_RATE = 500000
    val KEY_FRAME_RATE = 20
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

        encoderType = MediaCodecList(MediaCodecList.ALL_CODECS).findEncoderForFormat(format)
        encoder = MediaCodec.createByCodecName(encoderType)
        //encoder = MediaCodec.createEncoderByType("video/avc")

        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        vidSurface = encoder.createInputSurface()
        encoder.start()
        encoderStarted = true

    }

    fun reset() {
        // iconDef = IconDef()
        resetting.set(true)

        bitmapArrayList.clear()

        saveAndRestartRec()

        iconDef.shakeUp()
        iters = startIters
        bitmapArrayList.clear()
        currentIndex = 0
        reversing = false
        sameAsCount = 0
        count = 0
        bump = 0

        resetting.set(false)
    }

    fun startFlow(context: Context): Flow<Bitmap?> {

        val bitmapFlow = flow {
            while (true ) {
                val latestBitmap = callGenerateF(context)
                //       if (latestBitmap != null) {
                emit(latestBitmap)
                count++
                if (bump == 0 && count >= BUFFER_SIZE) {
                    iters *= 2
                    bump++
                } else if (bump == 1 && count >= 2 * BUFFER_SIZE) {
                    iters *= 2
                    bump++
                } else if (bump == 2 && count >= (3 * BUFFER_SIZE )) {
                    bump++
                    iters *= 2
                } else if (bump == 3 && count >= (3 * BUFFER_SIZE + 2 * iterBump)) {
                    bump++
                    iters *= 2
                } else if (bump == 4 && count >= (3 * BUFFER_SIZE + 3 * iterBump)) {
                    bump++
                    iters *= 2
                } else if (bump == 5 && count >= (3 * BUFFER_SIZE + 4 * iterBump)) {
                    bump++
                    iters *= 2
                } else if (bump == 6 && count >= (3 * BUFFER_SIZE + 5 * iterBump)) {
                    bump++
                    iters *= 2
                }else if (bump == 7 && count >= (3 * BUFFER_SIZE + 5 * iterBump)) {
                    bump++
                    iters *= 2
                }



//Log.d("bitmapFlow", "count is $count and iters is $iters")
                //   } else{
                // reset()
                //  }
                // Emits the result of the request to the flow
                delay(100) // Suspends the coroutine for some time
                if (latestBitmap == null && !resetting.get()) {
                    reset()

                } else {
                    moveParam()
                }

            }
        }
            .transform { value ->
                if (resetting.get() ) {
                    emit(null)
                } else {
                    emit(value)
                }
                while(paused){ delay(100)}

            }
        return bitmapFlow
    }

    fun collectBitmaps(imageView: SurfaceView, bitmapFlow: Flow<Bitmap?>) {
        viewModelScope.launch(Dispatchers.Default) {
            bitmapFlow.collect {
                if (!resetting.get()) {
                    bitmapArrayList.add(it)
                    storeBitmap(it)  //Save once to disk so can be processed (encoded to video)
                }
                /*   if(bitmapArrayList.isNotEmpty() && bitmapArrayList.last().sameAs(it)){
                       sameAsCount+=1
                       if(sameAsCount >5){
                           reset()
                       }
                   }

                 */

                // imageView.invalidate()
                //  if(bitmapArrayList.size > DELAY_SIZE){
                //     makeVid(imageView)
                // }
            }
        }
    }

    private fun storeBitmap(it: Bitmap?) {

    }

    fun startBufferWatchFlow(): Flow<Int> {
        val nextIndexFlow = flow {
            while (true) {
                if (reversing) {
                    if (currentIndex < 2) {
                        reversing = false
                    }
                    currentIndex -= 1
                } else if (currentIndex < bitmapArrayList.size - DELAY_SIZE) {
                    if (bitmapArrayList.size > BUFFER_SIZE) {
                        bitmapArrayList.removeAt(0) //prune first

                        currentIndex -= 1 //adjust for first element removed
                        Log.d(
                            "bufferwatch flow",
                            "after pruning size is ${bitmapArrayList.size} and currentIndex is $currentIndex"
                        )
                    }
                    currentIndex += 1
                } else if (currentIndex > 1) {  //not reversing and close to array end
                    reversing = true
                    currentIndex -= 1  // go backwards
                }

                emit(currentIndex)
                Log.d("bufferwatch flow", "emitted $currentIndex")
                delay(100)

            }
        }.transform { value ->
            if (resetting.get()) {
            } else {
                emit(value)
            }
            while(paused){ delay(100)}

        }

        return nextIndexFlow
    }

    fun collectBitmapWatch(imageView: SurfaceView, nextIndexFlow: Flow<Int>) {
        viewModelScope.launch(Dispatchers.Default) {
            nextIndexFlow.collect {
                if (it >= 0 && !resetting.get()) {
                    try {
                        makeVid(imageView, it)
                    } catch (xx: java.lang.Exception) {
Log.d("collectBitmapWatch", "exception " + xx.message)
                    }
                }

                // imageView.invalidate()
                //  if(bitmapArrayList.size > DELAY_SIZE){
                //     makeVid(imageView)
                // }
            }
        }
    }

    fun makeVid(imageView: SurfaceView, i: Int) {
        // AnimationSet()
        //imageView.transitionName
       //  if (!this.vidSurface.isValid) return //do nothing
        if(!imageView.isAttachedToWindow) {
           // stopAndSave()
            return
        }
        if (bitmapArrayList.size > i && bitmapArrayList[i] != null) {
            val canvas = imageView.holder.lockCanvas()

            canvas?.drawColor(Color.argb(bgClrF[3], bgClrF[0], bgClrF[1], bgClrF[2]))
            canvas?.drawBitmap(
                bitmapArrayList[i]!!,
                Rect(0, 0, sz, sz),
                Rect(10, 10, 10 + sz, 10 + sz),
                paint
            )

            if (imageView.holder?.surface?.isValid == true) {
                imageView.holder.unlockCanvasAndPost(canvas)
            } else {
               //  stopAndSave()
                return
            }
            // media encoder callback here?
            if(!encoderStarted){
                startEncoding()
            }

            val canv = vidSurface?.lockCanvas(Rect(0, 0, sz, sz))
            canv?.drawBitmap(bitmapArrayList[i]!!, Rect(0, 0, sz, sz), Rect(0, 0, sz, sz), paint)
            vidSurface?.unlockCanvasAndPost(canv)

            var bufferInfo = MediaCodec.BufferInfo()
            var outBufferId = encoder.dequeueOutputBuffer(bufferInfo, 100)

            while (outBufferId >= 0) {
                val encodedBuffer = encoder.getOutputBuffer(outBufferId)

                // MediaMuxer is ignoring KEY_FRAMERATE, so I set it manually here
                // to achieve the desired frame rate
                //   bufferInfo.presentationTimeUs = presentationTimeUs

                encodedBuffer?.also {
                    if (!muxerStarted) {

                        val mformat = encoder.outputFormat
                        trackIndex = mMuxer.addTrack(mformat)
                        mMuxer.start()
                        muxerStarted = true
                    }

                    mMuxer.writeSampleData(trackIndex, encodedBuffer, bufferInfo)
                    Log.d("makeVid", "muxer is writing buffer")
                }
                //  presentationTimeUs += 1000000/frameRate

                encoder.releaseOutputBuffer(outBufferId, false)

                outBufferId = encoder.dequeueOutputBuffer(bufferInfo, 100)
            }

        }
    }

    override fun onCleared() {

        //  stopAndSave()

        super.onCleared()

    }

    fun stopAndSave() {
        if (encoderStarted) {
            try {
                encoder.signalEndOfInputStream()
                encoder.flush()
            } catch (xx: java.lang.Exception) {
                Log.d("stopAndSave", "Exception encoder signalEndofInput: ${xx.message}")
                encoder.reset()
            }
            encoder.stop()
            encoderStarted = false
        }

        if (muxerStarted) {
            mMuxer.stop()
            muxerStarted = false
            mMuxer.release() // muxerInit initialises  mMuxer again
        }
        encoder.release()

    }

    fun startEncoding() {
        muxerInit()

        //  val encoderType = MediaCodecList(MediaCodecList.ALL_CODECS).findEncoderForFormat(format)
        encoder = MediaCodec.createByCodecName(encoderType)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
         vidSurface = encoder.createInputSurface()
        encoder.start()
        encoderStarted = true

    }

    fun saveAndRestartRec() {
        if (encoderStarted) {
            try {
                encoder.signalEndOfInputStream()
                encoder.flush()
            } catch (xx: java.lang.Exception) {
                Log.d("saveAndRestart", "Exception encoder signalEndofInput: ${xx.message}")
                encoder.reset()
            }

            encoder.stop()
            encoderStarted = false
            // encoder.release()
        }
        if (muxerStarted) {
            mMuxer.stop()
            mMuxer.release() // muxerInit initialises  mMuxer again
        }
        muxerStarted = false

        muxerInit()
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        vidSurface = encoder.createInputSurface()
        encoder.start()
        encoderStarted = true

    }

    fun muxerInit() {
        val saveFile = File(
            filesDir, "vid" + LocalDateTime.now().toEpochSecond(
                ZoneOffset.of("Z")
            ).toString() + ".mp4"
        ).toString()
        mMuxer = MediaMuxer(saveFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    }

    fun moveParam() {


        if ((iconDef.alpha * (1.0 + alphaDir * jump)).absoluteValue >= 0.9) {
            alphaDir *= -1

        }
        iconDef.alpha = iconDef.alpha * (1.0 + alphaDir * jump)

        if ((iconDef.gamma * (1.0 + gammaDir * jump)).absoluteValue >= 0.9) {
            gammaDir *= -1

        }
        iconDef.gamma = iconDef.gamma * (1.0 + gammaDir * jump)

        if ((iconDef.beta * (1.0 + betaDir * jump)).absoluteValue >= 1.0) {
            betaDir *= -1

        }
        iconDef.beta = iconDef.beta * (1.0 + betaDir * jump)

        if ((iconDef.omega * (1.0 + omegaDir * jump)).absoluteValue >= 1.0) {
            omegaDir *= -1

        }
        iconDef.omega = iconDef.omega * (1.0 + omegaDir * jump)

        if ((iconDef.lambda * (1.0 + lambdaDir * jump)).absoluteValue >= 1.0) {
            lambdaDir *= -1

        }
        iconDef.lambda = iconDef.lambda * (1.0 + lambdaDir * jump)

        if ((iconDef.ma * (1.0 + maDir * jump)).absoluteValue >= 0.29) {
            maDir *= -1

        }
        iconDef.ma = iconDef.ma * (1.0 + maDir * jump)


        Log.d(
            "moveParam",
            "count = $count bump = ${bump}iters is up to ${iters} aand param values are ${iconDef.gamma} and ${iconDef.lambda}"
        )
    }

    /*   fun callGenerate(){
           Thread.sleep(1000)
           var k=0
           while (k< 6){
               Thread.sleep(100)
               val id =   runGeneration()
               workManager.getWorkInfoByIdLiveData(id)
                   .observe(this) { workInfo ->
                       checkWI(workInfo, id)
                   }

               moveParam()
               k++
           }
       } */

    fun getActivity(context: Context): AppCompatActivity {
        if (context is AppCompatActivity) {
            return context
        } else {
            return (context as ContextWrapper).baseContext as AppCompatActivity
        }
    }

    fun callGenerateF(context: Context): Bitmap? {
        val bitmap = runGeneration(context)

        // Thread.sleep(100)


        //workManager.getWorkInfoByIdLiveData(id).observe(getActivity(context)) {
        // bitmap =  checkWI(context,  it, id)
        // Thread.sleep(1000)
        //   }


        return bitmap //checkWI(context,  workManager.getWorkInfoById(id).get(), id)
    }

    private fun checkWI(context: Context, workInfo: WorkInfo?, id: UUID): Bitmap? {


        if (workInfo?.state == WorkInfo.State.SUCCEEDED) {

            Log.i("Go Big checkWI", "success for work: " + id + " " + workInfo.toString())

            val iconImageFileName = workInfo.outputData.getString(BITMAP_NAME)
            val len = workInfo.outputData.getInt((BITMAP_LEN), 0)
            val cacheDir = context.cacheDir
            if (iconImageFileName != null) {
                try {

                    val imFile = File(cacheDir, iconImageFileName)

                    val inputStream = FileInputStream(imFile.path)

                    Log.d("GeneratedImage.getBitmap", "iconImageFileName is $iconImageFileName")
                    var byteArray = ByteArray(len)
                    inputStream.read(byteArray, 0, len)
                    var bitmapImage = BitmapFactory.decodeByteArray(byteArray, 0, len)

                    inputStream.close()

                    //   if(bitmapImage != null){
                    //      imageView.setImageBitmap(bitmapImage)
                    // }


                    imFile.delete()
                    return bitmapImage
                } catch (xx: Exception) {
                    Log.e("GeneratedImage.getBitmap", "ERROR msg is: " + xx.message)

                }
            }


        } else if (workInfo?.state == WorkInfo.State.ENQUEUED) {

            Log.d("Go Big checkWI", "queued")
        } else if (workInfo?.state == WorkInfo.State.RUNNING) {

            Log.d("Go Big checkWI", "running" + id + " ")
        } else if (workInfo?.state?.isFinished == true) {

            Log.e(
                "Go Big checkWI",
                "Error generating large image. workinfo state: " + id + " " + workInfo?.toString()
            )

        } /* else {
            // could be Failed, cancelled or blocked

            Log.e("Go Big checkWI", "Fail? fall through generating large image. workinfo: " +id +" "  + workInfo?.toString())

            workManager.cancelWorkById(id)
        } */

        return null
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun runGeneration(context: Context): Bitmap? {

        //var outputData: OutputData? = null
        //   val generateJob = CoroutineScope(Dispatchers.IO).async {
        Log.d(
            "runGeneration",
            "runSymiExample: icon TYPE ${iconDef.quiltType} symi.lambda " + iconDef.lambda.toString()
        )
        val outputDataJob = MainActivity.nativeWrap.runSample(
            context,
            iters,
            sz,
            sz,
            iconDef,
            bgClr,
            minClr,
            maxClr,
            clrFunction,
            clrFunctionExp
        )
        // outputDataJob.await()

        if (outputDataJob != null && outputDataJob!!.bitmap != null) {
            return outputDataJob!!.bitmap
        }
        // }
        return null
    }

    override fun surfaceCreated(p0: SurfaceHolder) {
Log.d("SurfaceCreated", "imageView created")
    }

    override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
    }

    override fun surfaceDestroyed(p0: SurfaceHolder) {
            stopAndSave()
        Log.d("surfaceDestroyed", "imageView destroyed stopAndSave called")

    }

}