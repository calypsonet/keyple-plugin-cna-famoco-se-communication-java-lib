/********************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://www.calypsonet-asso.org/
 ********************************************************************************/
package org.cna.keyple.famoco.example.model

data class ChoiceEventModel(val title: String, val choices: List<String> = arrayListOf(), val callback: (choice: String) -> Unit) :
    EventModel(TYPE_MULTICHOICE, title)
