package com.example.sdkinstatest.camera_api

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class viewModel @Inject constructor(val repository: Repository):ViewModel() {

     fun fetchingData() {viewModelScope.launch{
        val exData=ExcuteComandsBody(
            "camera.takePicture",
            Parameters(
                "12ABC3",
                Options(200,-2)
            )
        )
        repository.getResponse(exData)
            .let { response ->
                if (response.isSuccessful){
                    response.body().let {
                        if (it != null)
                            Log.d("??????","bodyRespo :: ${it.name}")
                    }
                }
            }
     }
    }
}