package main

import (
	"fmt"
	"gopkg.in/yaml.v2"
	"io/ioutil"
	"net/http"
	"os"

	attribution "github.com/CMSgov/dpc/attribution/pkg"

	"go.uber.org/zap"
)

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
