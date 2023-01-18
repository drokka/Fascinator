package com.drokka.emu.symicon.symfascinate

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.absoluteValue


@SuppressLint("StaticFieldLeak")
class MainViewModel : ViewModel(), SurfaceHolder.Callback {


    val onImageViewSurfaceDestroyed: SurfaceHolder.Callback? = this
    var paused: Boolean =false
    var iconDef: IconDef = IconDef()
    val sz = 480
    val startIters = 5000
    var iters = startIters


    var bgClr = doubleArrayOf(0.0, 0.08, 0.2, 1.0)
    var bgClrF = floatArrayOf(0.0f, 0.08f, 0.2f, 1.0f)
    var minClr = doubleArrayOf(0.5, 0.0, 0.1, 1.0)
    var maxClr = doubleArrayOf(0.99, 0.8, 0.9, 1.0)
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
    var cacheIndex = 0

    val paint = Paint()

    var alphaDir = 1.0
    var gammaDir = 1.0
    var lambdaDir = 1.0
    var maDir = -1.0
    var omegaDir = -1.0
    var betaDir = -1.0
    val jump = 0.001

    var resetting = AtomicBoolean(false)


        val jobList :ArrayList<UUID> = ArrayList(0)

    fun fireOffEncodingJob(context: Context){

            Log.d("fireOffEncodingJob", "Start creating work request")
        for(ci in cacheIndex..cacheIndex) {
            val cacheDir = File(context.cacheDir, "$ci").absolutePath
            val filesDir = context.filesDir.absolutePath
            val inputData = Data.Builder().putString("cacheDir", cacheDir)
                .putString("filesDir", filesDir)
                .build()
            val encodeWorkRequest: OneTimeWorkRequest =
                OneTimeWorkRequestBuilder<EncodeVideoWorker>()
                    .setInputData(inputData)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "encode_$ci", ExistingWorkPolicy.KEEP,
                encodeWorkRequest
            )

            jobList.add(encodeWorkRequest.id)
        }

    }

    fun checkJobs(context: Context) {
        for (id in jobList) {
            val workInfo = WorkManager
                .getInstance(context).getWorkInfoById(id)
            Log.d("bitmapFlow", "work info for encodeWorkRequest: $workInfo")

            try {
                if (workInfo.get().state == WorkInfo.State.SUCCEEDED) {

                    Log.d("checkJobs", "success for work: " + id + " " + workInfo.toString())

                }
            }catch (_:Exception){}
        }
    }

    fun reset(applicationContext: Context) {
        // iconDef = IconDef()
     //   fireOffEncodingJob(applicationContext)
        resetting.set(true)

        bitmapArrayList.clear()

        //saveAndRestartRec()



        iconDef.shakeUp()
        iters = startIters
        bitmapArrayList.clear()
        currentIndex = 0
        reversing = false
        sameAsCount = 0
        count = 0
        bump = 0
        cacheIndex +=1

        resetting.set(false)
      //  checkJobs(applicationContext)
    }

    val myEncoder = Encoder(sz)

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

                    reset(context)

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

    fun collectBitmaps(context: Context,bitmapFlow: Flow<Bitmap?>) {
        viewModelScope.launch(Dispatchers.Default) {
            bitmapFlow.collect {
                if (!resetting.get()) {
                    bitmapArrayList.add(it)
                   // storeBitmap(context, it)  //Save once to disk so can be processed (encoded to video)
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

    private fun storeBitmap(context: Context, it: Bitmap?) {
        //Store in cacheIndex directory, appending count to filename.
        try{
            val imagesDirPath = File(context.cacheDir, "$cacheIndex")
          //  Log.i("saveImage", "dirPath is:" + imagesDirPath.toString())
            imagesDirPath.mkdirs()
            val imFile = File(imagesDirPath, "bitmap_$count.png")
            val pngStream = FileOutputStream(imFile)
            //   image = BitmapFactory.decodeByteArray(outputData.pngBuffer, 0, outputData.pngBufferLen) // generatedImage.getBitmap()
            it?.compress(
                Bitmap.CompressFormat.PNG,
                100,
                pngStream
            )
            pngStream.flush()
            pngStream.close()
            Log.i("storeBitmap", "dirPath is:$imagesDirPath and file is $imFile")

        } catch (xx: Exception) {
            xx.message?.let { Log.e("saveImage", it) }

        }
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

        }
    }

    override fun onCleared() {

        //  stopAndSave()

        super.onCleared()

    }

    fun stopAndSave() {


    }

    fun startEncoding() {

    }

    fun saveAndRestartRec() {

    }

    fun muxerInit() {

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
//Log.d("SurfaceCreated", "imageView created")
    }

    override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
    }

    override fun surfaceDestroyed(p0: SurfaceHolder) {
     //       myEncoder.saveFile = true
       // Log.d("surfaceDestroyed", "imageView destroyed stopAndSave called")

    }

}