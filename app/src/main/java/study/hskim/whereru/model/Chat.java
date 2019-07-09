package study.hskim.whereru.model;

import java.util.HashMap;
import java.util.Map;

public class Chat {

    public Map<String, Boolean> users = new HashMap<>(); // 채팅방의 유저들
    public Map<String, ChatMessage> messages = new HashMap<>(); // 채팅방 내용


    public static class ChatMessage {
        public String userId;
        public String chatMessage;
        public Object timestamp;
    }
}
