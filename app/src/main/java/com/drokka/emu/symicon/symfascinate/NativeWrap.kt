package com.drokka.emu.symicon.symfascinate

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.UUID
import com.drokka.emu.symicon.symfascinate.OutputData

external fun callRunSampleFromJNI(
    intArgs: IntArray,
    type: Byte,
    dArgs: DoubleArray,
    clrFunction: String,
    clrFunExp: Double
): OutputData

class NativeWrap {

    fun runSample(context: Context, iterations:Int, width:Int, height:Int, iconDef: IconDef, bgClr: DoubleArray,
                          minClr: DoubleArray, maxClr: DoubleArray, clrFunction: String,
                          clrFunExp: Double): OutputData {
        var iconImageType: Byte
        val dArgs = DoubleArray(18)

        iconImageType = iconDef.quiltType.label[0].toByte()
        dArgs[0] = iconDef.lambda
        dArgs[1] = iconDef.alpha
        dArgs[2] = iconDef.beta
        dArgs[3] = iconDef.gamma
        dArgs[4] = iconDef.omega
        dArgs[5] = iconDef.ma

        dArgs[6] = bgClr[0]
        dArgs[7] = bgClr[1]
        dArgs[8] = bgClr[2]
        dArgs[9] = bgClr[3]

        dArgs[10] = minClr[0]
        dArgs[11] = minClr[1]
        dArgs[12] = minClr[2]
        dArgs[13] = minClr[3]

        dArgs[14] = maxClr[0]
        dArgs[15] = maxClr[1]
        dArgs[16] = maxClr[2]
        dArgs[17] = maxClr[3]

        val intArgs: IntArray = intArrayOf(
            iterations,
            width,
            height,
            iconDef.degreeSym
        )
        Log.d("RunSample", "type ${iconDef.quiltType} , on thread::  " + Thread.currentThread().id.toString())

      /*  val inputData = Data.Builder().putIntArray(INT_ARGS, intArgs)
            .putByte(ICON_TYPE, iconImageType)
            .putDoubleArray(D_ARGS, dArgs)
            .putString("clrFunction", clrFunction)
            .putDouble("clrFunExp", clrFunExp)
            .build()
        val symiWorkRequest: WorkRequest =
            OneTimeWorkRequestBuilder<SymiWorker>()
                .setInputData(inputData)
                .build()


        WorkManager
            .getInstance(context)
            .enqueue(symiWorkRequest)

       */

        return callRunSampleFromJNI(intArgs, iconImageType, dArgs, clrFunction, clrFunExp) //coroutineScope {
           // async {
            //    callRunSampleFromJNI(intArgs, iconImageType, dArgs, clrFunction, clrFunExp)
            //}
      //  }
       // return symiWorkRequest.id
    }
}