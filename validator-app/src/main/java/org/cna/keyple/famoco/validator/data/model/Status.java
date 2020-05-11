package org.cna.keyple.famoco.validator.data.model;

public enum Status {
    LOADING("loading"),
    ERROR("error"),
    TICKETS_FOUND("tickets_found"),
    INVALID_CARD("invalid_card"),
    EMPTY_CARD("empty_card"),
    SUCCESS("success");

    private final String status;

    /**
     * @param status
     */
    Status(final String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return status;
    }

    public static Status getStatus(String name) {
        try {
            return Status.valueOf(name.toUpperCase());
        } catch (Exception e) {
            // If the given state does not exist, return the default value.
            return Status.ERROR;
        }
    }
}