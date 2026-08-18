package com.liveqa.domain;

public record VoterKey(String value) {

    public static VoterKey guest(String guestId) {
        return new VoterKey("g:" + guestId);
    }

    public static VoterKey host(Long id) {
        return new VoterKey("u:" + id);
    }
}
