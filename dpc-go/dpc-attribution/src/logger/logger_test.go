package logger

import (
	"context"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
	"go.uber.org/zap"
	"go.uber.org/zap/zapcore"
	"go.uber.org/zap/zaptest/observer"
	"testing"
)

type LoggerTestSuite struct {
	suite.Suite
}

func TestLoggerTestSuite(t *testing.T) {
	suite.Run(t, new(LoggerTestSuite))
}

func (suite *LoggerTestSuite) TestLogging() {
	ctx := context.Background()
	core, logs := observer.New(zap.InfoLevel)
	SetLogger(zap.New(core))

	ctx = NewContext(ctx, zap.String("foo", "bar"))
	WithContext(ctx).Info("Hello")

	le := logs.All()
	firstLogContextKeys := getContextKeys(le[0].Context)

	assert.Len(suite.T(), le, 1)
	assert.Contains(suite.T(), le[0].Entry.Message, "Hello")
	assert.ElementsMatch(suite.T(), firstLogContextKeys, []string{"foo"})
}

func getContextKeys(fields []zapcore.Field) []string {
	keys := make([]string, 0)
	for _, f := range fields {
		keys = append(keys, f.Key)
	}
	return keys
}
