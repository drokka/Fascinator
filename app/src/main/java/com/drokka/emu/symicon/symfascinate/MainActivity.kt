package com.drokka.emu.symicon.symfascinate

import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.Executor


class MainActivity : AppCompatActivity() {

    companion object {

        /* this is used to load the  library on application
         * startup.
         */
        init {
            System.loadLibrary("emutil")
        }

        val nativeWrap: NativeWrap = NativeWrap()
    }

  //  lateinit var workManager: WorkManager

    lateinit var executor: Executor

    lateinit var imageView:SurfaceView

   // private val scope = CoroutineScope(ui)
    lateinit var viewModel:MainViewModel
  /*  lateinit var encoder: MediaCodec
    lateinit var format: MediaFormat

    lateinit var vidSurface:Surface
    lateinit var mMuxer:MediaMuxer
    var  trackIndex:Int = 0
    lateinit var saveFile:String

   */
    lateinit var bmFlow: Flow<Bitmap? >
    lateinit var indexFlow:Flow<Int>

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewModel = MainViewModel() //ViewModelProvider(this)[MainViewModel::class.java]
        //viewModel.filesDir = applicationContext.filesDir
        imageView = findViewById(R.id.imageView)
      //  workManager = WorkManager.getInstance(applicationContext)

        imageView.setOnClickListener {
            //saveAndRestartRec()
           // viewModel.fireOffEncodingJob(applicationContext)
            viewModel.myEncoder.saveFile = true // set off save and reinitialise encoder/muxer
            viewModel.reset(applicationContext)
         //   viewModel.checkJobs(applicationContext)
        }
      //  imageView.holder.addCallback(viewModel.onImageViewSurfaceDestroyed)

        bmFlow = viewModel.startFlow(this)
        viewModel.collectBitmaps(applicationContext, bmFlow)
        viewModel.myEncoder.filesDirPath = applicationContext.filesDir
        viewModel.myEncoder.collectEncodeBitmaps(bmFlow)

        indexFlow = viewModel.startBufferWatchFlow()
         viewModel.collectBitmapWatch(imageView, indexFlow)

      /*  lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // As collect is a suspend function, if you want to collect
                // multiple flows in parallel, you need to do so in
                // different coroutines
                launch {
                    bmFlow.collect {
                        if (!viewModel.resetting.get()) {
                            viewModel.bitmapArrayList.add(it)
                        }
                    }

                    launch {
                        indexFlow.collect {
                            if (it >= 0 && !viewModel.resetting.get()) {
                                try {
                                    viewModel.makeVid(imageView, it)
                                } catch (xx: java.lang.Exception) {
                                    Log.d("collectBitmapWatch", "exception " + xx.message)
                                }
                            }
                        }
                    }
                }
            }

        }

       */

/*

        format = MediaFormat.createVideoFormat("video/avc", 320, 240 ) //viewModel.sz, viewModel.sz )

        format.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )

        format.setInteger(MediaFormat.KEY_BIT_RATE, 125000) //viewModel.sz * viewModel.sz)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 15)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5)

            val encoderType = MediaCodecList(MediaCodecList.ALL_CODECS).findEncoderForFormat(format)
        encoder = MediaCodec.createByCodecName(encoderType)
        //encoder = MediaCodec.createEncoderByType("video/avc")

        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        vidSurface = encoder.createInputSurface()
        encoder.start()

 */
        // muxerInit()

       // callGenerate()
    }

    private fun muxerInit(){
        viewModel.muxerInit()
     //   saveFile = File(applicationContext.filesDir,"vid"+ LocalDateTime.now().toEpochSecond(
       //     ZoneOffset.of("Z")).toString() +".mp4").toString()
       // mMuxer = MediaMuxer(saveFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4 )

//        format = encoder.outputFormat
      //  trackIndex =mMuxer.addTrack(format)
    }



    /*
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveAndRestartRec() {

        encoder.signalEndOfInputStream()

        encoder.flush()
        encoder.stop()
       // encoder.release()

        if(viewModel.muxerStarted) {
            mMuxer.stop()
            mMuxer.release()
        }
        viewModel.muxerStarted = false

       // muxerInit()
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        vidSurface = encoder.createInputSurface()
        encoder.start()


    }

     */

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

    }

    override fun onPause() {
      //  viewModel.saveAndRestartRec()
        viewModel.paused = true
        viewModel.myEncoder.saveFile =true //set to save MP4 file

        Log.d("onPause", "called from MainActivity")

        super.onPause()
    }

    override fun onResume() {
      //  viewModel.startEncoding()
        Log.d("onResume", "called from MainActivity")

        viewModel.paused = false
        super.onResume()

    }

    override fun onStop() {
        viewModel.myEncoder.saveFile =true //set to save MP4 file

      //  viewModel.fireOffEncodingJob(applicationContext)
        Log.d("onStop", "called from MainActivity")
      //  viewModel.checkJobs(applicationContext)
        super.onStop()
    }
    override fun releaseInstance(): Boolean {
        viewModel.myEncoder.saveFile =true //set to save MP4 file

        // viewModel.stopAndSave()
      //  viewModel.fireOffEncodingJob(applicationContext)
        Log.d("onStop", "called fireOffEncodingJob")
       // viewModel.checkJobs(applicationContext)

        return super.releaseInstance()
    }
    override fun onDetachedFromWindow() {
     //   viewModel.myEncoder.saveFile = true
        Log.d("onDetachFromWindow", "saveFile set true")
      //  viewModel.stopAndSave()

     //   viewModel.fireOffEncodingJob(applicationContext)
        super.onDetachedFromWindow()


     //   encoder.signalEndOfInputStream()

       // encoder.flush()
      //  encoder.stop()

      //  mMuxer.stop()


     //   encoder.release()
       // mMuxer.release()
      //  mMuxer.
    }


}