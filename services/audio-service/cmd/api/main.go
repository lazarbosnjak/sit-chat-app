package main

import (
	"context"
	"errors"
	"log"
	"net/http"
	"os"
	"os/signal"
	"strconv"
	"syscall"
	"time"

	"audio_service/internal/audio"

	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
)

func main() {
	logger := log.New(os.Stdout, "[SVT-Audio-Service]", log.LstdFlags)

	postgresPool, err := audio.NewPostgresPool(audio.PostgresConfig{
		DatabaseURL:    databaseURLFromEnv(),
		MaxConnections: postgresMaxConnectionsFromEnv(logger),
		ConnectTimeout: durationFromEnv(logger, "AUDIO_DB_CONNECT_TIMEOUT", 30*time.Second),
		RetryInterval:  durationFromEnv(logger, "AUDIO_DB_RETRY_INTERVAL", 2*time.Second),
	})
	if err != nil {
		logger.Fatal(err)
	}
	defer postgresPool.Close()

	audioMetadataRepository := audio.NewAudioMetadataRepository(postgresPool)
	if err := audioMetadataRepository.EnsureSchema(); err != nil {
		logger.Fatal(err)
	}

	audioStorage := audio.NewFileStorage(envOrDefault("AUDIO_STORAGE_DIR", "./data/audio"))
	audioHandler := audio.NewHandler(
		audioMetadataRepository,
		audioStorage,
		os.Getenv("AUDIO_INTERNAL_TOKEN"),
		int64FromEnv(logger, "AUDIO_MAX_UPLOAD_BYTES", 25<<20),
	)

	logger.Println("Connected to PostgreSQL.")

	router := chi.NewRouter()
	router.Use(middleware.Logger)

	router.Get("/health", func(response http.ResponseWriter, request *http.Request) {
		response.WriteHeader(http.StatusOK)
	})
	router.Route("/internal/audio", audioHandler.RegisterRoutes)

	server := http.Server{
		Addr:         envOrDefault("AUDIO_HTTP_ADDR", ":8082"),
		Handler:      router,
		IdleTimeout:  120 * time.Second,
		ReadTimeout:  5 * time.Second,
		WriteTimeout: 5 * time.Second,
	}

	go func() {
		err := server.ListenAndServe()
		if err != nil && !errors.Is(err, http.ErrServerClosed) {
			logger.Fatal(err)
		}
	}()

	logger.Printf("Listening on %s.", server.Addr)

	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, syscall.SIGINT, syscall.SIGTERM)

	<-sigChan
	timeoutContext, cancel := context.WithTimeout(context.Background(), time.Second*30)

	defer cancel()

	logger.Println("Shutting down gracefully...")
	if err := server.Shutdown(timeoutContext); err != nil {
		logger.Fatal(err)
	}
	logger.Println("Server stopped.")
}

func databaseURLFromEnv() string {
	if databaseURL := os.Getenv("AUDIO_DATABASE_URL"); databaseURL != "" {
		return databaseURL
	}

	return os.Getenv("DATABASE_URL")
}

func postgresMaxConnectionsFromEnv(logger *log.Logger) int {
	rawValue := os.Getenv("AUDIO_DB_MAX_CONNECTIONS")
	if rawValue == "" {
		return 0
	}

	maxConnections, err := strconv.Atoi(rawValue)
	if err != nil || maxConnections < 1 {
		logger.Fatalf("AUDIO_DB_MAX_CONNECTIONS must be a positive integer, got %q", rawValue)
	}

	return maxConnections
}

func int64FromEnv(logger *log.Logger, key string, defaultValue int64) int64 {
	rawValue := os.Getenv(key)
	if rawValue == "" {
		return defaultValue
	}

	value, err := strconv.ParseInt(rawValue, 10, 64)
	if err != nil || value < 1 {
		logger.Fatalf("%s must be a positive integer, got %q", key, rawValue)
	}

	return value
}

func durationFromEnv(logger *log.Logger, key string, defaultValue time.Duration) time.Duration {
	rawValue := os.Getenv(key)
	if rawValue == "" {
		return defaultValue
	}

	duration, err := time.ParseDuration(rawValue)
	if err != nil || duration <= 0 {
		logger.Fatalf("%s must be a positive duration, got %q", key, rawValue)
	}

	return duration
}

func envOrDefault(key string, defaultValue string) string {
	value := os.Getenv(key)
	if value == "" {
		return defaultValue
	}

	return value
}
