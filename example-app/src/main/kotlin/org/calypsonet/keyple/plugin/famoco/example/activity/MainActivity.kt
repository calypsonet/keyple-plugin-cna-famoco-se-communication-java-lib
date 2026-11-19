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
package org.calypsonet.keyple.plugin.famoco.example.activity

import android.view.MenuItem
import androidx.core.view.GravityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.calypsonet.keyple.plugin.famoco.AndroidFamocoPluginFactoryProvider
import org.calypsonet.keyple.plugin.famoco.AndroidFamocoReader
import org.calypsonet.keyple.plugin.famoco.example.R
import org.calypsonet.keyple.plugin.famoco.example.databinding.ActivityMainBinding
import org.calypsonet.keyple.plugin.famoco.example.util.CalypsoClassicInfo
import org.eclipse.keyple.card.calypso.CalypsoExtensionService
import org.eclipse.keyple.core.service.KeyplePluginException
import org.eclipse.keyple.core.service.SmartCardServiceProvider
import org.eclipse.keyple.core.util.HexUtil
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcConfig
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcConstants
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcPluginFactoryProvider
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcSupportedProtocols
import org.eclipse.keypop.calypso.card.WriteAccessLevel
import org.eclipse.keypop.calypso.card.card.CalypsoCard
import org.eclipse.keypop.calypso.card.transaction.SymmetricCryptoSecuritySetting
import org.eclipse.keypop.reader.CardReader
import org.eclipse.keypop.reader.CardReaderEvent
import org.eclipse.keypop.reader.ChannelControl
import org.eclipse.keypop.reader.ConfigurableCardReader
import org.eclipse.keypop.reader.ObservableCardReader
import org.eclipse.keypop.reader.selection.CardSelectionManager
import org.eclipse.keypop.reader.selection.ScheduledCardSelectionsResponse
import timber.log.Timber

class MainActivity : AbstractExampleActivity() {

  private companion object {
    /** Logical name of the protocol of the contactless cards */
    const val CARD_PROTOCOL = "ISO_14443_4_CARD"
  }

  private lateinit var cardReader: ObservableCardReader
  private lateinit var samReader: CardReader

  private lateinit var cardSelectionManager: CardSelectionManager

  /** Security settings of the secure transactions, created on first use (requires the SAM) */
  private var securitySettings: SymmetricCryptoSecuritySetting? = null

  private enum class TransactionType {
    DECREASE,
    INCREASE,
  }

