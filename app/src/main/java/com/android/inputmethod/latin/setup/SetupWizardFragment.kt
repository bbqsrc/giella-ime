/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.inputmethod.latin.setup

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment

import com.android.inputmethod.latin.R
import com.android.inputmethod.latin.settings.SettingsActivity
import com.android.inputmethod.latin.utils.UncachedInputMethodManagerUtils
import kotlinx.android.synthetic.main.fragment_setup_wizard.view.*
import no.divvun.navigate

class SetupWizardFragment : Fragment() {
    private lateinit var imm: InputMethodManager

    private var imeSettingTriggered = false

    private val windowFocusListener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
        if(hasFocus) {
            val step = determineSetupStep()
            updateSetupStep(step)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_setup_wizard, container, false)

        imm = activity!!.getSystemService<InputMethodManager>()!!

        view.step1.setOnClickListener {
            invokeLanguageAndInputSettings()
        }

        view.step2.setOnClickListener {
            invokeInputMethodPicker()
        }

        view.setup_wizard_next.setOnClickListener {
            navigate(R.id.action_fragment_setup_wizard_to_fragment_setup_complete)
        }

        return view
    }

    internal fun invokeSetupWizardOfThisIme() {
        val intent = Intent()
        intent.setClass(activity!!, SetupWizardFragment::class.java)
        intent.flags = (Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                or Intent.FLAG_ACTIVITY_SINGLE_TOP
                or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    private fun invokeSettingsOfThisIme() {
        val intent = Intent()
        intent.setClass(activity!!, SettingsActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.putExtra(SettingsActivity.EXTRA_ENTRY_KEY,
                SettingsActivity.EXTRA_ENTRY_VALUE_APP_ICON)
        startActivity(intent)
    }

    private fun invokeLanguageAndInputSettings() {
        val intent = Intent()
        intent.action = Settings.ACTION_INPUT_METHOD_SETTINGS
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        startActivity(intent)
    }

    private fun invokeInputMethodPicker() {
        // Invoke input method picker.
        imm.showInputMethodPicker()
    }

    // TODO(rawa) Is this needed? Will subType be enabled?
    private fun invokeSubtypeEnablerOfThisIme() {
        val imi = UncachedInputMethodManagerUtils.getInputMethodInfoOf(activity!!.packageName, imm)
                ?: return
        val intent = Intent()
        intent.action = Settings.ACTION_INPUT_METHOD_SUBTYPE_SETTINGS
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.putExtra(Settings.EXTRA_INPUT_METHOD_ID, imi.id)
        startActivity(intent)
    }

    private fun determineSetupStep(): SetupStep {
        if (!UncachedInputMethodManagerUtils.isThisImeEnabled(activity!!, imm)) {
            return SetupStep.StepIME
        }
        return if (!UncachedInputMethodManagerUtils.isThisImeCurrent(activity!!, imm)) {
            SetupStep.StepSelectInput
        } else SetupStep.StepComplete
    }

    override fun onResume() {
        super.onResume()

        // Listener for changes on WindowFocusChanged (return from change of InputMethod
        view!!.viewTreeObserver.addOnWindowFocusChangeListener(windowFocusListener)

        val step = determineSetupStep()
        if(step is SetupStep.StepComplete){
            if(imeSettingTriggered){
                activity!!.finish()
                return
            } else {
                imeSettingTriggered = true
                invokeSettingsOfThisIme()
            }
        }
        updateSetupStep(step)
    }

    override fun onPause() {
        super.onPause()
        view!!.viewTreeObserver.removeOnWindowFocusChangeListener(windowFocusListener)
    }

    private fun updateSetupStep(step: SetupStep) =
        when(step){
            SetupStep.StepIME -> {
                view?.step1?.isEnabled = true
                view?.step2?.isEnabled = false
            }
            SetupStep.StepSelectInput -> {
                view?.step1?.isEnabled = false
                view?.step2?.isEnabled = true
            }
            SetupStep.StepComplete -> {
                view?.step1?.isEnabled = false
                view?.step2?.isEnabled = false
                navigate(R.id.action_fragment_setup_wizard_to_fragment_setup_complete)
            }
            SetupStep.StepSubType -> {}
        }

    sealed class SetupStep {
        object StepIME: SetupStep()
        object StepSelectInput: SetupStep()
        object StepSubType: SetupStep()
        object StepComplete: SetupStep()
    }
}