package com.intellisoft.nndak.screens.dashboard.prescription

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextUtils
import android.text.TextWatcher
import android.view.*
import android.widget.Toast
import androidx.activity.addCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import ca.uhn.fhir.context.FhirContext
import cn.pedant.SweetAlert.SweetAlertDialog
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.intellisoft.nndak.FhirApplication
import com.intellisoft.nndak.MainActivity
import com.intellisoft.nndak.R
import com.intellisoft.nndak.databinding.UpdatePrescriptionBinding
import com.intellisoft.nndak.dialogs.ConfirmationDialog
import com.intellisoft.nndak.logic.Logics.Companion.BREAST_MILK
import com.intellisoft.nndak.logic.Logics.Companion.DHM_CONSENT
import com.intellisoft.nndak.logic.Logics.Companion.DHM_REASON
import com.intellisoft.nndak.logic.Logics.Companion.DHM_ROUTE
import com.intellisoft.nndak.logic.Logics.Companion.DHM_TYPE
import com.intellisoft.nndak.logic.Logics.Companion.DHM_VOLUME
import com.intellisoft.nndak.logic.Logics.Companion.EBM_ROUTE
import com.intellisoft.nndak.logic.Logics.Companion.EBM_VOLUME
import com.intellisoft.nndak.logic.Logics.Companion.FORMULA_ROUTE
import com.intellisoft.nndak.logic.Logics.Companion.FORMULA_TYPE
import com.intellisoft.nndak.logic.Logics.Companion.FORMULA_VOLUME
import com.intellisoft.nndak.logic.Logics.Companion.IV_ROUTE
import com.intellisoft.nndak.logic.Logics.Companion.IV_VOLUME
import com.intellisoft.nndak.models.FeedDataItem
import com.intellisoft.nndak.models.FeedItem
import com.intellisoft.nndak.models.Prescription
import com.intellisoft.nndak.models.PrescriptionItem
import com.intellisoft.nndak.viewmodels.PatientDetailsViewModel
import com.intellisoft.nndak.viewmodels.PatientDetailsViewModelFactory
import com.intellisoft.nndak.viewmodels.ScreenerViewModel
import kotlinx.android.synthetic.main.success_dialog.*
import kotlinx.android.synthetic.main.update_prescription.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class EditPrescriptionFragment : Fragment() {
    private lateinit var confirmationDialog: ConfirmationDialog
    private var _binding: UpdatePrescriptionBinding? = null
    private lateinit var fhirEngine: FhirEngine
    private val viewModel: ScreenerViewModel by viewModels()
    private val args: EditPrescriptionFragmentArgs by navArgs()
    private lateinit var patientDetailsViewModel: PatientDetailsViewModel
    private lateinit var currentWeight: String
    private lateinit var totalFeeds: String
    private lateinit var feedFrequency: String
    private lateinit var supp: String
    private lateinit var other: String
    private var aggregateTotal: Float = 0f

    private val feedsList: MutableList<FeedDataItem> = mutableListOf()
    private val binding
        get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = UpdatePrescriptionBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = "Edit Prescription"
            setHomeAsUpIndicator(R.drawable.dash)
            setDisplayHomeAsUpEnabled(true)
        }
        updateArguments()
        onBackPressed()
        observeResourcesSaveAction()
        if (savedInstanceState == null) {
            addQuestionnaireFragment()
        }
        setHasOptionsMenu(true)
        binding.apply {
            breadcrumb.page.text =
                Html.fromHtml("Baby Panel >Baby's panel <font color=\"#37379B\">Prescribe feeds</font>")
            breadcrumb.page.setOnClickListener {
                findNavController().navigateUp()
            }

        }
        createUI()

        fhirEngine = FhirApplication.fhirEngine(requireContext())
        patientDetailsViewModel =
            ViewModelProvider(
                this,
                PatientDetailsViewModelFactory(
                    requireActivity().application,
                    fhirEngine,
                    args.patientId
                )
            ).get(PatientDetailsViewModel::class.java)

        patientDetailsViewModel.getCurrentPrescriptions()
        patientDetailsViewModel.livePrescriptionsData.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                updateUI(it.first())
            }
        }

        binding.apply {
            btnSubmit.setOnClickListener {
                onSubmitAction()
            }
            btnCancel.setOnClickListener {
                showCancelScreenerQuestionnaireAlertDialog()
            }
        }
        confirmationDialog = ConfirmationDialog(
            this::okClick,
            resources.getString(R.string.app_okay_message)
        )


    }

    private fun updateUI(it: PrescriptionItem) {
        binding.apply {

            eWeight.setText(it.cWeight.toString())
            eTotal.setText(it.totalVolume.toString())
            eFrequency.setText(it.frequency.toString())
            otherSup.appFrequency.setText(it.additionalFeeds)
            otherValue.appFrequency.setText(it.supplements)

            if (it.breastMilk != "N/A") {
                cbBreast.isChecked = true
                tvBreast.visibility = View.VISIBLE
                lnBrestMilk.visibility = View.VISIBLE
                updateVolumeFrequency(bfVolume.volume, it.feed, BREAST_MILK)

            }
            if (it.formula != "N/A") {
                cbFormula.isChecked = true
                tvFormula.visibility = View.VISIBLE
                lnFormula.visibility = View.VISIBLE
                lnFormulaAlt.visibility = View.VISIBLE
                updateVolumeFrequencyRouteType(
                    formulaVolume.volume,
                    formulaRoute.appType,
                    formulaType.appFrequency,
                    it.feed,
                    FORMULA_VOLUME
                )
            }
            if (it.ebm != "N/A") {
                cbEbm.isChecked = true
                tvExpressed.visibility = View.VISIBLE
                lnEbmMilk.visibility = View.VISIBLE

                updateVolumeFrequencyRoute(
                    ebmVolume.volume,
                    ebmRoute.appType,
                    it.feed,
                    EBM_VOLUME
                )
            }
            if (it.donorMilk != "N/A") {
                cbDhm.isChecked = true
                tvDhm.visibility = View.VISIBLE
                lnDhmMilk.visibility = View.VISIBLE
                lnDhmMilkOther.visibility = View.VISIBLE
                lnDhmReason.visibility = View.VISIBLE
                val con = if (it.consent == "Signed") {
                    "Yes"
                } else {
                    "No"
                }
                dhmConsent.appFrequency.setText(con)
                dhmReason.volume.setText(it.dhmReason)
                updateVolumeFrequencyRouteType(
                    dhmVolume.volume,
                    dhmRoute.appType, dhmType.appFrequency, it.feed, DHM_VOLUME
                )
            }
            if (it.ivFluids != "N/A") {
                cbFluid.isChecked = true
                tvIvFluids.visibility = View.VISIBLE
                lnIvFluids.visibility = View.VISIBLE
                updateVolumeFrequencyRoute(
                    ivVolume.volume,
                    ivRoute.appType,
                    it.feed,
                    IV_VOLUME
                )
            }
            if (it.additionalFeeds != "N/A") {
                if (it.additionalFeeds == "Yes") {
                    otherValue.tilFre.visibility = View.VISIBLE
                }
            }

        }

        regulateViews()
    }

    private fun regulateViews() {
        binding.apply {

            cbBreast.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    tvBreast.visibility = View.VISIBLE
                    lnBrestMilk.visibility = View.VISIBLE
                } else {
                    tvBreast.visibility = View.GONE
                    lnBrestMilk.visibility = View.GONE
                }
                feedsList.clear()
            }

            cbFormula.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    tvFormula.visibility = View.VISIBLE
                    lnFormula.visibility = View.VISIBLE
                    lnFormulaAlt.visibility = View.VISIBLE
                } else {
                    tvFormula.visibility = View.GONE
                    lnFormula.visibility = View.GONE
                    lnFormulaAlt.visibility = View.GONE
                }
                feedsList.clear()
            }

            cbEbm.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    tvExpressed.visibility = View.VISIBLE
                    lnEbmMilk.visibility = View.VISIBLE
                } else {
                    tvExpressed.visibility = View.GONE
                    lnEbmMilk.visibility = View.GONE
                }
                feedsList.clear()
            }

            cbDhm.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    tvDhm.visibility = View.VISIBLE
                    lnDhmMilk.visibility = View.VISIBLE
                    lnDhmMilkOther.visibility = View.VISIBLE
                    lnDhmReason.visibility = View.VISIBLE

                } else {
                    tvDhm.visibility = View.GONE
                    lnDhmMilk.visibility = View.GONE
                    lnDhmMilkOther.visibility = View.GONE
                    lnDhmReason.visibility = View.GONE

                }
                feedsList.clear()
            }

            cbFluid.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    tvIvFluids.visibility = View.VISIBLE
                    lnIvFluids.visibility = View.VISIBLE
                } else {
                    tvIvFluids.visibility = View.GONE
                    lnIvFluids.visibility = View.GONE
                }
                feedsList.clear()
            }
        }
    }

    private fun updateVolumeFrequencyRouteType(
        volume: TextInputEditText,
        route: TextInputEditText,
        type: TextInputEditText,
        feed: List<FeedItem>?,
        code: String
    ) {
        val vol = feed?.find { it.resourceId == code }?.volume
        val rou = feed?.find { it.resourceId == code }?.route
        val typ = feed?.find { it.resourceId == code }?.type

        volume.setText(vol)
        route.setText(rou)
        type.setText(typ)
    }


    private fun updateVolumeFrequency(
        volume: TextInputEditText,
        feed: List<FeedItem>?,
        code: String
    ) {
        val vol = feed?.find { it.resourceId == code }?.volume

        volume.setText(vol)
    }

    private fun updateVolumeFrequencyRoute(
        volume: TextInputEditText,
        route: TextInputEditText,
        feed: List<FeedItem>?,
        code: String
    ) {
        val vol = feed?.find { it.resourceId == code }?.volume
        val rou = feed?.find { it.resourceId == code }?.route

        volume.setText(vol)
        route.setText(rou)
    }


    private fun createUI() {

        binding.apply {

            dhmType.tilFre.hint = getString(R.string._dhm_type)
            dhmConsent.tilFre.hint = getString(R.string._consent)
            otherSup.tilFre.hint = getString(R.string.app_add)
            otherValue.tilFre.hint = getString(R.string.supp)
            formulaType.tilFre.hint = getString(R.string.type)
            /**
             * Freq
             */
            showOptions(eFrequency, R.menu.menu_frequency)
            showOptions(dhmType.appFrequency, R.menu.menu_in_transaction)
            showOptions(dhmConsent.appFrequency, R.menu.menu_consent)
            showOptions(otherSup.appFrequency, R.menu.menu_consent)
            showOptions(otherValue.appFrequency, R.menu.menu_supp)
            showOptions(formulaType.appFrequency, R.menu.menu_formula)

            /**
             * Route
             */
            showOptions(ebmRoute.appType, R.menu.route)
            showOptions(dhmRoute.appType, R.menu.route)
            showOptions(formulaRoute.appType, R.menu.route)

            /**
             * Listeners
             */
            listenChanges(eWeight, tilWeight, "Please enter valid wight value")
            listenChanges(eTotal, tliTotal, "Please enter valid value")
            listenChanges(bfVolume.volume, bfVolume.tilVolume, "Please enter valid value")
            listenChanges(ebmVolume.volume, ebmVolume.tilVolume, "Please enter valid value")
            listenChanges(formulaVolume.volume, formulaVolume.tilVolume, "Please enter valid value")
            listenChanges(dhmVolume.volume, dhmVolume.tilVolume, "Please enter valid  value")
            listenChanges(ivVolume.volume, ivVolume.tilVolume, "Please enter valid  value")

        }
    }

    private fun listenChanges(
        input: TextInputEditText,
        inputLayout: TextInputLayout,
        error: String
    ) {

        CoroutineScope(Dispatchers.Default).launch {
            input.addTextChangedListener(object : TextWatcher {

                override fun afterTextChanged(editable: Editable) {
                    try {
                        if (editable.toString().isNotEmpty()) {
                            val newValue = editable.toString()
                            input.removeTextChangedListener(this)
                            val position: Int = input.selectionEnd
                            input.setText(newValue)
                            if (position > (input.text?.length ?: 0)) {
                                input.text?.let { input.setSelection(it.length) }
                            } else {
                                input.setSelection(position);
                            }
                            input.addTextChangedListener(this)
                            inputLayout.error = null
                        } else {
                            inputLayout.error = error
                        }
                    } catch (e: Exception) {

                    }
                }

                override fun beforeTextChanged(
                    s: CharSequence, start: Int,
                    count: Int, after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence, start: Int,
                    before: Int, count: Int
                ) {

                }
            })
        }
    }

    private fun showOptions(textInputEditText: TextInputEditText, menuItem: Int) {
        textInputEditText.setOnClickListener {
            PopupMenu(requireContext(), textInputEditText).apply {
                menuInflater.inflate(menuItem, menu)
                setOnMenuItemClickListener { item ->
                    textInputEditText.setText(item.title)
                    true
                }
                show()
            }
        }
    }

    private fun okClick() {
        confirmationDialog.dismiss()
        (activity as MainActivity).displayDialog()

        val questionnaireFragment =
            childFragmentManager.findFragmentByTag(QUESTIONNAIRE_FRAGMENT_TAG) as QuestionnaireFragment
        try {

            val data = Prescription(
                currentWeight = currentWeight.toDouble().toString(),
                totalFeeds = totalFeeds.toDouble().toString(),
                feedFrequency = feedFrequency,
                supplements = other,
                additional = supp,
                data = feedsList
            )
            viewModel.updatePrescription(
                questionnaireFragment.getQuestionnaireResponse(), args.patientId, data
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                requireContext(),
                getString(R.string.inputs_missing),
                Toast.LENGTH_SHORT
            ).show()

        }
    }

    private fun updateArguments() {
        requireArguments().putString(QUESTIONNAIRE_FILE_PATH_KEY, "feed-prescription.json")
    }

    private fun observeResourcesSaveAction() {
        viewModel.customMessage.observe(viewLifecycleOwner) {
            if (!it.success) {
                Toast.makeText(
                    requireContext(), it.message,
                    Toast.LENGTH_SHORT
                ).show()
                (activity as MainActivity).hideDialog()
                return@observe
            }
            (activity as MainActivity).hideDialog()
            val dialog = SweetAlertDialog(requireContext(), SweetAlertDialog.CUSTOM_IMAGE_TYPE)
                .setTitleText("Success")
                .setContentText(resources.getString(R.string.app_okay_saved))
                .setCustomImage(R.drawable.smile)
                .setConfirmClickListener { sDialog ->
                    run {
                        sDialog.dismiss()
                        findNavController().navigateUp()
                    }
                }
            dialog.setCancelable(false)
            dialog.show()
        }


    }

    private fun addQuestionnaireFragment() {
        try {
            val fragment = QuestionnaireFragment()
            fragment.arguments =
                bundleOf(QuestionnaireFragment.EXTRA_QUESTIONNAIRE_JSON_STRING to viewModel.questionnaire)
            childFragmentManager.commit {
                add(
                    R.id.add_patient_container, fragment,
                    QUESTIONNAIRE_FRAGMENT_TAG
                )
            }
        } catch (e: Exception) {
            Timber.e("Exception ${e.localizedMessage}")
        }
    }

    private fun addQuestionnaireFragment(pair: Pair<String, String>) {
        Timber.e("First ${pair.first}")
        val fragment = QuestionnaireFragment()
        fragment.arguments =
            bundleOf(
                QuestionnaireFragment.EXTRA_QUESTIONNAIRE_JSON_STRING to pair.first,
                QuestionnaireFragment.EXTRA_QUESTIONNAIRE_RESPONSE_JSON_STRING to pair.second
            )
        childFragmentManager.commit {
            add(R.id.add_patient_container, fragment, QUESTIONNAIRE_FRAGMENT_TAG)
        }
    }


    private fun onSubmitAction() {

        binding.apply {
            currentWeight = eWeight.text.toString()
            totalFeeds = eTotal.text.toString()
            supp = otherSup.appFrequency.text.toString()
            other = otherValue.appFrequency.text.toString()
            totalFeeds = eTotal.text.toString()
            feedFrequency = eFrequency.text.toString()

            if (TextUtils.isEmpty(currentWeight)) {
                tilWeight.error = "Please Enter valid value"
                return
            }

            if (TextUtils.isEmpty(totalFeeds)) {
                tliTotal.error = "PLease enter valid volume"
                return
            }
            if (TextUtils.isEmpty(feedFrequency)) {
                tliFrequency.error = "PLease select frequency"
                return
            }
            aggregateTotal = totalFeeds.toFloat()

        }

        if (binding.cbBreast.isChecked || binding.cbEbm.isChecked || binding.cbFormula.isChecked || binding.cbDhm.isChecked || binding.cbFluid.isChecked) {
            binding.apply {
                feedsList.clear()
                if (cbBreast.isChecked) {
                    val vol = bfVolume.volume.text.toString()
                    if (checkEmptyData(vol)) {
                        return
                    }
                    feedsList.add(
                        FeedDataItem(
                            code = BREAST_MILK,
                            title = "Breast Volume",
                            value = vol.toDouble().toString(),
                            coding = false
                        )
                    )
                }
                if (cbEbm.isChecked) {
                    val vol = ebmVolume.volume.text.toString()
                    val rou = ebmRoute.appType.text.toString()
                    if (checkEmptyData(vol) || checkEmptyData(rou)) {
                        return
                    }
                    feedsList.add(
                        FeedDataItem(
                            code = EBM_VOLUME,
                            title = "EBM Volume",
                            value = vol.toDouble().toString(),
                            coding = false,
                        )
                    )

                    feedsList.add(
                        FeedDataItem(
                            code = EBM_ROUTE,
                            title = "EBM Route",
                            value = rou,
                            coding = true,
                        )
                    )
                }
                if (cbFluid.isChecked) {
                    val vol = ivVolume.volume.text.toString()
                    val rou = ivRoute.appType.text.toString()
                    if (checkEmptyData(vol)) {
                        return
                    }
                    feedsList.add(
                        FeedDataItem(
                            code = IV_VOLUME,
                            title = "IV Volume",
                            value = vol.toDouble().toString(),
                            coding = false
                        )
                    )

                }
                if (cbFormula.isChecked) {
                    val vol = formulaVolume.volume.text.toString()
                    val rou = formulaRoute.appType.text.toString()
                    val typ = formulaType.appFrequency.text.toString()

                    if (checkEmptyData(vol) || checkEmptyData(rou) || checkEmptyData(
                            typ
                        )
                    ) {
                        return@apply
                    }
                    feedsList.add(
                        FeedDataItem(
                            title = "Formula Volume",
                            code = FORMULA_VOLUME,
                            coding = false,
                            value = vol.toDouble().toString()
                        )
                    )
                    feedsList.add(
                        FeedDataItem(
                            title = "Formula Route",
                            code = FORMULA_ROUTE,
                            coding = true,
                            value = rou
                        )
                    )
                    feedsList.add(
                        FeedDataItem(
                            title = "Formula Type",
                            code = FORMULA_TYPE,
                            coding = true,
                            value = typ
                        )
                    )

                }
                if (cbDhm.isChecked) {
                    val vol = dhmVolume.volume.text.toString()
                    val rou = dhmRoute.appType.text.toString()

                    val typ = dhmType.appFrequency.text.toString()
                    val con = dhmConsent.appFrequency.text.toString()
                    val reason = dhmReason.volume.text.toString()

                    if (checkEmptyData(vol) || checkEmptyData(rou) || checkEmptyData(
                            typ
                        ) || checkEmptyData(con) || checkEmptyData(reason)
                    ) {
                        return
                    }
                    val sig = if (con.trim() == "Yes") {
                        "Signed"
                    } else {
                        "Not Signed"
                    }
                    feedsList.add(
                        FeedDataItem(
                            title = "DHM Type",
                            code = DHM_TYPE,
                            coding = true,
                            value = typ
                        )
                    )
                    feedsList.add(
                        FeedDataItem(
                            title = "DHM Volume",
                            code = DHM_VOLUME,
                            coding = false,
                            value = vol.toDouble().toString()
                        )
                    )
                    feedsList.add(
                        FeedDataItem(
                            title = "DHM Route",
                            code = DHM_ROUTE,
                            coding = true,
                            value = rou
                        )
                    )
                    feedsList.add(
                        FeedDataItem(
                            title = "Consent Given",
                            code = DHM_CONSENT,
                            coding = true,
                            value = sig
                        )
                    )
                    feedsList.add(
                        FeedDataItem(
                            title = "DHM Reason",
                            code = DHM_REASON,
                            coding = true,
                            value = reason
                        )
                    )
                }

                var totalFeedsVolume = 0f
                feedsList.forEach {
                    if (!it.coding) {
                        val quantity = it.value
                        totalFeedsVolume += quantity.toFloat()
                    }
                }
                if (totalFeedsVolume == aggregateTotal) {
                    confirmationDialog.show(childFragmentManager, "Confirm Details")
                } else {
                    if (totalFeedsVolume < aggregateTotal) {
                        Toast.makeText(
                            requireContext(),
                            "Please check Total Feeds",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Please check Feed Breakdown Volumes",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

        } else {
            Toast.makeText(
                requireContext(),
                getString(R.string.inputs_missing),
                Toast.LENGTH_SHORT
            ).show()

        }

    }

    private fun checkEmptyData(vol: String): Boolean {
        if (vol.isEmpty()) {
            Toast.makeText(
                requireContext(),
                getString(R.string.inputs_missing),
                Toast.LENGTH_SHORT
            ).show()
            return true
        }
        return false

    }

    private fun showCancelScreenerQuestionnaireAlertDialog() {


        SweetAlertDialog(activity, SweetAlertDialog.WARNING_TYPE)
            .setTitleText("Are you sure?")
            .setContentText(getString(R.string.cancel_questionnaire_message))
            .setConfirmText("Yes")
            .setConfirmClickListener { d ->
                d.dismiss()
                NavHostFragment.findNavController(this@EditPrescriptionFragment).navigateUp()
            }
            .setCancelText("No")
            .show()
    }

    private fun onBackPressed() {
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner) {
            showCancelScreenerQuestionnaireAlertDialog()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.hidden_menu, menu)
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

    companion object {
        const val QUESTIONNAIRE_FILE_PATH_KEY = "questionnaire-file-path-key"
        const val QUESTIONNAIRE_FRAGMENT_TAG = "questionnaire-fragment-tag"
    }
}