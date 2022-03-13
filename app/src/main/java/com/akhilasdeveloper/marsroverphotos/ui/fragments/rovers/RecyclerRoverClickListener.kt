package com.akhilasdeveloper.marsroverphotos.ui.fragments.rovers

import com.akhilasdeveloper.marsroverphotos.data.RoverMaster

interface RecyclerRoverClickListener {
    fun onItemSelected(master: RoverMaster, position: Int)
    fun onItemSaveSelected(master: RoverMaster, position: Int)
    fun onReadMoreSelected(master: RoverMaster, position: Int)
    fun onAboutSelected()
}