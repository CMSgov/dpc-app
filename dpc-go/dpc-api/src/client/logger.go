package client

import (
	"fmt"
	"go.uber.org/zap"
)

type httpClientLogger struct {
	l zap.Logger
}

func newLogger(log zap.Logger) *httpClientLogger {
	return &httpClientLogger{
		l: log,
	}
}

// Error function to fulfill retryablehttp logger interface
func (l httpClientLogger) Error(msg string, keysAndValues ...interface{}) {
	l.l.Error(msg, fields(keysAndValues)...)
}

// Info function to fulfill retryablehttp logger interface
func (l httpClientLogger) Info(msg string, keysAndValues ...interface{}) {
	l.l.Info(msg, fields(keysAndValues)...)
}

// Debug function to fulfill retryablehttp logger interface
func (l httpClientLogger) Debug(msg string, keysAndValues ...interface{}) {
	l.l.Debug(msg, fields(keysAndValues)...)
}

// Warn function to fulfill retryablehttp logger interface
func (l httpClientLogger) Warn(msg string, keysAndValues ...interface{}) {
	l.l.Warn(msg, fields(keysAndValues)...)
}

func fields(keysAndValues []interface{}) []zap.Field {
	fields := make([]zap.Field, 0)
	for i := 0; i < len(keysAndValues); i += 2 {
		key := fmt.Sprintf("%v", keysAndValues[i])
		value := fmt.Sprintf("%v", keysAndValues[i+1])
		fields = append(fields, zap.String(key, value))
	}
	return fields
}