  override fun initContentView() {
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)
    initActionBar(binding.toolbar, "Keyple demo", "Famoco Plugin")
  }

  override fun initReaders() {
    val smartCardService = SmartCardServiceProvider.getService()

    // Card reader: Android NFC reader
    val nfcPlugin =
        smartCardService.registerPlugin(
            AndroidNfcPluginFactoryProvider.provideFactory(AndroidNfcConfig(activity = this))
        )
    cardReader = nfcPlugin.getReader(AndroidNfcConstants.READER_NAME) as ObservableCardReader
    cardReader.setReaderObservationExceptionHandler { pluginName, readerName, e ->
      Timber.e(e, "An unexpected reader error occurred: %s:%s", pluginName, readerName)
    }
    cardReader.addObserver(this)
    (cardReader as ConfigurableCardReader).activateProtocol(
        AndroidNfcSupportedProtocols.ISO_14443_4.name,
        CARD_PROTOCOL,
    )

    // SAM reader: access provided by the Famoco lib-secommunication library
    samReader =
        smartCardService
            .registerPlugin(AndroidFamocoPluginFactoryProvider.getFactory())
            .getReader(AndroidFamocoReader.READER_NAME)
  }

  override fun onResume() {
    super.onResume()
    addActionEvent("Enabling NFC Reader mode")
    cardReader.startCardDetection(ObservableCardReader.DetectionMode.REPEATING)
    addResultEvent("Please choose a use case")
  }

  override fun onPause() {
    addActionEvent("Stopping card detection")
    try {
      // notify reader that card detection has been switched off
      cardReader.stopCardDetection()
    } catch (e: KeyplePluginException) {
      Timber.e(e, "NFC Plugin not found")
      addResultEvent("Error: NFC Plugin not found")
    } catch (e: Exception) {
      Timber.e(e, "NFC Plugin not found")
      addResultEvent("Error: NFC Plugin not found")
    }
    super.onPause()
  }

  override fun onDestroy() {
    cardReader.removeObserver(this)

    SmartCardServiceProvider.getService().plugins.forEach {
      SmartCardServiceProvider.getService().unregisterPlugin(it.name)
    }

    super.onDestroy()
  }

  override fun onBackPressed() {
    if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
      binding.drawerLayout.closeDrawer(GravityCompat.START)
    } else {
      super.onBackPressed()
    }
  }

  override fun onNavigationItemSelected(item: MenuItem): Boolean {
    if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
      binding.drawerLayout.closeDrawer(GravityCompat.START)
    }
    when (item.itemId) {
      R.id.usecase1 -> {
        clearEvents()
        addHeaderEvent("Running Calypso Read transaction (without SAM)")
        configureCalypsoTransaction(::runCardReadTransactionWithoutSam)
      }
      R.id.usecase2 -> {
        clearEvents()
        addHeaderEvent("Running Calypso Read transaction (with SAM)")
        configureCalypsoTransaction(::runCardReadTransactionWithSam)
      }
      R.id.usecase3 -> {
        clearEvents()
        addHeaderEvent("Running Calypso Read/Write transaction")
        configureCalypsoTransaction(::runCardReadWriteIncreaseTransaction)
      }
      R.id.usecase4 -> {
        clearEvents()
        addHeaderEvent("Running Calypso Read/Write transaction")
        configureCalypsoTransaction(::runCardReadWriteDecreaseTransaction)
      }
    }
    return true
  }

  override fun onReaderEvent(readerEvent: CardReaderEvent?) {
    addResultEvent("New ReaderEvent received : ${readerEvent?.type?.name}")
    useCase?.onEventUpdate(readerEvent)
  }

  private fun configureCalypsoTransaction(
      responseProcessor: (selectionsResponse: ScheduledCardSelectionsResponse) -> Unit
  ) {
    addActionEvent("Prepare Calypso card selection with AID: ${CalypsoClassicInfo.AID}")
    try {
      val readerApiFactory = SmartCardServiceProvider.getService().readerApiFactory

      /* Prepare a Calypso card selection, reading the environment and holder file afterwards */
      cardSelectionManager = readerApiFactory.createCardSelectionManager()
      cardSelectionManager.prepareSelection(
          readerApiFactory
              .createIsoCardSelector()
              .filterByCardProtocol(CARD_PROTOCOL)
              .filterByDfName(CalypsoClassicInfo.AID),
          CalypsoExtensionService.getInstance()
              .calypsoCardApiFactory
              .createCalypsoCardSelectionExtension()
              .prepareReadRecord(
                  CalypsoClassicInfo.SFI_EnvironmentAndHolder,
                  CalypsoClassicInfo.RECORD_NUMBER_1.toInt(),
              ),
      )

      /* Provide the reader with the selection operation to be processed when a card is inserted */
      cardSelectionManager.scheduleCardSelectionScenario(
          cardReader,
          ObservableCardReader.NotificationMode.MATCHED_ONLY,
      )

      useCase =
          object : UseCase {
            override fun onEventUpdate(event: CardReaderEvent?) {
              CoroutineScope(Dispatchers.Main).launch {
                when (event?.type) {
                  CardReaderEvent.Type.CARD_MATCHED -> {
                    addResultEvent("Card detected with AID: ${CalypsoClassicInfo.AID}")
                    responseProcessor(event.scheduledCardSelectionsResponse)
                    cardReader.finalizeCardProcessing()
                  }
                  CardReaderEvent.Type.CARD_INSERTED -> {
                    addResultEvent(
                        "Card detected but AID didn't match with ${CalypsoClassicInfo.AID}"
                    )
                    cardReader.finalizeCardProcessing()
                  }
                  CardReaderEvent.Type.CARD_REMOVED -> {
                    addResultEvent("Card removed")
                  }
                  else -> {
                    // Do nothing
                  }
                }
              }
            }
          }

      // notify reader that card detection has been launched
      cardReader.startCardDetection(ObservableCardReader.DetectionMode.REPEATING)
      addActionEvent("Waiting for card presentation")
    } catch (e: KeyplePluginException) {
      Timber.e(e)
      addResultEvent("Exception: ${e.message}")
    } catch (e: Exception) {
      Timber.e(e)
      addResultEvent("Exception: ${e.message}")
    }
  }

  /** Returns the security settings, created on first use with the SAM of the Famoco reader. */
  private fun getSecuritySettings(): SymmetricCryptoSecuritySetting =
      securitySettings ?: createSecuritySettings(samReader).also { securitySettings = it }

  /** Returns the Calypso card selected, or null if the selection has failed. */
  private fun parseSelection(selectionsResponse: ScheduledCardSelectionsResponse): CalypsoCard? =
      cardSelectionManager.parseScheduledCardSelectionsResponse(selectionsResponse).activeSmartCard
          as? CalypsoCard

  private fun runCardReadTransactionWithSam(selectionsResponse: ScheduledCardSelectionsResponse) {
    runCardReadTransaction(selectionsResponse, true)
  }

  private fun runCardReadTransactionWithoutSam(
      selectionsResponse: ScheduledCardSelectionsResponse
  ) {
    runCardReadTransaction(selectionsResponse, false)
  }

  private fun runCardReadTransaction(
      selectionsResponse: ScheduledCardSelectionsResponse,
      withSam: Boolean,
  ) {
    try {
      /*
       * print tag info in View
       */
      addActionEvent("Process selection")
      val calypsoCard = parseSelection(selectionsResponse)

      if (calypsoCard != null) {
        addResultEvent("Selection successful")

        /*
         * Retrieve the data read during the selection process
         */
        addActionEvent("Read environment and holder data")
        addResultEvent(
            "Environment and Holder file: ${
              HexUtil.toHex(
                calypsoCard.getFileBySfi(CalypsoClassicInfo.SFI_EnvironmentAndHolder).data.content
              )
            }"
        )

        addHeaderEvent("2nd card exchange: read the event log file")

        val calypsoCardApiFactory = CalypsoExtensionService.getInstance().calypsoCardApiFactory
        if (withSam) {
          addActionEvent("Create card secured transaction with SAM")
          val cardTransaction =
              calypsoCardApiFactory.createSecureRegularModeTransactionManager(
                  cardReader,
                  calypsoCard,
                  getSecuritySettings(),
              )

          /*
           * Actual card communication: open a secure session, read the counter and the event log
           * files, then close the session and the channel with the card
           */
          addActionEvent("Process card secure session for counter and event log reading")
          cardTransaction
              .prepareOpenSecureSession(WriteAccessLevel.LOAD)
              .prepareReadRecords(
                  CalypsoClassicInfo.SFI_EventLog,
                  CalypsoClassicInfo.RECORD_NUMBER_1.toInt(),
                  CalypsoClassicInfo.RECORD_NUMBER_1.toInt(),
                  CalypsoClassicInfo.RECORD_SIZE,
              )
              .prepareReadRecords(
                  CalypsoClassicInfo.SFI_Counter1,
                  CalypsoClassicInfo.RECORD_NUMBER_1.toInt(),
                  CalypsoClassicInfo.RECORD_NUMBER_1.toInt(),
                  CalypsoClassicInfo.RECORD_SIZE,
              )
              .prepareCloseSecureSession()
              .processCommands(ChannelControl.CLOSE_AFTER)
          addResultEvent("Secure session: SUCCESS")

          // In secured reading, value read elements can only be trusted if the session is closed
          // without error.
          addResultEvent("Counter value: ${readCounter(calypsoCard)}")
          addResultEvent("EventLog file: ${HexUtil.toHex(readEventLog(calypsoCard))}")
        } else {
          // Create card unsecured transaction
          addActionEvent("Process card command for counter and event log reading")
          calypsoCardApiFactory
              .createFreeTransactionManager(cardReader, calypsoCard)
              .prepareReadRecords(
                  CalypsoClassicInfo.SFI_EventLog,
                  CalypsoClassicInfo.RECORD_NUMBER_1.toInt(),
                  CalypsoClassicInfo.RECORD_NUMBER_1.toInt(),
                  CalypsoClassicInfo.RECORD_SIZE,
              )
              .prepareReadRecords(
                  CalypsoClassicInfo.SFI_Counter1,
                  CalypsoClassicInfo.RECORD_NUMBER_1.toInt(),
                  CalypsoClassicInfo.RECORD_NUMBER_1.toInt(),
                  CalypsoClassicInfo.RECORD_SIZE,
              )
              .processCommands(ChannelControl.CLOSE_AFTER)
          addResultEvent("Counter value: ${readCounter(calypsoCard)}")
          addResultEvent("EventLog file: ${HexUtil.toHex(readEventLog(calypsoCard))}")
        }

        addResultEvent("End of the Calypso card processing.")
        addResultEvent("You can remove the card now")
      } else {
        addResultEvent(
            "The selection of the card has failed. Should not have occurred due to the MATCHED_ONLY selection mode."
        )
      }
    } catch (e: KeyplePluginException) {
      Timber.e(e)
      addResultEvent("Exception: ${e.message}")
    } catch (e: Exception) {
      Timber.e(e)
      addResultEvent("Exception: ${e.message}")
    }
  }

  private fun readCounter(calypsoCard: CalypsoCard): Int? =
      calypsoCard
          .getFileBySfi(CalypsoClassicInfo.SFI_Counter1)
          .data
          .getContentAsCounterValue(CalypsoClassicInfo.RECORD_NUMBER_1.toInt())

  private fun readEventLog(calypsoCard: CalypsoCard): ByteArray? =
      calypsoCard.getFileBySfi(CalypsoClassicInfo.SFI_EventLog).data.content

  private fun runCardReadWriteIncreaseTransaction(
      selectionsResponse: ScheduledCardSelectionsResponse
  ) {
    runCardReadWriteTransaction(selectionsResponse, TransactionType.INCREASE)
  }

  private fun runCardReadWriteDecreaseTransaction(
      selectionsResponse: ScheduledCardSelectionsResponse
  ) {
    runCardReadWriteTransaction(selectionsResponse, TransactionType.DECREASE)
  }

  private fun runCardReadWriteTransaction(
      selectionsResponse: ScheduledCardSelectionsResponse,
      transactionType: TransactionType,
  ) {
    try {
      addActionEvent("1st card exchange: aid selection")
      val calypsoCard = parseSelection(selectionsResponse)

      if (calypsoCard != null) {
        addResultEvent("Calypso card selection: SUCCESS")
        addResultEvent("AID: ${CalypsoClassicInfo.AID}")

        // Create card secured transaction
        val cardTransaction =
            CalypsoExtensionService.getInstance()
                .calypsoCardApiFactory
                .createSecureRegularModeTransactionManager(
                    cardReader,
                    calypsoCard,
                    getSecuritySettings(),
                )

        when (transactionType) {
          TransactionType.INCREASE -> {
            /*
             * Open a secure session for the load key, read the counter, increase it, then close
             * the session
             */
            addActionEvent("Process card secure session: increase counter by 10")
            cardTransaction
                .prepareOpenSecureSession(WriteAccessLevel.LOAD)
                .prepareReadRecords(
                    CalypsoClassicInfo.SFI_Counter1,
                    CalypsoClassicInfo.RECORD_NUMBER_1.toInt(),
                    CalypsoClassicInfo.RECORD_NUMBER_1.toInt(),
                    CalypsoClassicInfo.RECORD_SIZE,
                )
                .prepareIncreaseCounter(
                    CalypsoClassicInfo.SFI_Counter1,
                    CalypsoClassicInfo.RECORD_NUMBER_1.toInt(),
                    10,
                )
                .prepareCloseSecureSession()
                .processCommands(ChannelControl.CLOSE_AFTER)
            addResultEvent("Increase by 10: SUCCESS")
          }
          TransactionType.DECREASE -> {
            /*
             * Open a secure session for the debit key, read the counter, decrease it, then close
             * the session (a ratification command is sent in contactless mode)
             */
            addActionEvent("Process card secure session: decrease counter by 1")
            cardTransaction
                .prepareOpenSecureSession(WriteAccessLevel.DEBIT)
                .prepareReadRecords(
                    CalypsoClassicInfo.SFI_Counter1,
                    CalypsoClassicInfo.RECORD_NUMBER_1.toInt(),
                    CalypsoClassicInfo.RECORD_NUMBER_1.toInt(),
                    CalypsoClassicInfo.RECORD_SIZE,
                )
                .prepareDecreaseCounter(
                    CalypsoClassicInfo.SFI_Counter1,
                    CalypsoClassicInfo.RECORD_NUMBER_1.toInt(),
                    1,
                )
                .prepareCloseSecureSession()
                .processCommands(ChannelControl.CLOSE_AFTER)
            addResultEvent("Decrease by 1: SUCCESS")
          }
        }
        addResultEvent("Counter value: ${readCounter(calypsoCard)}")

        addResultEvent("End of the Calypso card processing.")
        addResultEvent("You can remove the card now")
      } else {
        addResultEvent(
            "The selection of the card has failed. Should not have occurred due to the MATCHED_ONLY selection mode."
        )
      }
    } catch (e: KeyplePluginException) {
      Timber.e(e)
      addResultEvent("Exception: ${e.message}")
    } catch (e: Exception) {
      Timber.e(e)
      addResultEvent("Exception: ${e.message}")
    }
  }
}
