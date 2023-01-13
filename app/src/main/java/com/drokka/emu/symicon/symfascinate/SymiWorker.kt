package com.drokka.emu.symicon.symfascinate

import android.app.Activity
import androidx.work.WorkerParameters

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import java.io.File
import java.io.FileOutputStream

const val INT_ARGS = "intArgs"

const val ICON_TYPE = "iconType"

const val D_ARGS = "dArgs"
const val BITMAP_BYTES = "BITMAP_BYTES"
const val BITMAP_WIDTH = "BITMAP_WIDTH"
const val BITMAP_HEIGHT = "BITMAP_HEIGHT"
const val BITMAP_CONFIG_NAME = "BITMAP_CONFIG_NAME"

const val BITMAP_NAME = "BITMAP_NAME"
const val BITMAP_LEN ="BITMAP_LEN"

private const val RUN_SYMI_EXAMPLE = "runSymiExample"

class SymiWorker( context: Context, params:WorkerParameters)
    : CoroutineWorker(context, params){



    override suspend fun doWork(): Result {

        val intArgs:IntArray? = inputData.getIntArray(INT_ARGS)
        intArgs?.let {
            val iconImageType: Byte = inputData.getByte(ICON_TYPE, 'S'.toByte())
            val dArgs = inputData.getDoubleArray(D_ARGS)

            val clrFunction = inputData.getString("clrFunction")?:"default"
            val clrFunExp = inputData.getDouble("clrFunExp", 0.0)
            //      var job: Deferred<OutputData>
            dArgs?.let {
                //           withContext(Dispatchers.IO) {
                //              job = coroutineScope {
                //               async {
                val   generatedData:OutputData =    callRunSampleFromJNI(intArgs, iconImageType, dArgs, clrFunction, clrFunExp)
                Log.d("SymiWorker", "on thread::  " + Thread.currentThread().id.toString())

                //              }
                //        }
                //  }
                //  setForeground(createForegroundInfo("Started big job"))
                //   job.await()
                // val generatedData = job.getCompleted()

                if (generatedData?.bitmap != null) {

                        val bitmap = generatedData?.bitmap!!
                        val bitmapName = "bitmap$id"
                    try {

                        val imFile = File(applicationContext.cacheDir, bitmapName)
                        val pngStream = FileOutputStream(imFile)
                        val image = bitmap   //BitmapFactory.decodeByteArray(generatedData.pngBuffer,
                        //  0, generatedData.pngBufferLen)
                        image?.compress(
                            Bitmap.CompressFormat.PNG,
                            100,
                            pngStream
                        )
                        pngStream.flush()
                        pngStream.close()
                    } catch (xx: Exception) {
                        xx.message?.let { Log.e("storeWork", it) }
                        Log.e("storeWork", "exception thrown saving png " + bitmapName)
                    }


                        val returnData = Data.Builder().putString(BITMAP_NAME, bitmapName)
                            .putInt(BITMAP_LEN, bitmap.width * bitmap.height)

                        return Result.success(returnData.build()) // outData)
                    }
                    else {
                        Log.e(
                            RUN_SYMI_EXAMPLE,
                            "output data is error "
                        )
                        //   val errData = Data.Builder().putString(SAVED_DATA, generatedData.savedData)
                        //     .build()
                        return Result.failure()   // errData)
                    }
                }
                Log.e(RUN_SYMI_EXAMPLE, "generatedData?.savedData?.isNotEmpty() FALSE. Got nothing back on callRunSampleFromJNI.")
                return Result.failure()
            }


        if(this.isStopped){
            Log.d(RUN_SYMI_EXAMPLE, "worker is stopped, number of attempts: " + this.runAttemptCount)
        }
        return Result.failure()
    }

}