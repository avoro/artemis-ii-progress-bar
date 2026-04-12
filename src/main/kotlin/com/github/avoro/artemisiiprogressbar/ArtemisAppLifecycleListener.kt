package com.github.avoro.artemisiiprogressbar

import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.application.ApplicationActivationListener
import com.intellij.openapi.wm.IdeFrame
import javax.swing.UIManager

class ArtemisProgressBarListener : LafManagerListener, ApplicationActivationListener {

    init {
        updateProgressBarUI()
    }

    override fun lookAndFeelChanged(source: LafManager) {
        updateProgressBarUI()
    }

    override fun applicationActivated(ideFrame: IdeFrame) {
        updateProgressBarUI()
    }

    private fun updateProgressBarUI() {
        UIManager.put("ProgressBarUI", ArtemisProgressBarUI::class.java.name)
        UIManager.getDefaults().put(ArtemisProgressBarUI::class.java.name, ArtemisProgressBarUI::class.java)
    }
}
