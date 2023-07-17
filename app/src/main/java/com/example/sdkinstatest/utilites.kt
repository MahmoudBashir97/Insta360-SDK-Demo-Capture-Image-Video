package com.example.sdkinstatest

import android.content.Context
import android.widget.Toast

class  utilites {
    companion object{
        fun showToastMsg(msg:String,context:Context){
            Toast.makeText(context,msg,Toast.LENGTH_LONG).show()
        }
    }
}