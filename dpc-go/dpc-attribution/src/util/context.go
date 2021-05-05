package util

import (
	"context"
	"net/http"

	"github.com/CMSgov/dpc/attribution/logger"
	middleware2 "github.com/CMSgov/dpc/attribution/middleware"
	"github.com/darahayes/go-boom"
)

func FetchValueFromContext(ctx context.Context, w http.ResponseWriter, key middleware2.ContextKey) string {
	log := logger.WithContext(ctx)

	keyValue, ok := ctx.Value(key).(string)
	if !ok {
		log.Error("Failed to extract key from context")
		boom.BadRequest(w,"Could not get value from context")
	}
	return keyValue
}