/********************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://www.calypsonet-asso.org/
 */
package org.cna.keyple.famoco.validator.ticketing

import org.eclipse.keyple.core.util.ByteArrayUtil
import java.util.*

class CardContent {
    var serialNumber: ByteArray? = null
    var poRevision: String? = null
    var poTypeName: String? = null
    var extraInfo: String? = null
    var icc: SortedMap<Int, ByteArray>
    var id: SortedMap<Int, ByteArray>
    var environment: SortedMap<Int, ByteArray>
    var eventLog: SortedMap<Int, ByteArray>
    var specialEvents: SortedMap<Int, ByteArray>
    var contractsList: SortedMap<Int, ByteArray>
    var odMemory: SortedMap<Int, ByteArray>
    var contracts: SortedMap<Int, ByteArray>
    var counters: SortedMap<Int, Int>

    override fun toString(): String {
        return ("SN : " + (if (serialNumber != null) ByteArrayUtil.toHex(serialNumber) else "null")
                + "- PoTypeName:" + poTypeName + " - Ticket available:"
                + (if (counters.size > 0) counters[1] else "empty") + " - Contracts available : "
                + if (contracts.size > 0) String(contracts[1]!!) else "empty")
    }

    init {
        counters = TreeMap()
        contracts = TreeMap()
        odMemory = TreeMap()
        contractsList = TreeMap()
        specialEvents = TreeMap()
        eventLog = TreeMap()
        environment = TreeMap()
        id = TreeMap()
        icc = TreeMap()
    }
}