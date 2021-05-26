package conf

import (
	"log"
	"os"
	"strings"

	"github.com/spf13/viper"
)

var config viper.Viper

func addPaths(dir ...string) {
	if len(dir) > 0 {
		for _, v := range dir {
			config.AddConfigPath(v)
		}
	}
}

// NewConfig function sets up the viper config and optionally can pass in a list of different directories to search for configs
func NewConfig(dir ...string) {
	config = *viper.New()
	addPaths(dir...)
	config.AddConfigPath("../configs")
	config.SetConfigName("base")
	config.SetConfigType("yml")
	config.AutomaticEnv()
	config.SetEnvPrefix("DPC")
	config.SetEnvKeyReplacer(strings.NewReplacer(".", "_"))
	if err := config.ReadInConfig(); err != nil {
		log.Fatalf("Failed to setup config %s", err)
	}

	env, found := os.LookupEnv("ENV")
	if found {
		config.SetConfigName(env)
		config.SetConfigType("yml")
		if err := config.MergeInConfig(); err != nil {
			log.Fatalf("Failed to merge env config %s", err)
		}
	}

	additionalConfig, found := os.LookupEnv("ADDITIONAL_CONFIG")
	if found {
		config.SetConfigFile(additionalConfig)
		if err := config.MergeInConfig(); err != nil {
			log.Fatalf("Failed to merge additional config %s", err)
		}
	}
}

// GetAsString is a function to retrieve the value from the viper config as a string
// allowing to also pass in a default value
func GetAsString(key string, dv ...string) string {
	val := config.GetString(key)
	if val == "" && len(dv) > 0 {
		return dv[0]
	}
	return val
}

// GetAsInt is a function to retrieve the value from the viper config as an int
// allowing to also pass in a default value
func GetAsInt(key string, dv ...int) int {
	val := config.GetInt(key)
	if val == 0 && len(dv) > 0 {
		return dv[0]
	}
	return val
}

// Get is a function to retrieve the value from the viper config as an interface{}
func Get(key string) interface{} {
	return config.Get(key)
}
