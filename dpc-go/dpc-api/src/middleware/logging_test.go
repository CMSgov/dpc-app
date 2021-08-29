package middleware

import (
	"github.com/CMSgov/dpc/api/logger"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
	"go.uber.org/zap"
	"go.uber.org/zap/zapcore"
	"go.uber.org/zap/zaptest/observer"
	"net/http"
	"net/http/httptest"
	"testing"
)

type LoggingTestSuite struct {
	suite.Suite
}

func TestLoggingTestSuite(t *testing.T) {
	suite.Run(t, new(LoggingTestSuite))
}

func (suite *LoggingTestSuite) TestLogging() {
	core, logs := observer.New(zap.InfoLevel)
	logger.SetLogger(zap.New(core))

	nextHandler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		_, _ = w.Write([]byte("hello"))
	})

	req := httptest.NewRequest(http.MethodGet, "http://www.your-domain.com", nil)
	res := httptest.NewRecorder()

	l := Logging()(nextHandler)
	l.ServeHTTP(res, req)

	le := logs.All()
	firstLogContextKeys := getContextKeys(le[0].Context)
	secondLogContextKeys := getContextKeys(le[1].Context)

	assert.Len(suite.T(), le, 2)
	assert.Contains(suite.T(), le[0].Entry.Message, "Starting request")
	assert.ElementsMatch(suite.T(), firstLogContextKeys, []string{"rqId", "request-uri", "from", "method"})
	assert.ElementsMatch(suite.T(), secondLogContextKeys, []string{"rqId", "response-code", "bytes"})
}

func getContextKeys(fields []zapcore.Field) []string {
	keys := make([]string, 0)
	for _, f := range fields {
		keys = append(keys, f.Key)
	}
	return keys
}
