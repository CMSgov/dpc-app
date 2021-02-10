package logger

import (
	"context"
	"github.com/go-chi/chi/middleware"
	"go.uber.org/zap"
	"gopkg.in/yaml.v2"
	"io/ioutil"
	"os"
)

var logger *zap.Logger

func init() {
	lcfgfile, found := os.LookupEnv("API_LOG_CONFIG")
	if !found {
		lcfgfile = "logconfig.yml"
	}

	lcfg, err := ioutil.ReadFile(lcfgfile)
	if err != nil {
		panic(err)
	}

	var cfg zap.Config
	if err := yaml.Unmarshal(lcfg, &cfg); err != nil {
		panic(err)
	}
	logger, err = cfg.Build()
	if err != nil {
		panic(err)
	}
	defer logger.Sync()
}

func WithContext(ctx context.Context) *zap.Logger {
	newLogger := logger
	if ctx != nil {
		if ctxRqId, ok := ctx.Value(middleware.RequestIDKey).(string); ok {
			newLogger = newLogger.With(zap.String("rqId", ctxRqId))
		}
	}
	return newLogger
}
