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

	var cfg zap.Config
	lcfg, _ := ioutil.ReadFile(lcfgfile)

	if lcfg != nil {
		if err := yaml.Unmarshal(lcfg, &cfg); err != nil {
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
