package ftn.svt.model.dto.chat;

public record ChatUpdateRequest(
        String name,
        String description,
        String imageUrl
) {
}
