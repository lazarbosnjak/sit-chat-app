package audio

import "time"

type AudioMetadata struct {
	ID          string    `json:"id"`
	MessageID   string    `json:"messageId"`
	ChatID      string    `json:"chatId"`
	SenderID    string    `json:"senderId"`
	FilePath    string    `json:"-"`
	ContentType string    `json:"contentType"`
	SizeBytes   int64     `json:"sizeBytes"`
	DurationMS  int       `json:"durationMs"`
	CreatedAt   time.Time `json:"createdAt"`
}
