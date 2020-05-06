/********************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://www.calypsonet-asso.org/
 ********************************************************************************/
package org.eclipse.keyple.famoco.se.plugin

import com.famoco.secommunication.SmartcardReader
import io.mockk.MockKAnnotations
import io.mockk.mockk
import io.mockk.unmockkAll
import org.eclipse.keyple.core.seproxy.protocol.TransmissionMode
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

internal class AndroidFamocoReaderImplTest {

    companion object {
        internal const val PLUGIN_NAME = "AndroidFamocoPlugin"
        internal const val PO_AID = "A000000291A000000191"
        internal const val PO_AID_RESPONSE = "6F25840BA000000291A00000019102A516BF0C13C70800000000C0E11FA653070A3C230C1410019000"
    }

    lateinit var nativeReader: SmartcardReader
    lateinit var reader: AndroidFamocoReaderImpl

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        // default reader connected with secure element with poAid
        nativeReader = mockReader()

        // instantiate reader with nativeReader
        reader = AndroidFamocoReader
    }

    @After
    fun tearDown() {
        unmockkAll()
    }
    /*
     * TEST READERS
     */

    @Test
    fun getInstance() {
        Assert.assertNotNull(reader)
    }

    @Test
    fun getName() {
        Assert.assertEquals(getNativeReaderName(), reader.name)
    }

    @Test
    fun getTransmissionMode() {
        Assert.assertEquals(TransmissionMode.CONTACTS, reader.transmissionMode)
    }

    @Test
    fun isSEPresent() {
        Assert.assertEquals(true, reader.isSePresent)
    }

    @Test
    fun getParameters() {
        Assert.assertNotNull(reader.parameters)
    }

    @Test
    fun setParameters() {
        val parameters = HashMap<String, String>()
        parameters["key1"] = "value1"
        reader.parameters = parameters
        Assert.assertTrue(reader.parameters.size == 1)
        Assert.assertTrue(reader.parameters["key1"] == "value1")
    }

    @Test
    fun setParameter() {
        reader.setParameter("key2", "value2")
        Assert.assertTrue(reader.parameters.size == 1)
        Assert.assertTrue(reader.parameters["key2"] == "value2")
    }

    fun getNativeReaderName(): String {
        return reader.name
    }

    fun mockReader(): SmartcardReader {
        val nativeReader = mockk<SmartcardReader>()
        return nativeReader
    }
}
