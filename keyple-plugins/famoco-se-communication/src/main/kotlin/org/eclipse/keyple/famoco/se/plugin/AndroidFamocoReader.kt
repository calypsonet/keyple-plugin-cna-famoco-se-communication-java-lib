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

// Allow to provide a non ambiguous name for the reader while keeping it internal
interface AndroidFamocoReader {
    companion object {
        const val READER_NAME = "AndroidFamocoReader"
        const val FLAG_READER_RESET_STATE = "FLAG_READER_RESET_STATE"
    }
}
