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

func (l httpClientLogger) Error(msg string, keysAndValues ...interface{}) {
	l.l.Error(msg, fields(keysAndValues)...)
}

func (l httpClientLogger) Info(msg string, keysAndValues ...interface{}) {
	l.l.Info(msg, fields(keysAndValues)...)
}

func (l httpClientLogger) Debug(msg string, keysAndValues ...interface{}) {
	l.l.Debug(msg, fields(keysAndValues)...)
}

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
