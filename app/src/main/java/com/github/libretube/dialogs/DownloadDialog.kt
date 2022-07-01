package com.github.libretube.dialogs

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.util.TypedValue
import android.widget.ArrayAdapter
import androidx.core.app.ActivityCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.DialogFragment
import com.github.libretube.activities.MainActivity
import com.github.libretube.R
import com.github.libretube.databinding.DialogDownloadBinding
import com.github.libretube.obj.Streams
import com.github.libretube.services.DownloadService
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DownloadDialog : DialogFragment() {
    private val TAG = "DownloadDialog"
    private lateinit var binding: DialogDownloadBinding

    private lateinit var streams: Streams
    private lateinit var videoId: String
    private var duration = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            streams = arguments?.getParcelable("streams")!!
            videoId = arguments?.getString("video_id")!!

            val mainActivity = activity as MainActivity
            val builder = MaterialAlertDialogBuilder(it)
            binding = DialogDownloadBinding.inflate(layoutInflater)

            // request storage permissions if not granted yet
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Log.d("myz", "" + Build.VERSION.SDK_INT)
                if (!Environment.isExternalStorageManager()) {
                    ActivityCompat.requestPermissions(
                        mainActivity,
                        arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.MANAGE_EXTERNAL_STORAGE
                        ),
                        1
                    ) // permission request code is just an int
                }
            } else {
                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        mainActivity,
                        arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ),
                        1
                    )
                }
            }

            var vidName = arrayListOf<String>()
            var vidUrl = arrayListOf<String>()

            // add empty selection
            vidName.add(getString(R.string.no_video))
            vidUrl.add("")

            // add all available video streams
            for (vid in streams.videoStreams!!) {
                val name = vid.quality + " " + vid.format
                vidName.add(name)
                vidUrl.add(vid.url!!)
            }

            var audioName = arrayListOf<String>()
            var audioUrl = arrayListOf<String>()

            // add empty selection
            audioName.add(getString(R.string.no_audio))
            audioUrl.add("")

            // add all available audio streams
            for (audio in streams.audioStreams!!) {
                val name = audio.quality + " " + audio.format
                audioName.add(name)
                audioUrl.add(audio.url!!)
            }

            val videoArrayAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                vidName
            )
            videoArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.videoSpinner.adapter = videoArrayAdapter
            binding.videoSpinner.setSelection(1)

            val audioArrayAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                audioName
            )
            audioArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.audioSpinner.adapter = audioArrayAdapter
            binding.audioSpinner.setSelection(1)

            binding.download.setOnClickListener {
                val selectedAudioUrl = audioUrl[binding.audioSpinner.selectedItemPosition]
                val selectedVideoUrl = vidUrl[binding.videoSpinner.selectedItemPosition]

                val intent = Intent(context, DownloadService::class.java)
                intent.putExtra("videoId", videoId)
                intent.putExtra("videoUrl", selectedVideoUrl)
                intent.putExtra("audioUrl", selectedAudioUrl)
                intent.putExtra("duration", duration)
                context?.startService(intent)
                dismiss()
            }

            val typedValue = TypedValue()
            this.requireActivity().theme.resolveAttribute(R.attr.colorPrimaryDark, typedValue, true)
            val hexColor = String.format("#%06X", (0xFFFFFF and typedValue.data))
            val appName = HtmlCompat.fromHtml(
                "Libre<span  style='color:$hexColor';>Tube</span>",
                HtmlCompat.FROM_HTML_MODE_COMPACT
            )
            binding.title.text = appName

            builder.setView(binding.root)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
