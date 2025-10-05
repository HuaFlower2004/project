package com.mi.project.util;

import com.mi.project.common.MyWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

@Slf4j
public class WebSocketSenderUtil {

    // 直接向所有前端发送 JSON 字符串
    public static void sendJsonToAll(String json) {
        log.info("sendJsonToAll called, session count: {}", MyWebSocketHandler.getSessions().size());
        for (Map.Entry<String, WebSocketSession> entry : MyWebSocketHandler.getSessions().entrySet()) {
            WebSocketSession session = entry.getValue();
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(json));
                    log.info("Sent to session: " + session.getId());
                } catch (IOException e) {
                    log.info("错误爆发");
                }
            }
        }
    }

    // 读取 JSON 文件并发送给所有前端
    public static void sendJsonFileToAll(String jsonFilePath) {
        try {
            String json = new String(Files.readAllBytes(Paths.get(jsonFilePath)));
            sendJsonToAll(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
