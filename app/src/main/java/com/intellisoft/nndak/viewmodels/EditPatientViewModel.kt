package com.intellisoft.nndak.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

import androidx.lifecycle.liveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import com.intellisoft.nndak.screens.patients.EditPatientFragment
import com.intellisoft.nndak.FhirApplication
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse

/**
 * The ViewModel helper class for [EditPatientFragment], that is responsible for preparing data for
 * UI.
 */
class EditPatientViewModel(application: Application, private val state: SavedStateHandle) :
    AndroidViewModel(application) {
    private val fhirEngine: FhirEngine = FhirApplication.fhirEngine(application.applicationContext)

    private val patientId: String = requireNotNull(state["patient_id"])
    val livePatientData = liveData { emit(prepareEditPatient()) }

    private suspend fun prepareEditPatient(): Pair<String, String> {
        val patient = fhirEngine.load(Patient::class.java, patientId)
        val question = readFileFromAssets("new-patient-registration.json").trimIndent()
        val parser = FhirContext.forR4().newJsonParser()
        val questionnaire =
            parser.parseResource(org.hl7.fhir.r4.model.Questionnaire::class.java, question) as
                    Questionnaire

        val questionnaireResponse: QuestionnaireResponse =
            ResourceMapper.populate(questionnaire, patient)
        val questionnaireResponseJson = parser.encodeResourceToString(questionnaireResponse)
        return question to questionnaireResponseJson
    }

    private val questionnaire: String
        get() = getQuestionnaireJson()
    val isPatientSaved = MutableLiveData<Boolean>()

    private val questionnaireResource: Questionnaire
        get() = FhirContext.forR4().newJsonParser().parseResource(questionnaire) as Questionnaire

    private var questionnaireJson: String? = null

    /**
     * Update patient registration questionnaire response into the application database.
     *
     * @param questionnaireResponse patient registration questionnaire response
     */
    fun updatePatient(questionnaireResponse: QuestionnaireResponse) {
        viewModelScope.launch {
            val entry =
                ResourceMapper.extract(getApplication(), questionnaireResource, questionnaireResponse)
                    .entryFirstRep
            if (entry.resource !is Patient) return@launch
            val patient = entry.resource as Patient
            if (patient.hasName() &&
                patient.name[0].hasGiven() &&
                patient.name[0].hasFamily() &&
                patient.hasBirthDate() &&
                patient.hasTelecom() &&
                patient.telecom[0].value != null
            ) {
                patient.id = patientId
                fhirEngine.update(patient)
                isPatientSaved.value = true
                return@launch
            }

            isPatientSaved.value = false
        }
    }

    private fun getQuestionnaireJson(): String {
        questionnaireJson?.let {
            return it
        }
        questionnaireJson = readFileFromAssets(state[EditPatientFragment.QUESTIONNAIRE_FILE_PATH_KEY]!!)
        return questionnaireJson!!
    }

    private fun readFileFromAssets(filename: String): String {
        return getApplication<Application>().assets.open(filename).bufferedReader().use {
            it.readText()
        }
    }
}