package main

import (
	"encoding/json"
	"fmt"
	"net/http"
	"os"

	attribution "github.com/CMSgov/dpc/pkg/attribution"

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
	router := attribution.NewDPCAttributionRouter()

	port := os.Getenv("ATTRIBUTION_PORT")
	if port == "" {
		port = "3001"
	}
	http.ListenAndServe(fmt.Sprintf(":%s", port), router)
}
