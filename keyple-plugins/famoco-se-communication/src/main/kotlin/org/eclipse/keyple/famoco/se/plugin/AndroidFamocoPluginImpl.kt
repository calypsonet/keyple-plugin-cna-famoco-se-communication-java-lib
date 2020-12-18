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

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import org.eclipse.keyple.core.plugin.AbstractPlugin
import org.eclipse.keyple.core.service.Reader
import timber.log.Timber

internal object AndroidFamocoPluginImpl : AbstractPlugin(AndroidFamocoPlugin.PLUGIN_NAME), AndroidFamocoPlugin {

    override fun initNativeReaders(): ConcurrentMap<String, Reader>? {
        Timber.d("InitNativeReader() add the unique instance of AndroidFamocoReader")

        val readers = ConcurrentHashMap<String, Reader>()
        readers[AndroidFamocoReaderImpl.name] = AndroidFamocoReaderImpl
        return readers
    }
}
