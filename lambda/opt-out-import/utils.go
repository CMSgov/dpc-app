package main

import (
	"fmt"
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

func GenerateConfirmationFileName() string {
	currentDateTime := time.Now().Format("D060102.T150405")
	return fmt.Sprintf("P#EFT.ON.DPC.NGD.CONF.%s", currentDateTime)
}
