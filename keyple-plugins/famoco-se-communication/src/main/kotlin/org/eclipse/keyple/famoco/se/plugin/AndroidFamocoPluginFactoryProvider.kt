package org.eclipse.keyple.famoco.se.plugin

object AndroidFamocoPluginFactoryProvider {

     fun getFactory(): AndroidFamocoPluginFactory = AndroidFamocoPluginFactoryAdapter()
}