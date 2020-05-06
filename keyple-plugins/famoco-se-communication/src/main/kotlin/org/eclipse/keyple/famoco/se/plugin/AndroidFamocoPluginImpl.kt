/********************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://www.calypsonet-asso.org/
 ********************************************************************************/
package org.eclipse.keyple.famoco.se.plugin

import java.util.HashMap
import java.util.SortedSet
import java.util.TreeSet
import org.eclipse.keyple.core.seproxy.SeReader
import org.eclipse.keyple.core.seproxy.plugin.AbstractPlugin
import timber.log.Timber

internal object AndroidFamocoPluginImpl : AbstractPlugin(AndroidFamocoPlugin.PLUGIN_NAME), AndroidFamocoPlugin {

    private val parameters = HashMap<String, String>() // not in use in

    override fun getParameters(): MutableMap<String, String> {
        Timber.w("Android Famoco NFC Plugin does not support parameters, see AndroidNfcReader instead")
        return parameters
    }

    override fun setParameter(key: String, value: String) {
        Timber.w("Android Famoco NFC Plugin does not support parameters, see AndroidNfcReader instead")
        parameters[key] = value
    }

    override fun initNativeReaders(): SortedSet<SeReader> {
        Timber.d("InitNativeReader() add the unique instance of AndroidFamocoReader")

        val readers = TreeSet<SeReader>()
        readers.add(AndroidFamocoReaderImpl)
        return readers
    }
}
