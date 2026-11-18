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

import com.famoco.secommunication.ALPARProtocol
import com.famoco.secommunication.SmartcardReader
import org.calypsonet.keyple.plugin.famoco.AndroidFamocoReader.Companion.READER_NAME
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi
import org.eclipse.keyple.core.util.HexUtil
import org.slf4j.LoggerFactory

internal class AndroidFamocoReaderAdapter : ReaderSpi, AndroidFamocoReader {

  private companion object {
    private val logger = LoggerFactory.getLogger(AndroidFamocoReaderAdapter::class.java)
  }

  private val parameters: MutableMap<String, String> = HashMap()

  private val mSmarcardReader: SmartcardReader = SmartcardReader.getInstance()
  private var poweredOn = false
  private var atr: ByteArray? = null

  init {
    logger.info("Initialize Famoco reader: {}", READER_NAME)
    SmartcardReader.setDebuggingEnabled(true)
    mSmarcardReader.openReader(115200)
    if (logger.isDebugEnabled) {
      logger.debug("firmwareVersion = {}", mSmarcardReader.firmwareVersion)
    }
    mSmarcardReader.isAutoNegotiate = true
  }

  override fun transmitApdu(apduIn: ByteArray?): ByteArray {
    if (logger.isDebugEnabled) {
      logger.debug("Data Length to be sent to tag : {}", apduIn?.size)
      logger.debug("Data In : {}", HexUtil.toHex(apduIn))
    }
    val apduOut = mSmarcardReader.sendApdu(apduIn)
    if (logger.isDebugEnabled) {
      logger.debug("Data Out : {}", HexUtil.toHex(apduOut))
    }
    return apduOut
  }

  override fun getPowerOnData(): String {
    if (logger.isDebugEnabled) {
      logger.debug("getAtr()")
      logger.debug("ATR = {}", HexUtil.toHex(atr))
    }
    return HexUtil.toHex(atr)
  }

  override fun openPhysicalChannel() {
    if (logger.isDebugEnabled) {
      logger.debug("openPhysicalChannel()")
    }
    atr = mSmarcardReader.powerOn()
    if (logger.isDebugEnabled) {
      logger.debug("ATR = {}", HexUtil.toHex(atr))
    }
    mSmarcardReader.setClockCard(ALPARProtocol.PARAM_CLOCK_FREQUENCY_3_68MHz)
    poweredOn = true
  }

  override fun isPhysicalChannelOpen(): Boolean {
    if (logger.isDebugEnabled) {
      logger.debug("isPhysicalChannelOpen()")
    }
    return poweredOn
  }

  override fun checkCardPresence(): Boolean {
    if (logger.isDebugEnabled) {
      logger.debug("checkSePresence()")
    }
    return mSmarcardReader.isCardPresent
  }

  override fun closePhysicalChannel() {
    if (logger.isDebugEnabled) {
      logger.debug("closePhysicalChannel()")
    }
    mSmarcardReader.powerOff()
    poweredOn = false
  }

  override fun isContactless(): Boolean {
    return false
  }

  override fun getName(): String = READER_NAME

  override fun onUnregister() {
    // Do nothing
  }
}
