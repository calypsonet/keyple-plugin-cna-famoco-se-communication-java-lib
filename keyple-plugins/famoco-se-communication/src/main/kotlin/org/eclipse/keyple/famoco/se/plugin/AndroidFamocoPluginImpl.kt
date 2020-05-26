/********************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information regarding copyright
 * ownership.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
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
