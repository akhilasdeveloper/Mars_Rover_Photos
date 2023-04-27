package com.akhilasdeveloper.marsroverphotos.utilities

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(value = Parameterized::class)
class ExtensionFunctionsKtParameterizedTestPrevDate(val input:Long, val expectedValue: Long) {

    @Test
    fun test(){
        val result = input.prevDate()
        assertEquals(expectedValue, result)
    }

    companion object{

        @JvmStatic
        @Parameterized.Parameters(name = "{index} : {0} is {1}")
        fun data(): List<Array<Any>>{
            return listOf(
                arrayOf(1682101800000L, 1682015400000L),
                arrayOf(1582137000000L, 1582050600000L),
                arrayOf(1282069800000L, 1281983400000L)
            )
        }
    }
}