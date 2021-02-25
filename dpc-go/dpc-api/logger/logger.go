package logger

import (
	"context"
	"go.uber.org/zap"
	"gopkg.in/yaml.v2"
	"io/ioutil"
	"os"
)

type loggerKeyType int

// LoggerKey const for fetching the logger from the context
const LoggerKey loggerKeyType = iota

// logger that gets initialized
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
}

/*
   SetLogger
   function to allow the ability to override the logger
*/
func SetLogger(l *zap.Logger) {
	logger = l
}

func SyncLogger() error {
	return logger.Sync()
}

/*
   NewContext
   function to create a context that holds a zap logger with zap fields
*/
func NewContext(ctx context.Context, fields ...zap.Field) context.Context {
	return context.WithValue(ctx, LoggerKey, WithContext(ctx).With(fields...))
}

/*
   WithContext
   function to retrieve the logger from the context if in the context or return the logger if not found in
   the context
*/
func WithContext(ctx context.Context) *zap.Logger {
	if ctx == nil {
		return logger
	}

	if ctxlogger, ok := ctx.Value(LoggerKey).(*zap.Logger); ok {
		return ctxlogger
	} else {
		return logger
	}

}
