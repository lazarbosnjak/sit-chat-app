package audio

import (
	"fmt"
	"io"
	"os"
	"path/filepath"
	"strings"
)

type FileStorage struct {
	rootDir string
}

func NewFileStorage(rootDir string) *FileStorage {
	return &FileStorage{rootDir: rootDir}
}

func (storage *FileStorage) Save(id string, originalFilename string, source io.Reader) (string, error) {
	if err := os.MkdirAll(storage.rootDir, 0750); err != nil {
		return "", fmt.Errorf("create audio storage directory: %w", err)
	}

	extension := strings.ToLower(filepath.Ext(originalFilename))
	if extension == "" || len(extension) > 16 {
		extension = ".bin"
	}

	filePath := filepath.Join(storage.rootDir, id+extension)
	destination, err := os.OpenFile(filePath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0640)
	if err != nil {
		return "", fmt.Errorf("create audio file: %w", err)
	}
	defer destination.Close()

	if _, err := io.Copy(destination, source); err != nil {
		_ = os.Remove(filePath)
		return "", fmt.Errorf("write audio file: %w", err)
	}

	return filePath, nil
}

func (storage *FileStorage) Delete(filePath string) error {
	if err := os.Remove(filePath); err != nil && !os.IsNotExist(err) {
		return fmt.Errorf("delete audio file: %w", err)
	}

	return nil
}
