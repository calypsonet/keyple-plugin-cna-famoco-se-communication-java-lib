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
package org.calypsonet.keyple.plugin.famoco.example.util

import android.content.Context
import android.os.Build

fun Context.getColorResource(id: Int): Int {
  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    resources.getColor(id, null)
  } else {
    resources.getColor(id)
  }
}
