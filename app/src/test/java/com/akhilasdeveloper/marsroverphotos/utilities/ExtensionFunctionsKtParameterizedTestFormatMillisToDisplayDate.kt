package com.akhilasdeveloper.marsroverphotos.utilities

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(value = Parameterized::class)
class ExtensionFunctionsKtParameterizedTestFormatMillisToDisplayDate(val input:Long, val expectedValue: String) {

    @Test
    fun test(){
        val result = input.formatMillisToDisplayDate()
        assertEquals(expectedValue, result)
    }

    companion object{

        @JvmStatic
        @Parameterized.Parameters(name = "{index} : {0} is {1}")
        fun data(): List<Array<Any>>{
            return listOf(
                arrayOf(1682063784841L,"21 April 2023"),
                arrayOf(1582063004000L,"19 February 2020"),
                arrayOf(1282063004000L,"17 August 2010")
            )
        }
    }
}