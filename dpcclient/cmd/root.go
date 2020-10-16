package cmd

import (
	"fmt"
	"os"

	"github.com/spf13/cobra"

	"github.com/mitchellh/go-homedir"
	"github.com/spf13/viper"
)

var (
	keyDir      string
	tokenDir    string
	environment string
	baseURL     string
	cfgFile     string
)

// rootCmd represents the base dpcclient
var rootCmd = &cobra.Command{
	Use: "dpcclient",
	PersistentPreRun: func(cmd *cobra.Command, args []string) {
		if keyDir == "" {
			keyDir = viper.GetString("keydir")
		}
		if keyDir != "" {
			validateDirectory(keyDir)
		}
		if tokenDir == "" {
			tokenDir = viper.GetString("tokendir")
		}
		if tokenDir != "" {
			validateDirectory(tokenDir)
		}
		// alas, cobra breaks vipers config-over-default priority, so we have to handle default value setting.
		// https://github.com/spf13/viper/issues/671
		if environment == "" {
			environment = viper.GetString("env")
		}
		if environment == "" {
			environment = "sandbox"
		}
		validateEnvironment()
		baseURL = fmt.Sprintf("https://%s.dpc.cms.gov/api/v1", environment)
	},
	Short: "A command line tool to explore CMS's DPC API",
	Long: `
dpcclient helps you explore CMS's DPC API by generating the things you need 
-- a PKI key pair, an auth token and an access token -- to access it. This tool 
is not intended to be a full-fledged client, but to help you along the way in 
writing one of your own.

You invoke dpcclient with a command, its argument[s], and any appropriate flags:

dpcclient command argument flags

In addition to these builtin help messages, the README has more details and 
examples for every command.
`,
}

func validateDirectory(name string) {
	if name != "" {
		dir, err := os.Stat(name)
		if err != nil {
			fmt.Printf("directory '%s' not found or invalid? %s\n", name, err)
			os.Exit(1)
		}
		if !dir.Mode().IsDir() {
			fmt.Printf("file '%s' is not a directory? %s\n", name, err)
			os.Exit(1)
		}
	}
}

func validateEnvironment() {
	switch environment {
	case "dev", "test", "sandbox":
		fmt.Printf("Base domain: %s.dpc.cms.gov", environment)
	default:
		fmt.Printf("Invalid environment: %s. Should be one of 'dev', 'test' or 'sandbox'", environment)
		os.Exit(1)
	}
}

// Execute runs the command line
func Execute() {
	if err := rootCmd.Execute(); err != nil {
		if err.Error() != "subcommand is required" {
			fmt.Println(err)
		}
		os.Exit(1)
	}
}

func init() {
	cobra.OnInitialize(initConfig)

	// global flags that apply to all commands
	rootCmd.PersistentFlags().StringVarP(&keyDir, "keydir", "k", "", "directory used to store keys (defaults to working directory)")
	rootCmd.PersistentFlags().StringVarP(&tokenDir, "tokendir", "t", "", "directory used to store tokens (defaults to working directory)")
	// do not set default value here, as a default value here will always override the config file. https://github.com/spf13/viper/issues/671
	rootCmd.PersistentFlags().StringVarP(&environment, "env", "e", "", "the target environment name (defaults to 'sandbox')")
	rootCmd.PersistentFlags().StringVar(&cfgFile, "config", "", "config file (defaults to $HOME/.dpcclient.yaml)")
	bindViperOrExit("keydir")
	bindViperOrExit("tokendir")
	bindViperOrExit("env")
}

func bindViperOrExit(key string) {
	if err := viper.BindPFlag(key, rootCmd.PersistentFlags().Lookup(key)); err != nil {
		fmt.Println("Internal configuration error: ", err)
		os.Exit(1)
	}
}

// initConfig reads in config file and ENV variables if set.
func initConfig() {
	if cfgFile != "" {
		// Use config file from the flag.
		viper.SetConfigFile(cfgFile)
	} else {
		// Find home directory.
		home, err := homedir.Dir()
		if err != nil {
			fmt.Println(err)
			os.Exit(1)
		}

		// Search for config in home directory with name ".dpcclient" (without extension).
		viper.AddConfigPath(home)
		viper.SetConfigName(".dpcclient")
	}

	viper.AutomaticEnv() // read in environment variables that match

	// If a config file is found, read it in.
	if err := viper.ReadInConfig(); err == nil {
		fmt.Println("Using config file:", viper.ConfigFileUsed())
	}
}
