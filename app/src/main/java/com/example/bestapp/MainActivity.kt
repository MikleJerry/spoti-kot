package com.example.bestapp

import android.content.Context
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
import androidx.lifecycle.ViewModelProvider
import com.example.bestapp.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
	private lateinit var binding: ActivityMainBinding
	// TODO: Реализовать функцию добавления песен в хранилище, чтобы при выходе из приложения сохранялся список песен
	private lateinit var viewModel: MainViewModel
	private lateinit var dataStoreManager: DataStoreManager
	private lateinit var mediaPlayer: MediaPlayer
	private val metaRetriever: MediaMetadataRetriever = MediaMetadataRetriever()

	private var songsUri: ArrayList<Uri> = arrayListOf()
	private var currentSongIndex: Int = 0
	private var songTitle: String? = null
	private var artist: String? = null
	private lateinit var songBitmapImage: Bitmap
	private lateinit var arrayAdapter: ArrayAdapter<*>
	private var songsNames: ArrayList<String> = arrayListOf()

	// Инициализация launcher
	private val getSongLauncher =
		registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
			if (uri != null) {
				addSong(uri)
			}
		}

	private fun addSong(uri: Uri) {
		songsUri.add(uri)
		songsNames.add(uri.getName(applicationContext))

		// Помещаем данные в DataStore
		viewModel.setSongs(songsNames)

		// Загружаем песню в массив
		arrayAdapter = ArrayAdapter(applicationContext, android.R.layout.simple_list_item_1, songsNames)
		binding.songsListView.adapter = arrayAdapter
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityMainBinding.inflate(layoutInflater)
		setContentView(binding.root)
		viewModel = ViewModelProvider(this)[MainViewModel::class.java]
		dataStoreManager = DataStoreManager(this)

		checkSongUri()

		// Инициализация MediaPlayer
		mediaPlayer = MediaPlayer().apply {
			setAudioAttributes(
				AudioAttributes.Builder()
					.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
					.setUsage(AudioAttributes.USAGE_MEDIA)
					.build()
			)
		}

		binding.apply {
			songTitle.text = ""
			// Показывает оставшееся и пройденное время песни
			songPassedTime.text = getString(R.string.song_passed_time, "0:00")
			songRemainingTime.text = getString(R.string.song_remaining_time, "0:00")

			// Заставляет текст двигаться по горизонтали,
			// если не помещается на экране
			songTitle.isSelected = true

			// Добавление песни
			addSongButton.setOnClickListener {getSongLauncher.launch(arrayOf("*/*"))}

			songsListView.setOnItemClickListener { parent, view, position, id ->
				currentSongIndex = position
				setSong()
			}

			// Воспроизведение и пауза песни
			playPauseButton.setOnClickListener {
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
			previousSong.setOnClickListener {
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
			nextSong.setOnClickListener {
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

			songSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
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
	}

	private fun checkSongUri() {
		// Получение данных из DataStore
		binding.apply {
			viewModel.getSongs.observe(this@MainActivity) {song ->
				Toast.makeText(applicationContext, song, Toast.LENGTH_SHORT).show()
			}
		}
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
					val passedTime: Long = currentSongPosition.toLong()
					val remainingTime: Long = mediaPlayer.duration.toLong() - passedTime

					binding.apply {
						songSeekBar.progress = currentSongPosition
						songPassedTime.text = getString(R.string.song_passed_time, milliSecondsToTime(passedTime))
						songRemainingTime.text = getString(R.string.song_remaining_time, milliSecondsToTime(remainingTime))
					}

					// Переход к следующей песни
					// если текущая закончилась
					if (remainingTime.toInt() / 1000 < 0.2) {
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
		var time = ""
		var secondsString = ""

		val hours: Int = (milliSeconds / (1000 * 60 * 60)).toInt()
		val minutes: Int = (milliSeconds % (1000 * 60 * 60) / (1000 * 60)).toInt()
		val seconds: Int = (milliSeconds % (1000 * 60 * 60) % (1000 * 60) / 1000).toInt()

		if (hours > 0) {
			time = "${hours}:"
		}

		secondsString = if (seconds < 10) {
			"0$seconds"
		} else {
			"$seconds"
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
		binding.songTitle.text = getString(R.string.song_title, artist, songTitle)

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