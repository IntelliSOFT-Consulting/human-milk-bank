package com.intellisoft.nndak.screens.dashboard.child

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import com.intellisoft.nndak.MainActivity
import com.intellisoft.nndak.R
import com.intellisoft.nndak.databinding.FragmentBabiesBinding
import com.intellisoft.nndak.databinding.FragmentChildBinding
import com.intellisoft.nndak.databinding.FragmentChildDashboardBinding
import com.intellisoft.nndak.screens.children.ChildFragmentDirections

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ChildDashboardFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ChildDashboardFragment : Fragment() {
    private var _binding: FragmentChildDashboardBinding? = null
    private val binding
        get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChildDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = resources.getString(R.string.app_dashboard)
            setHomeAsUpIndicator(R.drawable.dash)
            setDisplayHomeAsUpEnabled(true)
        }
        setHasOptionsMenu(true)
        (activity as MainActivity).setDrawerEnabled(true)

        binding.apply {
            lnBabyDashboard.setOnClickListener {
                findNavController().navigate(ChildDashboardFragmentDirections.navigateToBabyDashboard())
            }
            lnBabyAssessment.setOnClickListener {
                findNavController().navigate(ChildDashboardFragmentDirections.navigateToBabyAssessment())
            }
            lnBabyFeeding.setOnClickListener {
                findNavController().navigate(ChildDashboardFragmentDirections.navigateToBabyFeeding())
            }
            lnBabyMonitoring.setOnClickListener {
                findNavController().navigate(ChildDashboardFragmentDirections.navigateToBabyMonitoring())
            }
            lnBabyLactation.setOnClickListener {
                findNavController().navigate(ChildDashboardFragmentDirections.navigateToBabyLactation())
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                (requireActivity() as MainActivity).openNavigationDrawer()
                true
            }
            else -> false
        }
    }
}