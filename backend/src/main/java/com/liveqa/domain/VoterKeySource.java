package com.liveqa.domain;

/**
 * 投票キーの取得元（HOST / 匿名 / 未確立）
 * VoterKeySource
 * @author ichikura
 */
public sealed interface VoterKeySource {

    /** OIDCログイン済み。oidcSubjectはKeycloakのsub */
    record Host(String oidcSubject) implements VoterKeySource {}

    /** 匿名SessionにguestIdあり */
    record Guest(String guestId) implements VoterKeySource {}

    /** どちらも無い（質問POSTは401、一覧のvotedByMeは全部false） */
    record Missing() implements VoterKeySource {}

}
