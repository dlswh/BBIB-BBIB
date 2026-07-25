package com.example.backend.global.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class SseEmitterService {

    /** 대시보드용 emitter */
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /** 로그 화면용 emitter (ipcId/type 필터 포함) */
    private final CopyOnWriteArrayList<LogEmitter> logEmitters = new CopyOnWriteArrayList<>();

    // ── 대시보드 SSE ──────────────────────────────────────────────────

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);
        log.info("SSE 클라이언트 연결 - 현재 연결 수: {}", emitters.size());

        emitter.onCompletion(() -> { emitters.remove(emitter); log.info("SSE 클라이언트 연결 종료 - 현재 연결 수: {}", emitters.size()); });
        emitter.onTimeout(()    -> { emitters.remove(emitter); log.info("SSE 클라이언트 타임아웃 - 현재 연결 수: {}", emitters.size()); });
        emitter.onError(e       -> { emitters.remove(emitter); log.warn("SSE 클라이언트 에러 - {}", e.getMessage()); });

        send(emitter, "connect", "connected");
        return emitter;
    }

    public void send(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException e) {
            emitters.remove(emitter);
            log.warn("SSE 전송 실패 - emitter 제거");
        }
    }

    public void broadcast(String eventName, Object data) {
        for (SseEmitter emitter : emitters) {
            send(emitter, eventName, data);
        }
    }

    // ── 로그 화면 SSE ─────────────────────────────────────────────────

    /**
     * 로그 화면 구독
     * ipcId / types 가 null 또는 비어있으면 전체 허용
     */
    public SseEmitter subscribeLog(String ipcId, List<String> types) {
        SseEmitter emitter    = new SseEmitter(Long.MAX_VALUE);
        LogEmitter logEmitter = new LogEmitter(emitter, ipcId, types);
        logEmitters.add(logEmitter);
        log.info("로그 SSE 연결 - ipcId={}, types={}, 현재: {}", ipcId, types, logEmitters.size());

        emitter.onCompletion(() -> { logEmitters.remove(logEmitter); log.info("로그 SSE 종료 - 현재: {}", logEmitters.size()); });
        emitter.onTimeout(()    -> { logEmitters.remove(logEmitter); log.info("로그 SSE 타임아웃 - 현재: {}", logEmitters.size()); });
        emitter.onError(e       -> { logEmitters.remove(logEmitter); log.warn("로그 SSE 에러 - {}", e.getMessage()); });

        send(emitter, "connect", "connected");
        return emitter;
    }

    /**
     * 로그 이벤트 발행 — 필터 조건에 맞는 emitter에만 전송
     */
    public void broadcastLog(String ipcId, String type, Object data) {
        for (LogEmitter le : logEmitters) {
            if (le.matches(ipcId, type)) {
                try {
                    le.emitter().send(SseEmitter.event()
                            .name(type.toLowerCase())
                            .data(data));
                } catch (IOException e) {
                    logEmitters.remove(le);
                    log.warn("로그 SSE 전송 실패 - emitter 제거");
                }
            }
        }
    }

    // ── 내부 레코드 ───────────────────────────────────────────────────

    private record LogEmitter(SseEmitter emitter, String ipcId, List<String> types) {
        boolean matches(String msgIpcId, String msgType) {
            boolean ipcMatch  = ipcId == null || ipcId.isBlank() || ipcId.equals(msgIpcId);
            boolean typeMatch = types == null || types.isEmpty() ||
                    types.stream().anyMatch(t -> t.equalsIgnoreCase(msgType));
            return ipcMatch && typeMatch;
        }
    }
}