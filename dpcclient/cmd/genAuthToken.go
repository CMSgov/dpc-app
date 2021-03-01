package cmd

import (
	"fmt"
	"os"
	"path/filepath"
	"time"

	"github.com/CMSgov/dpc-app/dpcclient/lib"
	"github.com/spf13/cobra"
)

var (
	kid      string
	keyName  string
	macaroon string
)

var genAuthTokenCmd = &cobra.Command{
	Use:   "genAuthToken name -K -I -M [-k -t]",
	Short: "Generate an auth token for DPC API",
	Long: `This command generates a signed JWT token required to get an access_token 
for the DPC API. The command requires a name prefix argument, which it uses to name 
the file in which it stores the generated token. dpcclient appends a numeric unix 
timestamp and the '.jwt' extension to the name argument. If you supply the -t or 
--tokendir flag, it puts the file in that directory; otherwise, it puts the file 
in the working directory. The command also requires three flags (described below)
that tell it the names and locations of keys.

For example,

dpcclient genAuthToken myDPCAuthToken -t ./tokens -k ./keys -K myDPCSandboxKey -I <uuid> -M myDPCSandboxMacaroon 

creates a file named something similar to 'myDPCAuthToken-1577976362.jwt'' in the 'tokens' 
directory.`,
	Run: func(cmd *cobra.Command, args []string) {
		tokenPath := filepath.Join(tokenDir, fmt.Sprintf("%s-%d", args[0], time.Now().Unix()))

		pk, err := lib.ReadSmallFile(filepath.Join(keyDir, fmt.Sprintf("%s-private.pem", keyName)))
		if err != nil {
			fmt.Println(err)
			os.Exit(-1)
		}

		privateKey, _, err := lib.KeyFromPEM(pk)
		if err != nil {
			fmt.Println(err)
			os.Exit(-1)
		}

		m, err := lib.ReadSmallFile(filepath.Join(keyDir, macaroon))
		if err != nil {
			fmt.Println(err)
			os.Exit(-1)
		}
		ml := len(m) - 1
		if m[ml] == '\n' {
			m = m[:ml]
		}

		token, err := lib.GenerateAuthToken(privateKey, kid, []byte(m), baseURL)
		if err != nil {
			fmt.Println(err)
			os.Exit(-1)
		}
		fmt.Printf("\nAuth Token:\n%s\n", string(token))
		if err := lib.WriteSmallFile(tokenPath, []byte(token)); err != nil {
			fmt.Printf("could not save auth token to file at %s; %s", tokenPath, err.Error())
		}
	},
}

func markRequiredOrFail(key string, cmd *cobra.Command) {
	if err := cobra.MarkFlagRequired(cmd.Flags(), key); err != nil {
		fmt.Println("can't mark flag required? %s", err)
	}
}

func init() {
	rootCmd.AddCommand(genAuthTokenCmd)
	genAuthTokenCmd.Flags().StringVarP(&keyName, "keyName", "K", "", "the name prefix of the PKI keys")
	genAuthTokenCmd.Flags().StringVarP(&kid, "keyId", "I", "", "the DPC key ID of the PKI keys")
	genAuthTokenCmd.Flags().StringVarP(&macaroon, "macaroon", "M", "", "the name of a file containing a DPC client-token (macaroon)")

	markRequiredOrFail("keyName", genAuthTokenCmd)
	markRequiredOrFail("keyId", genAuthTokenCmd)
	markRequiredOrFail("macaroon", genAuthTokenCmd)
}
