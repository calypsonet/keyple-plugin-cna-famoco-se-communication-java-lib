/********************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://www.calypsonet-asso.org/
 ********************************************************************************/
package org.cna.keyple.famoco.demo.util

import android.content.Context
import android.os.Build

fun Context.getColorResource(id: Int): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        resources.getColor(id, null)
    } else {
        resources.getColor(id)
    }
}
