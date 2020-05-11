/********************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://www.calypsonet-asso.org/
 ********************************************************************************/
package org.eclipse.keyple.famoco.se.plugin

// Allow to provide a non ambiguous name for the reader while keeping it internal
interface AndroidFamocoReader {
    companion object {
        const val READER_NAME = "AndroidFamocoReader"
        const val FLAG_READER_RESET_STATE = "FLAG_READER_RESET_STATE"
    }
}
