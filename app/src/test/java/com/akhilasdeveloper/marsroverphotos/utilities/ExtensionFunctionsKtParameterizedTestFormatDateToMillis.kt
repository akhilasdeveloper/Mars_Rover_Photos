package com.akhilasdeveloper.marsroverphotos.utilities

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(value = Parameterized::class)
class ExtensionFunctionsKtParameterizedTestFormatDateToMillis(val input:String, val expectedValue: Long) {

    @Test
    fun test(){
        val result = input.formatDateToMillis()
        assertEquals(expectedValue, result)
    }

    companion object{

        @JvmStatic
        @Parameterized.Parameters(name = "{index} : {0} is {1}")
        fun data(): List<Array<Any>>{
            return listOf(
                arrayOf("2023-04-21",1682015400000L),
                arrayOf("2020-02-19", 1582050600000L),
                arrayOf("2010-08-17", 1281983400000L)
            )
        }
    }
}