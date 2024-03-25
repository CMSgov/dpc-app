package main

import (
	"os"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func TestGenerateConfirmationFileName_Test(t *testing.T) {
	responseFileName := "bfdeft01/dpc/in/T.NGD.DPC.RSP.D240123.T1122001.IN"
	now, _ := time.Parse("2006-01-02 15:04:05", "2010-01-01 12:00:00")
	confirmationFileName := GenerateConfirmationFileName(responseFileName, now)

	assert.Equal(t, "bfdeft01/dpc/out/T#EFT.ON.DPC.NGD.CONF.D100101.T1200000", confirmationFileName)
}

func TestGenerateConfirmationFileName_Prod(t *testing.T) {
	testEnv := os.Getenv("ENV")

	os.Setenv("ENV", "prod")
	defer os.Setenv("ENV", testEnv)

	responseFileName := "bfdeft01/dpc/in/P.NGD.DPC.RSP.D240123.T1122001.IN"
	now, _ := time.Parse("2006-01-02 15:04:05", "2010-01-01 12:00:00")
	confirmationFileName := GenerateConfirmationFileName(responseFileName, now)

	assert.Equal(t, "bfdeft01/dpc/out/P#EFT.ON.DPC.NGD.CONF.D100101.T1200000", confirmationFileName)
}
