package com.zyelectric.ocpp.cache;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.socket.WebSocketSession;

@Getter
@Setter
public class SessionData {

    private final WebSocketSession session;

    public SessionData(WebSocketSession session) {
        this.session = session;
    }
}
