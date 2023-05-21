package com.example.bestapp

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class DataStoreManager(context: Context) {
	private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "USER_PREFERENCES")
	private val dataStore: DataStore<Preferences> = context.dataStore

	companion object{
		val song = stringPreferencesKey("counter")
		//val songs = arrayListOf(song to "")
	}

	suspend fun setSongs(songsUri: ArrayList<String>) {
		dataStore.edit { pref ->
			pref[song] = songsUri[0]
		}
	}

	fun getSongs(): Flow<String> {
		return dataStore.data
			.catch {exception ->
				if (exception is IOException) {
					emit(emptyPreferences())
				}
				else {
					throw exception
				}
			}
			.map { pref ->
				val uiMode = pref[song] ?: ""
					uiMode
			}
	}
}