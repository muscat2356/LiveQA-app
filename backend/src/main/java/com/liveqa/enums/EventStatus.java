package com.liveqa.enums;

public enum EventStatus {
    DRAFT,
    OPEN,
    CLOSED;

    public boolean canTransitioinTo(EventStatus next) {
        return switch (this) {
            case DRAFT -> next == OPEN;
            case OPEN -> next == CLOSED;
            case CLOSED -> next == OPEN;
        };
    }
}
