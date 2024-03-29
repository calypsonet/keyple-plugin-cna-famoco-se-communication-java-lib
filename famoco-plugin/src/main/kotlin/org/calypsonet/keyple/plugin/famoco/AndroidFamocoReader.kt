/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.calypsonet.keyple.plugin.famoco

import org.eclipse.keyple.core.common.KeypleReaderExtension

// Allow to provide a non ambiguous name for the reader while keeping it internal
interface AndroidFamocoReader : KeypleReaderExtension {
  companion object {
    const val READER_NAME = "AndroidFamocoReader"
    const val FLAG_READER_RESET_STATE = "FLAG_READER_RESET_STATE"
  }
}
