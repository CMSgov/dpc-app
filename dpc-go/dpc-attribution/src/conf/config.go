package conf

import (
	"log"
	"os"
	"path/filepath"
	"runtime"
	"strings"
	"testing"

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
	config.SetDefault("DecryptedDir", getDecryptedDir())
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

// SetEnv is a public function that adds key values into conf. This function should only be used
// either in this package itself or testing. Protect parameter is type *testing.T, and it's there
// to ensure developers knowingly use it in the appropriate circumstances.
// This function will most likely become a private function in later versions of the package.
func SetEnv(protect *testing.T, key string, value interface{}) {
	config.Set(key, value)
}

type nullValue struct{}

var null = nullValue{}

// UnsetEnv is a public function that "unsets" a variable. Like SetEnv, this should only be used
// either in this package itself or testing.
// This function will most likely become a private function in later versions of the package.
func UnsetEnv(protect *testing.T, key string) error {
	config.Set(key, null)
	// Unset environment variable too to ensure that viper does not attempt
	// to retrieve it from the os.
	return os.Unsetenv(key)
}

func getDecryptedDir() string {
	var (
		_, b, _, _ = runtime.Caller(0)
		basePath   = filepath.Dir(b)
	)
	return strings.Replace(basePath, "conf", "shared_files/decrypted/", 1)
}
