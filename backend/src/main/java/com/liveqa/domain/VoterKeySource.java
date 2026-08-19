package com.liveqa.domain;

public sealed interface VoterKeySource {

    record Host(String oidcSubject) implements VoterKeySource {}
    record Guest(String guestId) implements VoterKeySource {}
    record Missing() implements VoterKeySource {}

}
