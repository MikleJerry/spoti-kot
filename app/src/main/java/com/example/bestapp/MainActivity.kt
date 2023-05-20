package com.example.bestapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.OpenableColumns
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.bestapp.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
	private lateinit var binding: ActivityMainBinding
	lateinit var mediaPlayer: MediaPlayer
	val metaRetriever: MediaMetadataRetriever = MediaMetadataRetriever()
	var songsUri: ArrayList<Uri> = arrayListOf()
	var currentSongIndex: Int = 0
	var songTitle: String? = null
	var artist: String? = null
	lateinit var songBitmapImage: Bitmap
	lateinit var arrayAdapter: ArrayAdapter<*>
	var songsNames: ArrayList<String> = arrayListOf()

	// Инициализация launcher
	private val getSongLauncher =
		registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
			if (result.resultCode == Activity.RESULT_OK) {
				val uri: Uri = result.data!!.data!!

				// Добавляем URI песни в массив
				// и название песни в ListView
				songsUri.add(uri)
				songsNames.add(uri.getName(applicationContext))

				// Загружаем песню в массив
				arrayAdapter = ArrayAdapter(applicationContext, android.R.layout.simple_list_item_1, songsNames)
				binding.songsListView.adapter = arrayAdapter
			}
		}


	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityMainBinding.inflate(layoutInflater)
		setContentView(binding.root)

		// Заставляет текст двигаться по горизонтали,
		// если не помещается на экране
		binding.songTitle.isSelected = true

		// Инициализация MediaPlayer
		mediaPlayer = MediaPlayer().apply {
			setAudioAttributes(
				AudioAttributes.Builder()
					.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
					.setUsage(AudioAttributes.USAGE_MEDIA)
					.build()
			)
		}

		// Добавление песни
		binding.addSongButton.setOnClickListener {
			val intent: Intent = Intent(Intent.ACTION_GET_CONTENT)
			intent.type = "*/*"
			intent.addCategory(Intent.CATEGORY_OPENABLE)
			getSongLauncher.launch(intent)
		}

		binding.songsListView.setOnItemClickListener { parent, view, position, id ->
			currentSongIndex = position
			setSong()
		}

		// Воспроизведение и пауза песни
		binding.playPauseButton.setOnClickListener {
			if (mediaPlayer.isPlaying)
			{
				mediaPlayer.pause()
				binding.playPauseButton.setBackgroundResource(R.drawable.baseline_play_circle_filled_24)
			}
			else {
				mediaPlayer.start()
				binding.playPauseButton.setBackgroundResource(R.drawable.baseline_pause_circle_filled_24)
			}
		}

		// Переход к предыдущей песни
		binding.previousSong.setOnClickListener {
			mediaPlayer.stop()
			if (currentSongIndex == 0) {
				currentSongIndex = songsUri.size - 1
			}
			else {
				currentSongIndex--
			}
			mediaPlayer.reset()
			setSong()
		}

		// Переход к следующей песни
		binding.nextSong.setOnClickListener {
			mediaPlayer.stop()
			if (currentSongIndex == songsUri.size - 1) {
				currentSongIndex = 0
			}
			else {
				currentSongIndex++
			}
			mediaPlayer.reset()
			setSong()
		}

		binding.songSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
			override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
				if (fromUser) {
					mediaPlayer.seekTo(progress)
				}
			}
			override fun onStartTrackingTouch(seekBar: SeekBar?) {
			}
			override fun onStopTrackingTouch(seekBar: SeekBar?) {
			}
		})
	}

	// Получает полное имя файла (песни)
	private fun Uri.getName(context: Context): String {
		val returnCursor = context.contentResolver.query(this, null, null, null, null)
		val nameIndex = returnCursor!!.getColumnIndex(OpenableColumns.DISPLAY_NAME)
		returnCursor.moveToFirst()
		val fileName = returnCursor.getString(nameIndex)
		returnCursor.close()

		return fileName
	}

	private fun initializeSeekBar() {
		binding.songSeekBar.max = mediaPlayer.duration

		val handler = Handler()
		handler.postDelayed(object : Runnable {
			override fun run() {
				try {
					val currentSongPosition: Int = mediaPlayer.currentPosition
					binding.songSeekBar.progress = currentSongPosition

					val passedTime: Long = currentSongPosition.toLong()
					binding.songPassedTime.text = milliSecondsToTime(passedTime)

					val remainingTime: Long = mediaPlayer.duration.toLong() - passedTime
					binding.songRemainingTime.text = "-${milliSecondsToTime(remainingTime)}"

					// Переход к следующей песни
					// если текущая закончилась
					if (remainingTime.toInt() / 1000 < 0.5) {
						mediaPlayer.stop()
						if (currentSongIndex == songsUri.size - 1) {
							currentSongIndex = 0
						}
						else {
							currentSongIndex++
						}
						mediaPlayer.reset()
						setSong()
					}
					handler.postDelayed(this, 1000)
				} catch (e: Exception) {
					binding.songSeekBar.progress = 0
				}
			}
		}, 0)
	}

	// Переводит миллисекунды в формат времени
	private fun milliSecondsToTime(milliSeconds: Long): String {
		var time: String = ""
		var secondsString: String = ""

		val hours: Int = (milliSeconds / (1000 * 60 * 60)).toInt()
		val minutes: Int = (milliSeconds % (1000 * 60 * 60) / (1000 * 60)).toInt()
		val seconds: Int = (milliSeconds % (1000 * 60 * 60) % (1000 * 60) / 1000).toInt()

		if (hours > 0) {
			time = "${hours}:"
		}
		if (seconds < 10) {
			secondsString = "0$seconds"
		}
		else {
			secondsString = "$seconds"
		}
		time = "${time}${minutes}:${secondsString}"

		return time
	}

	// Устанавливает песню
	private fun setSong() {
		mediaPlayer.reset()
		mediaPlayer.setDataSource(applicationContext, songsUri[currentSongIndex])
		mediaPlayer.prepare()
		initializeSeekBar()

		// Получает имя исполнителя и название песни
		metaRetriever.setDataSource(applicationContext, songsUri[currentSongIndex])
		artist = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
		songTitle = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
		binding.songTitle.text = "${artist} - ${songTitle}"

		// Получение картинки песни
		val songRawImage: ByteArray? = metaRetriever.embeddedPicture
		if (songRawImage != null) {
			songBitmapImage = BitmapFactory.decodeByteArray(songRawImage, 0, songRawImage.size, BitmapFactory.Options())
			binding.songImage.setImageBitmap(songBitmapImage)
		}
		else {
			binding.songImage.setImageResource(R.drawable.file_earmark_music)
		}

		mediaPlayer.start()
		binding.playPauseButton.setBackgroundResource(R.drawable.baseline_pause_circle_filled_24)
	}
}