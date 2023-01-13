package com.drokka.emu.symicon.symfascinate

import android.util.Log
import java.util.Date
import kotlin.random.Random

class IconDef(
    var lambda: Double = 0.4 ,//(Random.nextDouble()- 0.5)*2.81,
    var alpha: Double = -0.1, //(Random.nextDouble()- 0.25)*15.999,
    var beta: Double = -0.35, //(Random.nextDouble() - 0.9)*19.999,
    var gamma: Double = 0.4, //(Random.nextDouble()- 0.5)*2.1,
    var omega: Double = 0.01, //(Random.nextDouble() - 0.5)*0.5,
    var ma: Double = 0.02 , //Random.nextDouble()*0.02, // - 0.5)*1.999,
    var quiltType: QuiltType = QuiltType.FRACTAL,
    var degreeSym:Int =5
) {

    fun shakeUp() {
        quiltType = randomType()

        lambda = when(quiltType) {
            QuiltType.SQUARE_ICON -> (Random.nextDouble() - 0.65)
            QuiltType.SQUARE -> (Random.nextDouble() - 0.65)
            QuiltType.HEX -> (Random.nextDouble() - 0.5)* 00.4
            QuiltType.FRACTAL -> (Random.nextDouble() - 0.5)
        }
                    alpha =  when(quiltType) {
                        QuiltType.SQUARE_ICON -> (Random.nextDouble() - 0.5)*0.6
                        QuiltType.SQUARE -> (Random.nextDouble() - 0.5)*0.6
                        QuiltType.HEX -> (Random.nextDouble() - 0.5)* 0.2
                        QuiltType.FRACTAL -> (Random.nextDouble() - 0.3)*1.1
                    }
                    beta = when(quiltType) {
                        QuiltType.SQUARE_ICON -> (Random.nextDouble()) *0.2
                        QuiltType.SQUARE -> (Random.nextDouble()  *0.2)
                        QuiltType.HEX -> (Random.nextDouble() - 0.7)
                        QuiltType.FRACTAL -> (Random.nextDouble() - 0.4)
                    }
                    gamma= when(quiltType) {
                        QuiltType.SQUARE_ICON -> (Random.nextDouble() - 0.5)* 00.8
                        QuiltType.SQUARE -> (Random.nextDouble() - 0.5)* 00.8
                        QuiltType.HEX -> (Random.nextDouble() - 0.5)* 00.2
                        QuiltType.FRACTAL -> (Random.nextDouble() - 0.4)
                    }
                    omega= when(quiltType) {
                        QuiltType.SQUARE_ICON -> (Random.nextDouble()) *0.1
                        QuiltType.SQUARE -> (Random.nextDouble()) *0.1
                        QuiltType.HEX -> (Random.nextDouble() )* 00.1
                        QuiltType.FRACTAL -> (Random.nextDouble() ) *0.5
                    }
                    ma= when(quiltType) {
                        QuiltType.SQUARE_ICON -> (Random.nextInt(0,2).toDouble())*.1
                        QuiltType.SQUARE -> (Random.nextInt(0,2).toDouble())*0.1
                        QuiltType.HEX -> when(Random.nextInt(0,1) ){
                            0 -> 0
                            1 -> 0.1
                          //  2 -> 1.366
                            else -> {0}
                        }.toDouble()


                        QuiltType.FRACTAL -> (Random.nextDouble())*0.5
                    }
                    degreeSym = kotlin.random.Random.nextInt(3,6)

                Log.d("shakeUp", "Icon is ${quiltType} ma is $ma and degreeSym is $degreeSym")
        }


    fun randomType(): QuiltType{
        var quiltType = QuiltType.FRACTAL
        val i =    kotlin.random.Random.nextInt() % 4
        when (i ){
            0 -> quiltType =  QuiltType.SQUARE_ICON
            1 -> quiltType =  QuiltType.SQUARE
            2 -> quiltType =  QuiltType.HEX
            3 -> quiltType =  QuiltType.FRACTAL
        }
        return  quiltType
    }

}