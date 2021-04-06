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
package org.cna.keyple.famoco.example.activity

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.activity_main.drawerLayout
import kotlinx.android.synthetic.main.activity_main.eventRecyclerView
import kotlinx.android.synthetic.main.activity_main.navigationView
import kotlinx.android.synthetic.main.activity_main.toolbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.calypsonet.terminal.calypso.WriteAccessLevel
import org.calypsonet.terminal.calypso.sam.CalypsoSam
import org.calypsonet.terminal.calypso.transaction.CardSecuritySetting
import org.calypsonet.terminal.reader.CardReaderEvent
import org.calypsonet.terminal.reader.spi.CardReaderObservationExceptionHandlerSpi
import org.calypsonet.terminal.reader.spi.CardReaderObserverSpi
import org.cna.keyple.famoco.example.R
import org.cna.keyple.famoco.example.adapter.EventAdapter
import org.cna.keyple.famoco.example.model.ChoiceEventModel
import org.cna.keyple.famoco.example.model.EventModel
import org.cna.keyple.famoco.example.util.CalypsoClassicInfo.SAM_PROFILE_NAME
import org.eclipse.keyple.card.calypso.CalypsoExtensionService
import org.eclipse.keyple.core.service.Plugin
import org.eclipse.keyple.core.service.Reader
import org.eclipse.keyple.core.service.resource.CardResourceProfileConfigurator
import org.eclipse.keyple.core.service.resource.CardResourceService
import org.eclipse.keyple.core.service.resource.CardResourceServiceProvider
import org.eclipse.keyple.core.service.resource.PluginsConfigurator
import org.eclipse.keyple.core.service.resource.spi.ReaderConfiguratorSpi
import org.eclipse.keyple.core.service.spi.PluginObservationExceptionHandlerSpi
import org.eclipse.keyple.famoco.se.plugin.AndroidFamocoReader
import timber.log.Timber

abstract class AbstractExampleActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    CardReaderObserverSpi {

    /**
     * Use to modify event update behaviour reguarding current use case execution
     */
    interface UseCase {
        fun onEventUpdate(event: CardReaderEvent?)
    }

    /**
     * Variables for event window
     */
    private lateinit var adapter: RecyclerView.Adapter<*>
    private lateinit var layoutManager: RecyclerView.LayoutManager
    protected val events = arrayListOf<EventModel>()

    protected var useCase: UseCase? = null

    protected lateinit var calypsoCardExtensionProvider: CalypsoExtensionService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initContentView()

        /**
         * Init recycler view
         */
        adapter = EventAdapter(events)
        layoutManager = LinearLayoutManager(this)
        eventRecyclerView.layoutManager = layoutManager
        eventRecyclerView.adapter = adapter

