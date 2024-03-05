package main

import (
	"fmt"
	"strings"
	"time"
)

func ConvertDt(s string) (time.Time, error) {
	if s == "" {
		return time.Time{}, nil
	}
	t, err := time.Parse("20060102", s)
	if err != nil || t.IsZero() {
		return t, err
	}
	return t, nil
}

func GenerateConfirmationFileName(responseFile string) string {
	path := strings.Split(responseFile, "/")
	currentDateTime := time.Now().Format("D060102.T150405")
	filename := fmt.Sprintf("P#EFT.ON.DPC.NGD.CONF.%s", currentDateTime)
	return fmt.Sprintf("%s/%s/out/%s", path[0], path[1], filename)
}
