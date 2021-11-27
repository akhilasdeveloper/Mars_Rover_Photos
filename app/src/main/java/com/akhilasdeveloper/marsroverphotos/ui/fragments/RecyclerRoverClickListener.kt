package com.akhilasdeveloper.marsroverphotos.ui.fragments

import com.akhilasdeveloper.marsroverphotos.data.RoverMaster

interface RecyclerRoverClickListener {
    fun onItemSelected(master: RoverMaster, position: Int)
    fun onReadMoreSelected(master: RoverMaster, position: Int)
}