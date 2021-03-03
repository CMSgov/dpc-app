package logger

import (
	"bytes"
	"context"
	"encoding/json"
	"github.com/CMSgov/dpc/attribution/conf"
	"go.uber.org/zap"
)

type loggerKeyType int

// LoggerKey const for fetching the logger from the context
const LoggerKey loggerKeyType = iota

// logger that gets initialized
var logger *zap.Logger

func init() {
	lc := conf.Get("log")

	var cfg zap.Config
	if lc != nil {
		var lcb bytes.Buffer // Stand-in for a network connection
		err := json.NewEncoder(&lcb).Encode(lc)
		if err != nil {
			panic(err)
		}
		if err := json.Unmarshal(lcb.Bytes(), &cfg); err != nil {
			panic(err)
		}
		logger, _ = cfg.Build()
	}

	if logger == nil {
		l, err := zap.NewProduction()
		if err != nil {
			panic(err)
		}
		logger = l
	}
}

// SetLogger function to allow the ability to override the logger
func SetLogger(l *zap.Logger) {
	logger = l
}

// SyncLogger function calls zap logger sync
func SyncLogger() error {
	return logger.Sync()
}

// NewContext function to create a context that holds a zap logger with zap fields
func NewContext(ctx context.Context, fields ...zap.Field) context.Context {
	return context.WithValue(ctx, LoggerKey, WithContext(ctx).With(fields...))
}

// WithContext function to retrieve the logger from the context if in the context or return the logger if not found in the context
func WithContext(ctx context.Context) *zap.Logger {
	if ctx == nil {
		return logger
	}

	if ctxlogger, ok := ctx.Value(LoggerKey).(*zap.Logger); ok {
		return ctxlogger
	}
	return logger
}
