package conf

import (
	"github.com/spf13/viper"
	"log"
	"os"
	"strings"
)

var config viper.Viper

func NewConfig(dir ...string) {
	config = *viper.New()
	config.AddConfigPath("../configs")
	if len(dir) > 0 {
		for _, v := range dir {
			config.AddConfigPath(v)
		}
	}

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

func GetAsString(key string, dv ...string) string {
	val := config.GetString(key)
	if val == "" && len(dv) > 0 {
		return dv[0]
	}
	return val
}

func Get(key string) interface{} {
	return config.Get(key)
}
