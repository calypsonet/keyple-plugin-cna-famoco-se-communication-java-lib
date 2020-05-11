package org.cna.keyple.famoco.validator.data.model;

public class CardReaderResponse {

    public final Status status;

    public final Integer ticketsNumber;

    public final String contract;

    public final String cardType;

    public CardReaderResponse(Status status, Integer ticketsNumber, String contract, String cardType) {
        this.status = status;
        this.ticketsNumber = ticketsNumber;
        this.contract = contract;
        this.cardType = cardType;
    }
}

