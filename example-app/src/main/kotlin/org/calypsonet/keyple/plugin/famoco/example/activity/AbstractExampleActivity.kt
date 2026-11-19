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

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.calypsonet.keyple.plugin.famoco.example.R
import org.calypsonet.keyple.plugin.famoco.example.adapter.EventAdapter
import org.calypsonet.keyple.plugin.famoco.example.databinding.ActivityMainBinding
import org.calypsonet.keyple.plugin.famoco.example.model.ChoiceEventModel
import org.calypsonet.keyple.plugin.famoco.example.model.EventModel
import org.eclipse.keyple.card.calypso.CalypsoExtensionService
import org.eclipse.keyple.card.calypso.crypto.legacysam.LegacySamExtensionService
import org.eclipse.keyple.card.calypso.crypto.legacysam.LegacySamUtil
import org.eclipse.keyple.core.service.SmartCardServiceProvider
import org.eclipse.keypop.calypso.card.WriteAccessLevel
import org.eclipse.keypop.calypso.card.transaction.SymmetricCryptoSecuritySetting
import org.eclipse.keypop.calypso.crypto.legacysam.sam.LegacySam
import org.eclipse.keypop.reader.CardReader
import org.eclipse.keypop.reader.CardReaderEvent
import org.eclipse.keypop.reader.spi.CardReaderObserverSpi
import timber.log.Timber

abstract class AbstractExampleActivity :
    AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, CardReaderObserverSpi {

  private companion object {
    // The default KIF values for personalization, loading and debiting
    const val DEFAULT_KIF_PERSO: Byte = 0x21
    const val DEFAULT_KIF_LOAD: Byte = 0x27
    const val DEFAULT_KIF_DEBIT: Byte = 0x30
  }

  /** Use to modify event update behaviour regarding current use case execution */
  interface UseCase {
    fun onEventUpdate(event: CardReaderEvent?)
  }

  /** Variables for event window */
  private lateinit var adapter: RecyclerView.Adapter<*>
  private lateinit var layoutManager: RecyclerView.LayoutManager
  private val events = arrayListOf<EventModel>()

  protected var useCase: UseCase? = null

  /** Views of the activity layout, set by [initContentView] */
  protected lateinit var binding: ActivityMainBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    initContentView()

    /** Init recycler view */
    adapter = EventAdapter(events)
    layoutManager = LinearLayoutManager(this)
    binding.eventRecyclerView.layoutManager = layoutManager
    binding.eventRecyclerView.adapter = adapter

    /** Init menu */
    binding.navigationView.setNavigationItemSelectedListener(this)
    val toggle =
        ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.open_navigation_drawer,
            R.string.close_navigation_drawer,
        )
    binding.drawerLayout.addDrawerListener(toggle)
    toggle.syncState()

    initReaders()

    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
  }

  protected fun initActionBar(toolbar: Toolbar, title: String, subtitle: String) {
    setSupportActionBar(toolbar)
    val actionBar = supportActionBar
    actionBar?.title = title
    actionBar?.subtitle = subtitle
  }

  protected fun showAlertDialog(t: Throwable) {
    val builder = AlertDialog.Builder(this@AbstractExampleActivity)
    builder.setTitle(R.string.alert_dialog_title)
    builder.setMessage(getString(R.string.alert_dialog_message, t.message))
    val dialog = builder.create()
    dialog.show()
  }

  protected fun clearEvents() {
    events.clear()
    adapter.notifyDataSetChanged()
  }

  protected fun addHeaderEvent(message: String) {
    events.add(EventModel(EventModel.TYPE_HEADER, message))
    updateList()
    Timber.d("Header: %s", message)
  }

  protected fun addActionEvent(message: String) {
    events.add(EventModel(EventModel.TYPE_ACTION, message))
    updateList()
    Timber.d("Action: %s", message)
  }

  protected fun addResultEvent(message: String) {
    events.add(EventModel(EventModel.TYPE_RESULT, message))
    updateList()
    Timber.d("Result: %s", message)
  }

  protected fun addChoiceEvent(
      title: String,
      choices: List<String>,
      callback: (choice: String) -> Unit,
  ) {
    events.add(ChoiceEventModel(title, choices, callback))
    updateList()
    Timber.d("Choice: %s: %s", title, choices.toString())
  }

  private fun updateList() {
    CoroutineScope(Dispatchers.Main).launch {
      adapter.notifyDataSetChanged()
      adapter.notifyItemInserted(events.lastIndex)
      binding.eventRecyclerView.smoothScrollToPosition(events.size - 1)
    }
  }

  abstract fun initContentView()

  abstract fun initReaders()

  /**
   * Selects the Calypso SAM C1 inserted in the provided reader, and creates the security settings
   * of the secure card transactions using it, with the default KIFs for personalization, loading
   * and debiting.
   *
   * @param samReader The reader in which the SAM is inserted.
   * @return The security settings.
   * @throws IllegalStateException If no SAM C1 is found in the reader.
   */
  protected fun createSecuritySettings(samReader: CardReader): SymmetricCryptoSecuritySetting {
    val readerApiFactory = SmartCardServiceProvider.getService().readerApiFactory
    val legacySamApiFactory = LegacySamExtensionService.getInstance().legacySamApiFactory

    // Select the SAM C1 using its power-on data
    val samSelectionManager = readerApiFactory.createCardSelectionManager()
    samSelectionManager.prepareSelection(
        readerApiFactory
            .createBasicCardSelector()
            .filterByPowerOnData(
                LegacySamUtil.buildPowerOnDataFilter(LegacySam.ProductType.SAM_C1, null)
            ),
        legacySamApiFactory.createLegacySamSelectionExtension(),
    )
    val sam =
        samSelectionManager.processCardSelectionScenario(samReader).activeSmartCard as? LegacySam
            ?: throw IllegalStateException("No SAM C1 found in the reader '${samReader.name}'")

    // Create the security settings, enabling the multiple session mode
    return CalypsoExtensionService.getInstance()
        .calypsoCardApiFactory
        .createSymmetricCryptoSecuritySetting(
            legacySamApiFactory.createSymmetricCryptoCardTransactionManagerFactory(samReader, sam)
        )
        .assignDefaultKif(WriteAccessLevel.PERSONALIZATION, DEFAULT_KIF_PERSO)
        .assignDefaultKif(WriteAccessLevel.LOAD, DEFAULT_KIF_LOAD)
        .assignDefaultKif(WriteAccessLevel.DEBIT, DEFAULT_KIF_DEBIT)
        .enableMultipleSession()
  }
}
