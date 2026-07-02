package audio

import (
	"errors"
	"fmt"
	"time"

	"github.com/jackc/pgx"
)

const defaultMaxPostgresConnections = 5
const defaultConnectTimeout = 30 * time.Second
const defaultRetryInterval = 2 * time.Second

type PostgresConfig struct {
	DatabaseURL    string
	MaxConnections int
	ConnectTimeout time.Duration
	RetryInterval  time.Duration
}

func NewPostgresPool(config PostgresConfig) (*pgx.ConnPool, error) {
	if config.DatabaseURL == "" {
		return nil, errors.New("database URL is required")
	}

	connectionConfig, err := pgx.ParseURI(config.DatabaseURL)
	if err != nil {
		return nil, fmt.Errorf("parse database URL: %w", err)
	}

	maxConnections := config.MaxConnections
	if maxConnections <= 0 {
		maxConnections = defaultMaxPostgresConnections
	}

	connectTimeout := config.ConnectTimeout
	if connectTimeout <= 0 {
		connectTimeout = defaultConnectTimeout
	}

	retryInterval := config.RetryInterval
	if retryInterval <= 0 {
		retryInterval = defaultRetryInterval
	}

	deadline := time.Now().Add(connectTimeout)
	var lastErr error

	for {
		pool, err := connect(connectionConfig, maxConnections)
		if err == nil {
			return pool, nil
		}

		lastErr = err
		if time.Now().Add(retryInterval).After(deadline) {
			break
		}

		time.Sleep(retryInterval)
	}

	return nil, fmt.Errorf("connect to postgres after %s: %w", connectTimeout, lastErr)
}

func connect(connectionConfig pgx.ConnConfig, maxConnections int) (*pgx.ConnPool, error) {
	pool, err := pgx.NewConnPool(pgx.ConnPoolConfig{
		ConnConfig:     connectionConfig,
		MaxConnections: maxConnections,
	})
	if err != nil {
		return nil, err
	}

	if _, err := pool.Exec("SELECT 1"); err != nil {
		pool.Close()
		return nil, err
	}

	return pool, nil
}
