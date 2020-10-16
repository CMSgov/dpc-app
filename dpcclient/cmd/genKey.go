package cmd

import (
	"fmt"
	"path/filepath"

	"github.com/CMSgov/dpc-app/dpcclient/lib"
	"github.com/spf13/cobra"
)

var genKeyCmd = &cobra.Command{
	Use:   "genKey name [flags]",
	Args:  cobra.ExactArgs(1),
	Short: "Generate a private/public RSA key pair",
	Long: `This command generates the RSA PKI key pair needed for the DPC API. 
The command requires a name prefix, which it uses to name the files in which 
it stores the generated keys. If you supply a -k or --keydir flag, it puts files 
in that directory; otherwise, it puts them in the working directory. For example,

dpcclient genKey myDPCKey -k keys 

creates two files, myDPCKey-private.pem and myDPC-public.pem, in the 'keys'
directory.`,
	Run: func(cmd *cobra.Command, args []string) {
		private, public, err := lib.GenRSAKeyPair()
		if err != nil {
			fmt.Println("Unable to generate key pair ", err)
		}
		if err := lib.SaveDPCKeyPair(filepath.Join(keyDir, args[0]), private, public); err != nil {
			fmt.Println("Bad things happened to good keys ", err)
		}
	},
}

func init() {
	rootCmd.AddCommand(genKeyCmd)
}
