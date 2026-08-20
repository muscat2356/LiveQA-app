package com.liveqa.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.liveqa.dto.response.EventJoinResponse;
import com.liveqa.service.EventService;

/**
 * リソースjoinに対するAPIコントローラー
 * JoinController
 * @author ichikura
 */
@RestController
@RequestMapping("/api")
public class JoinController {

    private final EventService eventService;

    public JoinController(EventService eventService) {
        this.eventService = eventService;
    }

    /**
     * 参加コードからルームを照会するGETメソッド
     * @param joinCode 8文字の参加コード
     * @return 該当ルームの公開情報
     */
    @GetMapping("/join/{joinCode}")
    public ResponseEntity<EventJoinResponse> findByJoinCode(@PathVariable String joinCode) {
        return ResponseEntity.ok(eventService.findByJoinCode(joinCode));
    }
}
