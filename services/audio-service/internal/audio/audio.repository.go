package audio

import (
	"fmt"
	"time"

	"github.com/jackc/pgx"
)

type AudioMetadataRepository struct {
	db *pgx.ConnPool
}

func NewAudioMetadataRepository(db *pgx.ConnPool) *AudioMetadataRepository {
	return &AudioMetadataRepository{db: db}
}

func (repository *AudioMetadataRepository) EnsureSchema() error {
	_, err := repository.db.Exec(`
		CREATE TABLE IF NOT EXISTS audio_metadata (
			id TEXT PRIMARY KEY,
			message_id TEXT NOT NULL UNIQUE,
			chat_id TEXT NOT NULL,
			sender_id TEXT NOT NULL,
			file_path TEXT NOT NULL,
			content_type TEXT NOT NULL,
			size_bytes BIGINT NOT NULL,
			duration_ms INTEGER NOT NULL,
			created_at TIMESTAMPTZ NOT NULL DEFAULT now()
		);
	`)
	if err != nil {
		return fmt.Errorf("ensure audio metadata schema: %w", err)
	}

	_, err = repository.db.Exec(`
		CREATE INDEX IF NOT EXISTS idx_audio_metadata_chat_id
			ON audio_metadata(chat_id)
	`)
	if err != nil {
		return fmt.Errorf("ensure audio metadata chat index: %w", err)
	}

	return nil
}

func (repository *AudioMetadataRepository) Save(metadata AudioMetadata) error {
	createdAt := metadata.CreatedAt
	if createdAt.IsZero() {
		createdAt = time.Now().UTC()
	}

	_, err := repository.db.Exec(`
		INSERT INTO audio_metadata (
			id,
			message_id,
			chat_id,
			sender_id,
			file_path,
			content_type,
			size_bytes,
			duration_ms,
			created_at
		)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
	`, metadata.ID,
		metadata.MessageID,
		metadata.ChatID,
		metadata.SenderID,
		metadata.FilePath,
		metadata.ContentType,
		metadata.SizeBytes,
		metadata.DurationMS,
		createdAt,
	)
	if err != nil {
		return fmt.Errorf("save audio metadata: %w", err)
	}

	return nil
}

func (repository *AudioMetadataRepository) FindByID(id string) (AudioMetadata, error) {
	return repository.findOne("id", id)
}

func (repository *AudioMetadataRepository) FindByMessageID(messageID string) (AudioMetadata, error) {
	return repository.findOne("message_id", messageID)
}

func (repository *AudioMetadataRepository) DeleteByID(id string) error {
	_, err := repository.db.Exec("DELETE FROM audio_metadata WHERE id = $1", id)
	if err != nil {
		return fmt.Errorf("delete audio metadata by id: %w", err)
	}

	return nil
}

func (repository *AudioMetadataRepository) findOne(column string, value string) (AudioMetadata, error) {
	var metadata AudioMetadata

	err := repository.db.QueryRow(fmt.Sprintf(`
		SELECT
			id,
			message_id,
			chat_id,
			sender_id,
			file_path,
			content_type,
			size_bytes,
			duration_ms,
			created_at
		FROM audio_metadata
		WHERE %s = $1
	`, column), value).Scan(
		&metadata.ID,
		&metadata.MessageID,
		&metadata.ChatID,
		&metadata.SenderID,
		&metadata.FilePath,
		&metadata.ContentType,
		&metadata.SizeBytes,
		&metadata.DurationMS,
		&metadata.CreatedAt,
	)
	if err != nil {
		return AudioMetadata{}, fmt.Errorf("find audio metadata by %s: %w", column, err)
	}

	return metadata, nil
}
