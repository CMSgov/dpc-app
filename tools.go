// +build tools

package main

import (
	// bcda/worker/ssas dependencies
	_ "github.com/BurntSushi/toml"
	_ "github.com/go-delve/delve/cmd/dlv"
	_ "github.com/howeyc/fsnotify"
	_ "github.com/mattn/go-colorable"
)
