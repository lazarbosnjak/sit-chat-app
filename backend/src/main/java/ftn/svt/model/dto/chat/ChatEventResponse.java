package ftn.svt.model.dto.chat;

import java.util.List;
import java.util.UUID;

public record ChatEventResponse(
        String type,
        UUID chatId,
        MessageResponse message,
        long unreadCount,
        List<MessageStatusResponse> messageStatuses
) {}
