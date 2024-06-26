package com.example.shuffleit.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.shuffleit.R
import com.example.shuffleit.databinding.PlaylistItemBinding
import com.example.shuffleit.viewModels.AudioControlViewModel
import com.example.shuffleit.data.database.entities.PlayListEntity

class PlaylistAdapter(
    private val viewModel: AudioControlViewModel,
    private val drawerLayout: DrawerLayout
) : RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>() {

    private var playlists: List<PlayListEntity> = emptyList()

    fun setPlaylists(playlists: List<PlayListEntity>) {
        this.playlists = playlists
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val binding = PlaylistItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlaylistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val playlist = playlists[position]
        holder.bind(playlist)
    }

    override fun getItemCount(): Int {
        return playlists.size
    }

    inner class PlaylistViewHolder(private val binding: PlaylistItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(playlist: PlayListEntity) {
            binding.playlistTitle.text = playlist.title

            binding.root.setOnClickListener {
                drawerLayout.close()
                viewModel.onPlaylistClick(playlist)
            }

            binding.deleteButton.setOnClickListener {
                showDeleteConfirmationDialog(binding.root.context, playlist)
            }
        }

        private fun showDeleteConfirmationDialog(context: Context, playlist: PlayListEntity) {
            AlertDialog.Builder(context)
                .setTitle(R.string.delete_playlist)
                .setMessage(R.string.delete_playlist_confirmation)
                .setPositiveButton(R.string.delete) { _, _ ->
                    viewModel.deletePlayList(playlist)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }
}
