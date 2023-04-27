package com.akhilasdeveloper.marsroverphotos.utilities

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(value = Parameterized::class)
class ExtensionFunctionsKtParameterizedTestFormatMillisToFileDate(val input:Long, val expectedValue: String) {

    @Test
    fun test(){
        val result = input.formatMillisToFileDate()
        assertEquals(expectedValue, result)
    }

    companion object{

        @JvmStatic
        @Parameterized.Parameters(name = "{index} : {0} is {1}")
        fun data(): List<Array<Any>>{
            return listOf(
                arrayOf(1682063784841L,"2023_04_21"),
                arrayOf(1582063004000L,"2020_02_19"),
                arrayOf(1282063004000L,"2010_08_17")
            )
        }
    }
}