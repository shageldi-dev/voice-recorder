package com.gs.voicerecord

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.github.squti.androidwaverecorder.WaveRecorder
import com.gs.voicerecord.ui.theme.VoiceRecordTheme
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import kotlin.math.ceil
import kotlin.random.Random


class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalLayoutApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val filePath: String = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "audi.wav"
        ).absolutePath
        val cvs = "voices.txt"
        val waveRecorder = WaveRecorder(filePath)
        waveRecorder.noiseSuppressorActive = true

        setContent {
            VoiceRecordTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val timerViewModel: TimerViewModel by viewModels()
                    val result = remember { mutableStateOf<Uri?>(null) }
                    val launcher =
                        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                            result.value = it.data?.data
                        }
                    val context = LocalContext.current
                    var isFirst by rememberSaveable {
                        mutableStateOf(true)
                    }

                    var currentFile by rememberSaveable {
                        mutableStateOf("")
                    }

                    var isSaved by rememberSaveable {
                        mutableStateOf(true)
                    }

                    var text by rememberSaveable {
                        mutableStateOf("")
                    }

                    var canDelete by rememberSaveable {
                        mutableStateOf(false)
                    }

                    var list by remember {
                        mutableStateOf(processText(removeAdditionalSymbols(text.lowercase())))
                    }

                    var words by remember {
                        mutableStateOf<List<String>>(emptyList())
                    }

                    var step by rememberSaveable {
                        mutableIntStateOf(0)
                    }

                    var recording by rememberSaveable {
                        mutableStateOf(false)
                    }

                    var amplitudes by remember {
                        mutableStateOf<List<Int>>(emptyList())
                    }

                    val timerValue by timerViewModel.timer.collectAsState()

                    var canNext by rememberSaveable {
                        mutableStateOf(false)
                    }

                    val limit = 6

                    LaunchedEffect(step, list) {
                        words = try {
                            list.subList(step * limit, step * limit + limit)
                        } catch (ex: Exception) {
                            emptyList()
                        }
                    }


                    BackHandler(isSaved.not() || result.value!=null) {
                        if(isSaved.not())
                            Toast.makeText(context, "Click next to save", Toast.LENGTH_SHORT).show()
                        else if(result.value!=null){
                            result.value = null
                        }
                    }

                    LaunchedEffect(text) {
                        list = processText(removeAdditionalSymbols(text.lowercase()))
                    }
                    LaunchedEffect(true) {
                        if (isFirst) {
                            val uri = Uri.parse("package:com.gs.voicerecord")

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                        uri
                                    )
                                )
                            }
                            isFirst = false
                        }
                    }

                    waveRecorder.onAmplitudeListener = {
                        amplitudes = amplitudes.plus(it)
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (result.value == null) {
                            Image(
                                painter = painterResource(id = R.drawable.panos),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .zIndex(0f),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .zIndex(2f),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (result.value == null) {
                                Button(onClick = {
                                    val intent = Intent(
                                        Intent.ACTION_OPEN_DOCUMENT,
                                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                    )
                                        .apply {
                                            addCategory(Intent.CATEGORY_OPENABLE)
                                        }
                                    launcher.launch(intent)
                                }) {
                                    Text(text = "Select Document")
                                }
                            }
                            result.value?.let { image ->

                                try {
                                    val `in` = contentResolver.openInputStream(image)
                                    val r = BufferedReader(InputStreamReader(`in`))
                                    val total = StringBuilder()
                                    var line: String?
                                    while (r.readLine().also { line = it } != null) {
                                        total.append(line).append('\n')
                                    }
                                    val content = total.toString()
                                    Log.e("OK", content)
                                    text = content
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                if (step < ceil((list.size / limit).toDouble()).toInt()) {
                                    Text(
                                        text = "Step: ${step + 1} / ${ceil((list.size / limit).toDouble()).toInt()}",
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                                        verticalArrangement = Arrangement.spacedBy(20.dp),
                                    ) {
                                        repeat(words.size) {
                                            Text(
                                                text = words[it],
                                                style = MaterialTheme.typography.headlineLarge.copy(
                                                    fontSize = 40.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                        }
                                    }
                                    AudioVisualizer(
                                        amplitudes = amplitudes,
                                        modifier = Modifier.height(200.dp)
                                    )
                                    TimerScreen(
                                        timerValue = timerValue,
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Button(onClick = {
                                            canNext = false
                                            amplitudes = emptyList()
                                            canDelete = false
                                            recording = true
                                            val uuid = UUID.randomUUID()
                                            val str = uuid.toString()
                                            val sdf = SimpleDateFormat("dd_M_yyyy-hh_mm_ss")
                                            val currentDate = sdf.format(Date())
                                            val file = File(
                                                Environment.getExternalStoragePublicDirectory(
                                                    Environment.DIRECTORY_DOWNLOADS
                                                ),
                                                "${currentDate}_${str}.wav"
                                            )
                                            if (!file.exists())
                                                file.createNewFile()
                                            currentFile = file.absolutePath
                                            waveRecorder.changeFilePath(file.absolutePath)
                                            waveRecorder.startRecording()
                                            timerViewModel.startTimer()
                                            isSaved = false
                                        }) {
                                            Text(text = "Start")
                                        }
                                        Button(onClick = {
                                            canNext = false
                                            recording = false
                                            canDelete = false
                                            amplitudes = emptyList()
                                            timerViewModel.stopTimer()
                                            isSaved = true
                                            val myFile: File = File(currentFile)
                                            if (myFile.exists()) myFile.delete()

                                        }, enabled = canDelete) {
                                            Text(text = "Delete")
                                        }
                                        Button(onClick = {
                                            canNext = true
                                            recording = false
                                            canDelete = true
                                            timerViewModel.pauseTimer()
                                            waveRecorder.stopRecording()
                                            isSaved = false
                                        }) {
                                            Text(text = "Stop")
                                        }
                                    }

                                    fun saveToCvs(path: String, data: String) {
                                        val cvsFile = File(
                                            Environment.getExternalStoragePublicDirectory(
                                                Environment.DIRECTORY_DOWNLOADS
                                            ),
                                            path
                                        )
                                        if (!cvsFile.exists()) cvsFile.createNewFile()
                                        cvsFile.appendText("\n${data}")
                                    }

                                    fun getFileSize(path: String): Int {
                                        val myFile: File = File(path)
                                        return java.lang.String.valueOf(myFile.length() / 1024)
                                            .toInt()
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 22.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        if (step != 0 && recording.not()) {
                                            TextButton(onClick = {
                                                timerViewModel.stopTimer()
                                                canNext = false
                                                saveToCvs(
                                                    cvs,
                                                    data = "${
                                                        currentFile.split("/").asReversed().first()
                                                    },${words.joinToString("|")},${
                                                        getFileSize(
                                                            currentFile
                                                        )
                                                    }"
                                                )
                                                amplitudes = emptyList()
                                                canDelete = false
                                                recording = false
                                                isSaved = true
                                                step = step.dec()

                                            }, enabled = canNext) {
                                                Text(text = "Previous")
                                            }
                                        }
                                        if (step < ceil((list.size / limit).toDouble()).toInt() && recording.not()) {
                                            TextButton(onClick = {
                                                timerViewModel.stopTimer()
                                                canNext = false
                                                saveToCvs(
                                                    cvs,
                                                    data = "${
                                                        currentFile.split("/").asReversed().first()
                                                    },${words.joinToString("|")},${
                                                        getFileSize(
                                                            currentFile
                                                        )
                                                    }"
                                                )
                                                amplitudes = emptyList()
                                                canDelete = false
                                                recording = false
                                                isSaved = true
                                                step = step.inc()
                                            }, enabled = canNext) {
                                                Text(text = "Next")
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "Finished",
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                    OutlinedButton(onClick = {
                                        canNext = false
                                        amplitudes = emptyList()
                                        canDelete = false
                                        recording = false
                                        step = 0
                                        timerViewModel.stopTimer()
                                        isSaved = true
                                    }) {
                                        Text(text = "Start again")
                                    }
                                }

                            }

                        }
                        Text(
                            text = "Developed BY: Meredow Begench", modifier = Modifier
                                .align(
                                    Alignment.BottomCenter
                                )
                                .padding(bottom = 6.dp)
                                .zIndex(2f)
                        )
                    }
                }
            }
        }
    }
}

fun removeAdditionalSymbols(input: String): String {
    // Regular expression to match non-Unicode letters, numbers, and spaces
    val regexSymbols = Regex("[^\\p{L}0-9 -]")
    // Regular expression to match multiple whitespace characters
    val regexWhitespace = Regex("\\s+")

    // Replace additional symbols
    var result = input.replace(regexSymbols, "")
    // Remove extra whitespace
    result = result.replace(regexWhitespace, " ")

    return result.trim() // Trim leading and trailing whitespace
}

fun processText(input: String): List<String> {
    val words = input.split("\\s+".toRegex()) // Split the text into words
    val wordCounts = words.groupingBy { it }.eachCount() // Count the occurrences of each word

    val filteredWords =
        words.filter { (wordCounts[it] ?: 0) < 4 } // Remove words repeated 4 times or more

    return filteredWords
}
