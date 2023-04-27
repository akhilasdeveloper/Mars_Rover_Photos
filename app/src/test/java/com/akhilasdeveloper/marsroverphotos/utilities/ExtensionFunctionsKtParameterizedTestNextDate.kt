package com.akhilasdeveloper.marsroverphotos.utilities

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(value = Parameterized::class)
class ExtensionFunctionsKtParameterizedTestNextDate(val input:Long, val expectedValue: Long) {

    @Test
    fun test(){
        val result = input.nextDate()
        assertEquals(expectedValue, result)
    }

    companion object{

        @JvmStatic
        @Parameterized.Parameters(name = "{index} : {0} is {1}")
        fun data(): List<Array<Any>>{
            return listOf(
                arrayOf(1682015400000L,1682101800000L),
                arrayOf(1582050600000L, 1582137000000L),
                arrayOf(1281983400000L, 1282069800000L)
            )
        }
    }
}