        /**
         * Init menu
         */
        navigationView.setNavigationItemSelectedListener(this)
        val toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open_navigation_drawer, R.string.close_navigation_drawer)
        drawerLayout.addDrawerListener(toggle)
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

    protected fun addChoiceEvent(title: String, choices: List<String>, callback: (choice: String) -> Unit) {
        events.add(ChoiceEventModel(title, choices, callback))
        updateList()
        Timber.d("Choice: %s: %s", title, choices.toString())
    }

    private fun updateList() {
        CoroutineScope(Dispatchers.Main).launch {
            adapter.notifyDataSetChanged()
            adapter.notifyItemInserted(events.lastIndex)
            eventRecyclerView.smoothScrollToPosition(events.size - 1)
        }
    }
    abstract fun initContentView()
    abstract fun initReaders()


    /**
     * Setup the [CardResourceService] to provide a Calypso SAM C1 resource when requested.
     *
     * @param plugin The plugin to which the SAM reader belongs.
     * @param readerNameRegex A regular expression matching the expected SAM reader name.
     * @param samProfileName A string defining the SAM profile.
     * @throws IllegalStateException If the expected card resource is not found.
     */

    open fun setupCardResourceService(
        plugin: Plugin,
        readerNameRegex: String?,
        samProfileName: String?
    ){
        // Create a card resource extension expecting a SAM "C1".
        val samSelection = CalypsoExtensionService.getInstance()
            .createSamSelection()
            .filterByProductType(CalypsoSam.ProductType.SAM_C1)

        val samCardResourceExtension =
            CalypsoExtensionService.getInstance().createSamResourceProfileExtension(samSelection)

        // Get the service
        val cardResourceService = CardResourceServiceProvider.getService()

        val pluginAndReaderExceptionHandler = PluginAndReaderExceptionHandler()

        // Configure the card resource service:
        // - allocation mode is blocking with a 100 milliseconds cycle and a 10 seconds timeout.
        // - the readers are searched in the PC/SC plugin, the observation of the plugin (for the
        // connection/disconnection of readers) and of the readers (for the insertion/removal of cards)
        // is activated.
        // - two card resource profiles A and B are defined, each expecting a specific card
        // characterized by its power-on data and placed in a specific reader.
        // - the timeout for using the card's resources is set at 5 seconds.
        cardResourceService
            .configurator
            .withBlockingAllocationMode(100, 10000)
            .withPlugins(
                PluginsConfigurator.builder()
                    .addPluginWithMonitoring(
                        plugin,
                        ReaderConfigurator(),
                        pluginAndReaderExceptionHandler,
                        pluginAndReaderExceptionHandler
                    )
                    .withUsageTimeout(5000)
                    .build()
            )
            .withCardResourceProfiles(
                CardResourceProfileConfigurator.builder(samProfileName, samCardResourceExtension)
                    .withReaderNameRegex(readerNameRegex)
                    .build()
            )
            .configure()

        cardResourceService.start()

        // verify the resource availability


        // verify the resource availability
        val cardResource = cardResourceService.getCardResource(samProfileName)
            ?: throw IllegalStateException(
                java.lang.String.format(
                    "Unable to retrieve a SAM card resource for profile '%s' from reader '%s' in plugin '%s'",
                    samProfileName, readerNameRegex, plugin.name
                )
            )

        // release the resource
        cardResourceService.releaseCardResource(cardResource)
    }

    protected fun getSecuritySettings(): CardSecuritySetting? {

        // The default KIF values for personalization, loading and debiting
        val DEFAULT_KIF_PERSO = 0x21.toByte()
        val DEFAULT_KIF_LOAD = 0x27.toByte()
        val DEFAULT_KIF_DEBIT = 0x30.toByte()
        // The default key record number values for personalization, loading and debiting
        // The actual value should be adjusted.
        val DEFAULT_KEY_RECORD_NUMBER_PERSO = 0x01.toByte()
        val DEFAULT_KEY_RECORD_NUMBER_LOAD = 0x02.toByte()
        val DEFAULT_KEY_RECORD_NUMBER_DEBIT = 0x03.toByte()

        val samCardResourceExtension =
            CalypsoExtensionService.getInstance()

        val sec = samCardResourceExtension.createCardSecuritySetting()

        // Create security settings that reference the same SAM profile requested from the card resource
        // service and enable the multiple session mode.
        // Create security settings that reference the same SAM profile requested from the card resource
        // service and enable the multiple session mode.
        val samResource = CardResourceServiceProvider.getService()
            .getCardResource(SAM_PROFILE_NAME)

        val cardSecuritySetting = CalypsoExtensionService.getInstance()
            .createCardSecuritySetting()
            .setSamResource(samResource.reader, samResource.smartCard as CalypsoSam)
            .assignDefaultKif(
                WriteAccessLevel.PERSONALIZATION,
                DEFAULT_KIF_PERSO
            )
            .assignDefaultKif(
                WriteAccessLevel.LOAD,
                DEFAULT_KIF_LOAD
            ) //
            .assignDefaultKif(
                WriteAccessLevel.DEBIT,
                DEFAULT_KIF_DEBIT
            ) //
            .enableMultipleSession()

        return cardSecuritySetting
    }

    /**
     * Reader configurator used by the card resource service to setup the SAM reader with the required
     * settings.
     */
    internal class ReaderConfigurator : ReaderConfiguratorSpi {
        /** {@inheritDoc}  */
        override fun setupReader(reader: Reader) {
            // Configure the reader with parameters suitable for contactless operations.
            try {
                reader
                    .getExtension(AndroidFamocoReader::class.java)
            } catch (e: Exception) {
                Timber.e(
                    "Exception raised while setting up the reader ${reader.getName()} : ${e.message}"
                )
            }
        }
    }


    /** Class implementing the exception handler SPIs for plugin and reader monitoring.  */
    private class PluginAndReaderExceptionHandler :
        PluginObservationExceptionHandlerSpi, CardReaderObservationExceptionHandlerSpi {
        override fun onPluginObservationError(pluginName: String, e: Throwable) {
            Timber.e("An exception occurred while monitoring the plugin '${e.message}'.")
        }

        override fun onReaderObservationError(
            pluginName: String,
            readerName: String,
            e: Throwable
        ) {
            Timber.e("An exception occurred while monitoring the plugin '${e.message}'.")
        }
    }
}
