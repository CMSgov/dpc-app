package main

import (
	"encoding/json"
	"fmt"
	"net/http"
	"os"

	api "github.com/CMSgov/dpc/pkg/api"

	"go.uber.org/zap"
)

func init() {
	rawJSON := []byte(`{
	  "level": "info",
	  "encoding": "json",
	  "outputPaths": ["stdout"],
	  "errorOutputPaths": ["stderr"],
	  "encoderConfig": {
	    "messageKey": "message",
	    "levelKey": "level",
	    "levelEncoder": "lowercase",
        "timeEncoder": "iso8601",
        "timeKey": "timestamp",
        "callerEncoder": "short",
        "callerKey": "caller"
	  }
	}`)

	var cfg zap.Config
	if err := json.Unmarshal(rawJSON, &cfg); err != nil {
		panic(err)
	}
	logger, err := cfg.Build()
	if err != nil {
		panic(err)
	}
	zap.ReplaceGlobals(logger)
	defer logger.Sync()
}

func main() {
	router := api.NewDPCAPIRouter()

	port := os.Getenv("API_PORT")
	if port == "" {
		port = "3000"
	}
	http.ListenAndServe(fmt.Sprintf(":%s", port), router)
}
