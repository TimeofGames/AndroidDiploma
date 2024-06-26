package com.example.shuffleit.views

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.shuffleit.R
import com.example.shuffleit.adapters.PlaylistAdapter
import com.example.shuffleit.databinding.AudioControlBinding
import com.example.shuffleit.viewModels.AudioControlViewModel


class AudioControlView: AppCompatActivity() {
    private lateinit var binding: AudioControlBinding
    private val viewModel: AudioControlViewModel by viewModels()

    private val selectFolderLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            showAddPlaylistDialog(it)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.audio_control)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        binding.menuButton.setOnClickListener {
            binding.drawerLayout.open()
        }

        binding.addPlaylistButton.setOnClickListener {
            selectFolderLauncher.launch(null)
        }

        val adapter = PlaylistAdapter(viewModel, binding.drawerLayout)
        binding.playlistRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.playlistRecyclerView.adapter = adapter

        viewModel.playlists.observe(this, Observer { playlists ->
            adapter.setPlaylists(playlists)
        })

        viewModel.trackImage.observe(this, Observer { image->
            if(image == null){
                binding.audioImage.setImageResource(R.drawable.ic_audio_image_placeholder)
            }
            else{
            Glide.with(this)
                .load(image)
                .into(binding.audioImage)
            }
        })
    }

    private fun showAddPlaylistDialog(uri: Uri) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter Playlist Name")

        val input = EditText(this)
        builder.setView(input)

        builder.setPositiveButton("OK") { dialog, which ->
            val playlistName = input.text.toString()
            viewModel.addPlaylist(uri, playlistName)
        }
        builder.setNegativeButton("Cancel") { dialog, which -> dialog.cancel() }

        builder.show()
    }

    override fun onStop() {
        super.onStop()
        Log.d("AudioControlView", "onStop called")
        viewModel.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
