package com.akhilasdeveloper.marsroverphotos.utilities

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(value = Parameterized::class)
class ExtensionFunctionsKtParameterizedTestSimplify(val input:Int, val expectedValue: String) {

    @Test
    fun test(){
        val result = input.simplify()
        assertEquals(expectedValue, result)
    }

    companion object{

        @JvmStatic
        @Parameterized.Parameters(name = "{index} : {0} is {1}")
        fun data(): List<Array<Any>>{
            return listOf(
                arrayOf(12323,"12K"),
                arrayOf(123233,"123K"),
                arrayOf(5677778,"5M"),
                arrayOf(56777738,"56M")
            )
        }
    }
}