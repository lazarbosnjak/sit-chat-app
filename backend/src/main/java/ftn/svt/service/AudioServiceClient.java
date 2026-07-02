package ftn.svt.service;

import ftn.svt.exception.ApiException;
import ftn.svt.model.dto.audio.AudioMetadataResponse;
import ftn.svt.model.dto.audio.AudioStreamResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
public class AudioServiceClient {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final RestClient restClient;
    private final String internalToken;

    public AudioServiceClient(
            @Value("${app.audio-service.base-url:http://localhost:8082}") String baseUrl,
            @Value("${app.audio-service.internal-token:}") String internalToken
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.internalToken = internalToken;
    }

    public AudioMetadataResponse upload(
            UUID messageId,
            UUID chatId,
            UUID senderId,
            MultipartFile file,
            int durationMs
    ) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("messageId", messageId.toString());
            body.add("chatId", chatId.toString());
            body.add("senderId", senderId.toString());
            body.add("durationMs", Integer.toString(durationMs));
            body.add("file", audioFilePart(file));

            AudioMetadataResponse response = withInternalToken(restClient.post()
                    .uri("/internal/audio/")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body))
                    .retrieve()
                    .body(AudioMetadataResponse.class);

            if (response == null) {
                throw ApiException.badGateway("Audio service returned an empty upload response");
            }

            return response;
        } catch (IOException ex) {
            throw ApiException.badRequest("Could not read uploaded audio file");
        } catch (HttpStatusCodeException ex) {
            log.warn("Audio service upload failed with status {}", ex.getStatusCode(), ex);
            throw ApiException.badGateway("Audio service could not store the voice message");
        } catch (RestClientException ex) {
            log.warn("Audio service upload failed", ex);
            throw ApiException.badGateway("Audio service is unavailable");
        }
    }

    public AudioStreamResponse stream(UUID audioId, String rangeHeader) {
        try {
            RestClient.RequestHeadersSpec<?> request = withInternalToken(restClient.get()
                    .uri("/internal/audio/{audioId}", audioId));

            if (rangeHeader != null && !rangeHeader.isBlank()) {
                request = request.header(HttpHeaders.RANGE, rangeHeader);
            }

            ResponseEntity<byte[]> response = request
                    .retrieve()
                    .toEntity(byte[].class);

            return new AudioStreamResponse(
                    response.getStatusCode(),
                    mediaHeaders(response.getHeaders()),
                    response.getBody() == null ? new byte[0] : response.getBody()
            );
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw ApiException.notFound("Audio file not found");
            }

            log.warn("Audio service stream failed with status {}", ex.getStatusCode(), ex);
            throw ApiException.badGateway("Audio service could not stream the voice message");
        } catch (RestClientException ex) {
            log.warn("Audio service stream failed", ex);
            throw ApiException.badGateway("Audio service is unavailable");
        }
    }

    public void delete(UUID audioId) {
        try {
            withInternalToken(restClient.delete()
                    .uri("/internal/audio/{audioId}", audioId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().value() == 404) {
                return;
            }

            log.warn("Audio service delete failed with status {}", ex.getStatusCode(), ex);
            throw ApiException.badGateway("Audio service could not delete the voice message");
        } catch (RestClientException ex) {
            log.warn("Audio service delete failed", ex);
            throw ApiException.badGateway("Audio service is unavailable");
        }
    }

    private HttpEntity<ByteArrayResource> audioFilePart(MultipartFile file) throws IOException {
        ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                String filename = file.getOriginalFilename();
                return filename == null || filename.isBlank() ? "voice-message.webm" : filename;
            }
        };

        HttpHeaders headers = new HttpHeaders();
        if (file.getContentType() != null && !file.getContentType().isBlank()) {
            try {
                headers.setContentType(MediaType.parseMediaType(file.getContentType()));
            } catch (InvalidMediaTypeException ex) {
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            }
        } else {
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        }

        return new HttpEntity<>(resource, headers);
    }

    private RestClient.RequestHeadersSpec<?> withInternalToken(RestClient.RequestHeadersSpec<?> request) {
        if (internalToken == null || internalToken.isBlank()) {
            return request;
        }

        return request.header(INTERNAL_TOKEN_HEADER, internalToken);
    }

    private HttpHeaders mediaHeaders(HttpHeaders source) {
        HttpHeaders headers = new HttpHeaders();
        copyHeader(source, headers, HttpHeaders.CONTENT_TYPE);
        copyHeader(source, headers, HttpHeaders.CONTENT_LENGTH);
        copyHeader(source, headers, "Content-Range");
        copyHeader(source, headers, "Accept-Ranges");
        return headers;
    }

    private void copyHeader(HttpHeaders source, HttpHeaders target, String name) {
        if (source.containsHeader(name)) {
            target.put(name, source.get(name));
        }
    }
}
