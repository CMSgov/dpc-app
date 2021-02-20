package logger

import (
	"context"
	"go.uber.org/zap"
	"gopkg.in/yaml.v2"
	"io/ioutil"
	"os"
)

type loggerKeyType int

const LoggerKey loggerKeyType = iota

var Logger *zap.Logger

func init() {
	lcfgfile, found := os.LookupEnv("ATTRIBUTION_LOG_CONFIG")
	if !found {
		lcfgfile = "logconfig.yml"
	}

	var cfg zap.Config
	lcfg, _ := ioutil.ReadFile(lcfgfile)

	if lcfg != nil {
		if err := yaml.Unmarshal(lcfg, &cfg); err != nil {
			panic(err)
		}
		Logger, _ = cfg.Build()
	}

	if Logger == nil {
		l, err := zap.NewProduction()
		if err != nil {
			panic(err)
		}
		Logger = l
	}

	defer Logger.Sync()
}

func NewContext(ctx context.Context, fields ...zap.Field) context.Context {
	return context.WithValue(ctx, LoggerKey, WithContext(ctx).With(fields...))
}

func WithContext(ctx context.Context) *zap.Logger {
	if ctx == nil {
		return Logger
	}

	if ctxlogger, ok := ctx.Value(LoggerKey).(*zap.Logger); ok {
		return ctxlogger
	} else {
		return Logger
	}

}
