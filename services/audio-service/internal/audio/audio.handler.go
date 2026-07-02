package audio

import (
	"crypto/rand"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"os"
	"strconv"
	"strings"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/jackc/pgx"
)

const defaultMaxUploadBytes = 25 << 20
const internalTokenHeader = "X-Internal-Token"

type Handler struct {
	repository     *AudioMetadataRepository
	storage        *FileStorage
	internalToken  string
	maxUploadBytes int64
}

func NewHandler(
	repository *AudioMetadataRepository,
	storage *FileStorage,
	internalToken string,
	maxUploadBytes int64,
) *Handler {
	if maxUploadBytes <= 0 {
		maxUploadBytes = defaultMaxUploadBytes
	}

	return &Handler{
		repository:     repository,
		storage:        storage,
		internalToken:  internalToken,
		maxUploadBytes: maxUploadBytes,
	}
}

func (handler *Handler) RegisterRoutes(router chi.Router) {
	router.Group(func(router chi.Router) {
		router.Use(handler.requireInternalToken)

		router.Post("/", handler.upload)
		router.Get("/{id}", handler.stream)
		router.Delete("/{id}", handler.delete)
	})
}

func (handler *Handler) requireInternalToken(next http.Handler) http.Handler {
	return http.HandlerFunc(func(response http.ResponseWriter, request *http.Request) {
		if handler.internalToken != "" && request.Header.Get(internalTokenHeader) != handler.internalToken {
			http.Error(response, "invalid internal token", http.StatusUnauthorized)
			return
		}

		next.ServeHTTP(response, request)
	})
}

func (handler *Handler) upload(response http.ResponseWriter, request *http.Request) {
	request.Body = http.MaxBytesReader(response, request.Body, handler.maxUploadBytes)

	if err := request.ParseMultipartForm(handler.maxUploadBytes); err != nil {
		http.Error(response, "invalid multipart form", http.StatusBadRequest)
		return
	}

	messageID := strings.TrimSpace(request.FormValue("messageId"))
	chatID := strings.TrimSpace(request.FormValue("chatId"))
	senderID := strings.TrimSpace(request.FormValue("senderId"))
	durationMS, err := strconv.Atoi(request.FormValue("durationMs"))
	if err != nil || durationMS < 0 {
		http.Error(response, "durationMs must be a non-negative integer", http.StatusBadRequest)
		return
	}

	if messageID == "" || chatID == "" || senderID == "" {
		http.Error(response, "messageId, chatId, and senderId are required", http.StatusBadRequest)
		return
	}

	file, fileHeader, err := request.FormFile("file")
	if err != nil {
		http.Error(response, "file is required", http.StatusBadRequest)
		return
	}
	defer file.Close()

	contentType := fileHeader.Header.Get("Content-Type")
	if contentType == "" {
		contentType = "application/octet-stream"
	}

	if !isSupportedAudioContentType(contentType) {
		http.Error(response, "unsupported audio content type", http.StatusUnsupportedMediaType)
		return
	}

	audioID, err := newUUID()
	if err != nil {
		http.Error(response, "could not create audio id", http.StatusInternalServerError)
		return
	}

	filePath, err := handler.storage.Save(audioID, fileHeader.Filename, file)
	if err != nil {
		http.Error(response, "could not store audio file", http.StatusInternalServerError)
		return
	}

	metadata := AudioMetadata{
		ID:          audioID,
		MessageID:   messageID,
		ChatID:      chatID,
		SenderID:    senderID,
		FilePath:    filePath,
		ContentType: contentType,
		SizeBytes:   fileHeader.Size,
		DurationMS:  durationMS,
		CreatedAt:   time.Now().UTC(),
	}

	if err := handler.repository.Save(metadata); err != nil {
		_ = handler.storage.Delete(filePath)
		http.Error(response, "could not save audio metadata", http.StatusInternalServerError)
		return
	}

	writeJSON(response, http.StatusCreated, metadata)
}

func (handler *Handler) stream(response http.ResponseWriter, request *http.Request) {
	audioID := chi.URLParam(request, "id")
	metadata, err := handler.repository.FindByID(audioID)
	if err != nil {
		status := statusForRepositoryError(err)
		http.Error(response, http.StatusText(status), status)
		return
	}

	file, err := os.Open(metadata.FilePath)
	if err != nil {
		if os.IsNotExist(err) {
			http.Error(response, "audio file not found", http.StatusNotFound)
			return
		}

		http.Error(response, "could not open audio file", http.StatusInternalServerError)
		return
	}
	defer file.Close()

	fileInfo, err := file.Stat()
	if err != nil {
		http.Error(response, "could not stat audio file", http.StatusInternalServerError)
		return
	}

	response.Header().Set("Accept-Ranges", "bytes")
	response.Header().Set("Content-Type", metadata.ContentType)
	http.ServeContent(response, request, audioID, fileInfo.ModTime(), file)
}

func (handler *Handler) delete(response http.ResponseWriter, request *http.Request) {
	audioID := chi.URLParam(request, "id")
	metadata, err := handler.repository.FindByID(audioID)
	if err != nil {
		status := statusForRepositoryError(err)
		http.Error(response, http.StatusText(status), status)
		return
	}

	if err := handler.storage.Delete(metadata.FilePath); err != nil {
		http.Error(response, "could not delete audio file", http.StatusInternalServerError)
		return
	}

	if err := handler.repository.DeleteByID(audioID); err != nil {
		http.Error(response, "could not delete audio metadata", http.StatusInternalServerError)
		return
	}

	response.WriteHeader(http.StatusNoContent)
}

func isSupportedAudioContentType(contentType string) bool {
	contentType = strings.ToLower(strings.TrimSpace(strings.Split(contentType, ";")[0]))

	return strings.HasPrefix(contentType, "audio/") || contentType == "video/webm"
}

func statusForRepositoryError(err error) int {
	if errors.Is(err, pgx.ErrNoRows) {
		return http.StatusNotFound
	}

	return http.StatusInternalServerError
}

func writeJSON(response http.ResponseWriter, status int, payload any) {
	response.Header().Set("Content-Type", "application/json")
	response.WriteHeader(status)
	_ = json.NewEncoder(response).Encode(payload)
}

func newUUID() (string, error) {
	var bytes [16]byte
	if _, err := rand.Read(bytes[:]); err != nil {
		return "", fmt.Errorf("read random bytes: %w", err)
	}

	bytes[6] = (bytes[6] & 0x0f) | 0x40
	bytes[8] = (bytes[8] & 0x3f) | 0x80

	return fmt.Sprintf(
		"%s-%s-%s-%s-%s",
		hex.EncodeToString(bytes[0:4]),
		hex.EncodeToString(bytes[4:6]),
		hex.EncodeToString(bytes[6:8]),
		hex.EncodeToString(bytes[8:10]),
		hex.EncodeToString(bytes[10:16]),
	), nil
}
