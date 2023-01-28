package com.drokka.emu.symicon.symfascinate

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [CarouselFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CarouselFragment : Fragment(), Observer<ArrayList<String>> {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    var carousal:RecyclerView? = null
    val viewModel:MainViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
       val view = inflater.inflate(R.layout.fragment_carousel, container, false)
        carousal = view?.findViewById(R.id.videoCarousel)
        carousal?.setAdapter(context?.let {

                VideoCarouselAdapter(viewModel, carousal!!,   it)
            })
        Log.d("CarouselFragment onCreateView", "Adapter set and adding as observer")

        viewModel.videoListLiveData.observe(viewLifecycleOwner, carousal?.adapter as VideoCarouselAdapter)
        return view
    }

    override fun onChanged(t: ArrayList<String>?) {
        Log.d("onChanged", "CarouselFragment is also Observer of main view model video list" +
                "size is ${t?.size}")

        carousal?.refreshDrawableState()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment CarouselFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(  param1: String, param2: String) =
            CarouselFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}