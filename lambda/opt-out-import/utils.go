package main

import (
	"fmt"
	"os"
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

func GenerateConfirmationFileName(responseFile string, dateTime time.Time) string {
	prefix := "T"
	if os.Getenv("ENV") == "prod" {
		prefix = "P"
	}
	path := strings.Split(responseFile, "in")
	filename := fmt.Sprintf("%s#EFT.ON.DPC.NGD.CONF.%s", prefix, dateTime.Format("D060102.T1504050"))
	return fmt.Sprintf("%sout/%s", path[0], filename)
}
