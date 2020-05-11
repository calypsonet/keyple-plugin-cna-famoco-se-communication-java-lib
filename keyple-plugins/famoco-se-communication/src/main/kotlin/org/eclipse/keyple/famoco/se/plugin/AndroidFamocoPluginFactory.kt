/********************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://www.calypsonet-asso.org/
 ********************************************************************************/
package org.eclipse.keyple.famoco.se.plugin

import org.eclipse.keyple.core.seproxy.AbstractPluginFactory
import org.eclipse.keyple.core.seproxy.ReaderPlugin

class AndroidFamocoPluginFactory : AbstractPluginFactory() {

    override fun getPluginName(): String {
        return AndroidFamocoPluginImpl.name
    }

    override fun getPluginInstance(): ReaderPlugin {
        return AndroidFamocoPluginImpl
    }
}
