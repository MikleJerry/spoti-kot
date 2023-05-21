package com.example.bestapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.ArrayList

class MainViewModel(application: Application): AndroidViewModel(application) {
	private val dataStore = DataStoreManager(application)

	val getSongs = dataStore.getSongs().asLiveData(Dispatchers.IO)

	fun setSongs(songUri: ArrayList<String>) {
		viewModelScope.launch {
			dataStore.setSongs(songUri)
		}
	}
}