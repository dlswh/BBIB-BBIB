package com.example.backend.global.kafka.consumer;

import com.example.backend.domain.ipc.dto.IpcMessageDto;
import com.example.backend.domain.ipc.service.IpcStreamService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IpcKafkaConsumer {

    private final IpcStreamService ipcStreamService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "ipc.motor.command", concurrency = "3")
    public void consumeCommand(String rawMessage) {
        ipcStreamService.process(parse(rawMessage), rawMessage);
    }

    @KafkaListener(topics = "ipc.motor.state", concurrency = "3")
    public void consumeState(String rawMessage) {
        ipcStreamService.process(parse(rawMessage), rawMessage);
    }

    @KafkaListener(topics = "ipc.motor.error", concurrency = "1")
    public void consumeError(String rawMessage) {
        ipcStreamService.process(parse(rawMessage), rawMessage);
    }

    private IpcMessageDto parse(String rawMessage) {
        try {
            return objectMapper.readValue(rawMessage, IpcMessageDto.class);
        } catch (Exception e) {
            log.error("Kafka 메시지 파싱 실패 - message: {}, error: {}", rawMessage, e.getMessage());
            throw new RuntimeException("Kafka 메시지 파싱 실패", e);
        }
    }
}