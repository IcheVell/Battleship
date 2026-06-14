package network;

import com.fasterxml.jackson.databind.ObjectMapper;
import controller.BattleshipClientController;
import request.BaseRequest;

import javax.websocket.*;
import java.net.URI;

@ClientEndpoint
public class BattleshipWebSocketClient {

    private Session session;
    private final ObjectMapper objectMapper;
    private BattleshipClientController controller;

    public BattleshipWebSocketClient(BattleshipClientController controller) {
        this.controller = controller;
        this.objectMapper = new ObjectMapper();
    }

    public void connect(String url) {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, new URI(url));
        } catch (Exception e) {
            System.err.println("Ошибка сети: " + e.getMessage());
        }
    }

    public void disconnect() {
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        System.out.println("Соединение с сервером установлено!");
    }

    @OnMessage
    public void onMessage(String message) {
        controller.handleServerMessage(message);
    }

    public void sendRequest(BaseRequest request) {
        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(request);
                session.getBasicRemote().sendText(json);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}