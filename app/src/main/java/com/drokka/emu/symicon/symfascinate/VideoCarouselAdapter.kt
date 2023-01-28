package com.drokka.emu.symicon.symfascinate

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.VideoView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import  com.drokka.emu.symicon.symfascinate.databinding.FragmentMyVideoViewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VideoCarouselAdapter(
    //val videoFilePathList: MutableLiveData<MutableList<String>>,
    val viewModel: MainViewModel,
    val videoCarousel: RecyclerView, val context:Context
): Observer<ArrayList<String>>, RecyclerView.Adapter<VideoCarouselAdapter.ViewHolder>() {

   // var fileList = ArrayList<String>()


        inner class ViewHolder(binding: FragmentMyVideoViewBinding) :
            RecyclerView.ViewHolder(binding.root), View.OnClickListener {

            val mp4VideoView = binding.mp4VideoView

            override fun onClick(p0: View?) {
                    Log.d("onClick", "ViewHolder clicked")            }

        }
/*
    fun populate(view: View?, index: Int) {
        if(videoFilePathList.value?.elementAt(index) != null){
            val file = videoFilePathList.value?.elementAt(index)?.let { File(it)}

            file?.let { (view as VideoView?)?.setVideoURI(Uri.fromFile(file))
                (view as VideoView).start()
            }

        }
    }

    fun onNewItem(index: Int) {
        Log.d("VideoCarouselAdapter onNewItem", "onNewItem called")

        val view = VideoView(context)
        videoCarousel.addView(view)
        populate(view, index)
    }

 */

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val viewHolder= ViewHolder(
                FragmentMyVideoViewBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                )

        return viewHolder
    }

    override fun getItemCount(): Int {
        Log.d("VideoCarouselAdapter onBindViewHolder","count ${viewModel.videoListLiveData.value?.size}")
            return viewModel.videoListLiveData.value?.size ?: 0
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.d("VideoCarouselAdapter onBindViewHolder", "the input position is $position")
        if(viewModel.videoListLiveData.value?.size!! > position) {
                val file = File(viewModel.videoListLiveData.value!!.elementAt(position))

                holder.mp4VideoView.setVideoURI(Uri.fromFile(file))
                holder.mp4VideoView.start()
            Log.d("VideoCarouselAdapter onBindViewHolder","video file ${file.name} and holder $holder")
        }
    }

    override fun onChanged(t: ArrayList<String>?) {
        if (t != null) {
         //   fileList = t
            Log.d("VideoCarouselAdapter onChanged", "the input arraylist size is ${t.size}")
            videoCarousel.isDirty
        }
    }


}