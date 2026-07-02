package ftn.svt.model.dto.audio;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;

public record AudioStreamResponse(
        HttpStatusCode statusCode,
        HttpHeaders headers,
        byte[] body
) {
}
