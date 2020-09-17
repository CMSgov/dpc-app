package cmd

import (
	"fmt"
	"os"
	"path/filepath"
	"time"

	"github.com/CMSgov/dpc-app/dpcclient/lib"
	"github.com/spf13/cobra"
)

var authTokenPath string

// getAccessTokenCmd represents the getAccessToken command
var getAccessTokenCmd = &cobra.Command{
	Use:   "getAccessToken prefix -A authToken [-t directory]",
	Short: "Get a DPC API access token",
	Long: `This command GETs an access token for the DPC API. The command requires a  
prefix argument, which it uses to name the file in which it stores the generated token. 
dpcclient appends a numeric unix timestamp to form the file name. 
If you supply the -t or --tokendir flag, it puts the file in that directory; otherwise, it 
puts the file in the working directory. The command also requires the -A or --authToken flag, which
has the full file path to a previously generated auth token.

For example,

dpcclient getAccessToken dpcSandboxAccessToken -t ./tokens -A dpcSandboxAuthToken-1577976362

creates a file named something similar to 'dpcSandboxAccessToken-1577976362'' in the 'tokens' 
directory. (The timestamp portion of the name will be different with each invocation.)`,
	Run: func(cmd *cobra.Command, args []string) {
		tokenPath := filepath.Join(tokenDir, fmt.Sprintf("%s-%d", args[0], time.Now().Unix()))

		authTokenBytes, err := lib.ReadSmallFile(filepath.Join(tokenDir, authTokenPath))
		if err != nil {
			fmt.Println(err)
			os.Exit(-1)
		}

		token, err := lib.GetAccessToken(authTokenBytes, baseURL)
		if err != nil {
			fmt.Println(err)
			os.Exit(-1)
		}

		fmt.Printf("\nAccess Token:\n%s\n", token)
		if err = lib.WriteSmallFile(tokenPath, []byte(token)); err != nil {
			fmt.Printf("could not save access token to file at %s; %s", tokenPath, err.Error())
			os.Exit(-1)
		}
	},
}

func init() {
	rootCmd.AddCommand(getAccessTokenCmd)
	getAccessTokenCmd.Flags().StringVarP(&authTokenPath, "authToken", "A", "", "the full file path of the auth token")
	markRequiredOrFail("authToken", getAccessTokenCmd)
}
