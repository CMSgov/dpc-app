package client_test

import (
	"compress/gzip"
	"fmt"
	"io"
	"math/rand"
	"net/http"
	"net/http/httptest"
	"net/url"
	"os"
	"strconv"
	"strings"
	"testing"
	"time"

	"github.com/CMSgov/dpc/attribution/client"
	"github.com/CMSgov/dpc/attribution/conf"
	models "github.com/CMSgov/dpc/attribution/model/fhir"
	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

const (
	clientIDHeader    = "BULK-CLIENTID"
	jobIDHeader       = "BULK-JOBID"
	oldClientIDHeader = "DPC-JOBID"
	oldJobIDHeader    = "DPC-CMSID"

	validFakeB64CA   = "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSURJRENDQWdnQ0NRRFl4YzZmUmVKSXpEQU5CZ2txaGtpRzl3MEJBUXNGQURCU01Rc3dDUVlEVlFRR0V3SlYKVXpFTE1Ba0dBMVVFQ0F3Q1EwRXhFekFSQmdOVkJBY01Da3h2YzBGdVoyVnNaWE14RFRBTEJnTlZCQW9NQkVGRApUVVV4RWpBUUJnTlZCQU1NQ1d4dlkyRnNhRzl6ZERBZUZ3MHlNVEE0TURNeU1ERXpNREphRncweU1qQTRNRE15Ck1ERXpNREphTUZJeEN6QUpCZ05WQkFZVEFsVlRNUXN3Q1FZRFZRUUlEQUpEUVRFVE1CRUdBMVVFQnd3S1RHOXoKUVc1blpXeGxjekVOTUFzR0ExVUVDZ3dFUVVOTlJURVNNQkFHQTFVRUF3d0piRzlqWVd4b2IzTjBNSUlCSWpBTgpCZ2txaGtpRzl3MEJBUUVGQUFPQ0FROEFNSUlCQ2dLQ0FRRUF5eWx4Y0lzZUV0ckVqZ1I1QWNhK3ZPenpDYzRiCk91TUxaazVVcnJnQWI2Qy9yNGFWQzN1bVlOUTNtd2J5eGhwYjUwNUd4dHdDZ0hCR0tpMzB2Q1AzNWtqbjFyZG4Ka3B3V2J2aWJTR0NyV2xQc0hBSHFBSnhtaTFjL1ozVlFSRnh5bm52dXQ4L0ZEditpWW9BdVAzM3QyTzhWMTlaVgpJRzY0bTZVSFYzckh1V1RmMDN3Vk1IQzNNSlUxTjVZM0IvVCsxUjRwNGl4R2laRUhQVFJGMzlEenlDK1ZyYjRjCklCSXZxd3cyUk5MMHFZdGpRVmFMendOVmhrRXFTblhBODhTZ0p3TG1hTWh3dkczQktGQ245c2dXVDBoeFNGOTYKNWVvbVA3enU3QW5xbVFsUXBaUmEwR2cvS01IYytUYXMzN2dwL3MvTVZKSDlrNjNneXRvQ3l6U2ZYd0lEQVFBQgpNQTBHQ1NxR1NJYjNEUUVCQ3dVQUE0SUJBUUFhM1lZRCt4MjlMblllTXNnbGt0amlyQmZpMlJSeGdiQkh6VXQ5Ci9xa3dvVEQ4dGdEQnFRenhJNXhOY0QzblhPOUIwMTVBRms2ems5TUFLL2dwSkJTWk9HUGRhaHhBVXI3SGkvdWEKczQyNU56ZEczN29HaTkyaXZjLzc3M2t4YmxQK0xWczBjekMvWElqVExDaGtzVVN3ZEU3VTRiZGpMZk9Sa2p6VApVTnhkUktReHpCcElWL0FXRnk1VlMrQXZPaC82QngzRDlZdFVaMUZJTWlmdHUvbnptaENBMCszQURSMnlVTGo3CnFMblNpQXMrajh6R09iS1FtWFdMdGlUcjAraCtUeDdlS1Y2S1krN3pCNXJSY1hsZlFkMVNGWUlwR3E3Tk00eDgKTmVXZ0xoc3YrVFYyUjVEdC9oMGhvREMyNDFDRmdGTU9sb3ZHT2pNTUFqeUoxUkJMCi0tLS0tRU5EIENFUlRJRklDQVRFLS0tLS0K"
	validFakeB64Cert = "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSURXakNDQWtLZ0F3SUJBZ0lKQU85czJDTnBET1dlTUEwR0NTcUdTSWIzRFFFQkN3VUFNRkl4Q3pBSkJnTlYKQkFZVEFsVlRNUXN3Q1FZRFZRUUlEQUpEUVRFVE1CRUdBMVVFQnd3S1RHOXpRVzVuWld4bGN6RU5NQXNHQTFVRQpDZ3dFUVVOTlJURVNNQkFHQTFVRUF3d0piRzlqWVd4b2IzTjBNQjRYRFRJeE1EZ3dNekl3TVRNd00xb1hEVEl5Ck1EZ3dNekl3TVRNd00xb3dWakVMTUFrR0ExVUVCaE1DVlZNeEN6QUpCZ05WQkFnTUFrTkJNUk13RVFZRFZRUUgKREFwTWIzTkJibWRsYkdWek1SRXdEd1lEVlFRS0RBaEJRMDFGSUVGUVNURVNNQkFHQTFVRUF3d0piRzlqWVd4bwpiM04wTUlJQklqQU5CZ2txaGtpRzl3MEJBUUVGQUFPQ0FROEFNSUlCQ2dLQ0FRRUF1MEQydXVKaG5SMlE4V3JrCnR1SFBwS2JzdXR3RTdYWmRDYmRnZEFsYjBNUG1Tc1M3T1Z4SzhDT2JjaFJBUkZLVFpJcVdkeUc4ZVZkYkpnTUkKVklhRC81MXJIWHdIYU9nakcrUHlHclgxSEhraHVWUzkvZ2FvOG9HaWI5SGJKUmxxSTV1YVZvQmZFZHY1OFVWRgowZWdMSk5HMVJZcnEyUmNZcjVoTkY1QzEwRTdKYzZqQWhyL3FldU10bTNUS2ZNRzhFUEZRQUV2TEtjZ1l6L0xjCmhJOW5aV1N4aU9YckJveHlRcVZub0ZJSXdFN0FPbXRzTG4wOXB2amNuaGhPWUszaWFNbFQwWkZ4VXVIS1BhKzMKRkorL0RhaFNvNXFQay9zYTM5ditxNzJRSEt2eEl0R0pRSENXcVB6Smw4azJlc2NHYXFkTDhXbTZJaW5RWDRtagpHVnZtelFJREFRQUJveTh3TFRBckJnTlZIUkVFSkRBaWdnbHNiMk5oYkdodmMzU0NGV3h2WTJGc0xtRndhUzVrCmNHTXVZMjF6TG1kdmRqQU5CZ2txaGtpRzl3MEJBUXNGQUFPQ0FRRUFKY1pGdVBUVGxXRTNaVmZvZWhZV3Y2Ty8KeCtoYTdwODcwdHgwajd4cEtCR0pjcEZCZEJhYVoyS0FvaTNMU3lVKzE4KytzcnFXaGlXUUhxdFpMUlRPOVNDeQpjU2FTU1NCTllvRDF1cFU0ajB0U29Sdmg2QmQyczZiUHpnY2xPK3pkR1A3RnY2d0NhN0owVEhQWEJNZXVSQ0JNClBncFg1VFkzWWYxN0lnSFlBVldpSlRyN1YxRUV2S3M1ZVhwL2NtWTZPS1BJVGxDdjVJN1c5VlQvbFRFR1NBODEKTDQ0NDZBaGczRVhza2xGVnNkZmhjTXhFby9RWFFPeE5VSlkrNDJpN05BSENPVDJ0aGFYWTRkWW1CQW1tajdYVgpMa3M2c09jL083enhPUzF6cFNwNDJ0eHlVNjNUOUdUdnYwY0F6NTNaV01ndWc2WldCWThQdUlkMjhlTGNBQT09Ci0tLS0tRU5EIENFUlRJRklDQVRFLS0tLS0K"
	validFakeB64Key  = "LS0tLS1CRUdJTiBSU0EgUFJJVkFURSBLRVktLS0tLQpNSUlFb3dJQkFBS0NBUUVBdTBEMnV1SmhuUjJROFdya3R1SFBwS2JzdXR3RTdYWmRDYmRnZEFsYjBNUG1Tc1M3Ck9WeEs4Q09iY2hSQVJGS1RaSXFXZHlHOGVWZGJKZ01JVklhRC81MXJIWHdIYU9nakcrUHlHclgxSEhraHVWUzkKL2dhbzhvR2liOUhiSlJscUk1dWFWb0JmRWR2NThVVkYwZWdMSk5HMVJZcnEyUmNZcjVoTkY1QzEwRTdKYzZqQQpoci9xZXVNdG0zVEtmTUc4RVBGUUFFdkxLY2dZei9MY2hJOW5aV1N4aU9YckJveHlRcVZub0ZJSXdFN0FPbXRzCkxuMDlwdmpjbmhoT1lLM2lhTWxUMFpGeFV1SEtQYSszRkorL0RhaFNvNXFQay9zYTM5ditxNzJRSEt2eEl0R0oKUUhDV3FQekpsOGsyZXNjR2FxZEw4V202SWluUVg0bWpHVnZtelFJREFRQUJBb0lCQVFDMjhIbmNETzBtelVyYwp3UHpmdXU3Y3dvUUc3b3NWMzR4M3dLTEgycGpMOVllWXhtalBXbDZRQzRtRFEwdWlORFp4aElBSnRYam41ek5TCmlLWHJ4bHRSTUY4RXVEYVpCQ25BeHNxeDA5QzYxNURkK205L3JNd0QyQ2gxTEVYNEVjTkROSEx0VFk4VDZLQmcKV2Jnam9acTVodk9kRkdIcVk0a25qQmpOREFERWwzZHJVdTZDNzJrNmZCSmNYQ0NCb1lZMlFOVkxHcCtDVldrVwozSzJVMHZhckNkbFNFNnFndXJtYzdCMEtFTXpFejdaZUY2djBObFdrMnM5dkV4aEVjdTkyb3B5WkJpdFZXaWllCmo4dkxTMGpjV3NteXl2ZmVpZTh0a3M4bnJOdnR6ODBJeVJ0aVllazhpZzNKekllYnQ2SHQ2Z0N6c3NwS204THcKWGVDRXpVT2hBb0dCQU93Z1NncG44Mm9TdHZRdC9mcWpDcmdYMDFxY2kyNWxEa0UxZ2xqeEdHVisyMXdnRHdKZwpucGd6ZUkwbFUyRElFUU5XZllOdVFBVUVaV0tLK1VxcnB1d1RRVTJRRUpBUTlSOFVrWFlaODB0QVh1RGZDQi83ClBra21EbjA2aUZ5VXRvK0lIUFpvQkQrWWlvZ1dPTFNKVlA1UWxyN09ud3Z2ejdzU0l4a0NyMWdWQW9HQkFNc0QKcEdjVkNnME1TbEg3eHpMVFhwVGFWbndKZjNGODVvdXZxK1l1TUgyL2p3L1FzQTd3UElGWGxPMy8zdy9uUlRqVApyQXB3OFlXUTI1VE8wTnVyZTM5WGp1V0grSHZwQWc1QmhLcEtWLzNLQklaNEEzNWJPM2lQdG1xZEtVV2IyT0xLCm5kaTF3SzJUOUNQSWdtQThnMGNjOEpEN3hTekE4cENQMUVvZys0blpBb0dBSWliOWJvbmdmQndlMkN4NnlyQ2cKVVVZbzdMY3R0NWJvTytoSVpTTGgrM1FndUM5dTNGSXJQaXBicWxhV3U2M1VRQjVYWXhZa0xsV1hjR3hYYUVSeApqVDJ2dVU4NEJnNWQxZFA4d2c4NDBFNk1Lb3czdWFCMlB0QkJVajJRRVl2MDU4ZXhJTGFrdnFvS2gycG5ZejFsCnpLN05UdVhGdlUwL1IzaDFHUjM1VEkwQ2dZQVJkSiszS1V6eThFS1hxQUZwc2xqb2pabHdFdFQ5YWhMY29kOHkKN0hmSTZDYWg0bnl3M0NFbnNlTWhUNlhiVlRSVGZZZkdZZzJ5UVZGUUN6UlVIdnVBYUlQbDRub1FGV25TMWZsVgpOdzAzeWgzM2ZldzIraHN6ODQ5b3ZWaW1IbzlZZUxsM3Y3RHdlODg3SUd0dFlPYWN2N1ZEa2hKVEZjZ0ZmQWh4CjAwSXRpUUtCZ0R5T2ptZnVZaS8wbXY1NXdRamhqYnZQc0ZoVDFxZjFvVDJVZDdLZGhrQTFsQ1lGNkttWmVYbGEKSk9acmEwdGZzeis4a2dWcjkvdndGdjdEU2JNc1R1WGd2RWYzZ294dUpCdWgvd08zb1ZDbXVzZDR6dkg3YkpLVApTb085TnRPamFnY2Y3akNxV2tpcXF2T3BaRk5PRTdydHRCaHppbUJTWGRaMFBXK0NnQUZMCi0tLS0tRU5EIFJTQSBQUklWQVRFIEtFWS0tLS0tCg=="
)

type BfdTestSuite struct {
	suite.Suite
}

type BfdRequestTestSuite struct {
	BfdTestSuite
	bbClient *client.BfdClient
	ts       *httptest.Server
}

var (
	ts200, ts500 *httptest.Server
	now          = time.Now()
	nowFormatted = url.QueryEscape(now.Format(time.RFC3339Nano))
	since        = "gt2020-02-14"
	claimsDate   = client.ClaimsWindow{LowerBound: time.Date(2017, 12, 31, 0, 0, 0, 0, time.UTC),
		UpperBound: time.Date(2020, 12, 31, 0, 0, 0, 0, time.UTC)}
)

func (s *BfdTestSuite) SetupSuite() {
	_ = os.Setenv("ENV", "test")
	conf.NewConfig("../../configs")
}

func (s *BfdTestSuite) TearDownSuite() {
	os.Unsetenv("ENV")
}

func (s *BfdRequestTestSuite) SetupSuite() {
	ts200 = httptest.NewTLSServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		handlerFunc(w, r, false)
	}))

	ts500 = httptest.NewTLSServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		http.Error(w, "Some server error", http.StatusInternalServerError)
	}))
}

func (s *BfdRequestTestSuite) TearDownSuite() {
	os.Unsetenv("ENV")
}

func (s *BfdRequestTestSuite) BeforeTest(suiteName, testName string) {
	_ = os.Setenv("ENV", "test")
	conf.NewConfig("../../configs")
	if strings.Contains(testName, "500") {
		s.ts = ts500
	} else {
		s.ts = ts200
	}

	config := client.BfdConfig{
		BfdServer: s.ts.URL,
	}
	if bfdClient, err := client.NewBfdClient(config); err != nil {
		s.Fail("Failed to create BFD client", err)
	} else {
		s.bbClient = bfdClient
	}
}

/* Tests for creating client and other functions that don't make requests */
func (s *BfdTestSuite) TestNewBfdClientNoCertFile() {
	origCertFile := conf.GetAsString("bfd.clientCertFile")
	defer conf.SetEnv(s.T(), "bfd.clientCertFile", origCertFile)

	assert := assert.New(s.T())

	conf.UnsetEnv(s.T(), "bfd.clientCertFile")
	bbc, err := client.NewBfdClient(client.NewConfig("basePath"))
	assert.Nil(bbc)
	assert.EqualError(err, "failed to load BFD cert/key pair from file: open : no such file or directory")

	conf.SetEnv(s.T(), "bfd.clientCertFile", "foo.pem")
	bbc, err = client.NewBfdClient(client.NewConfig("basePath"))
	assert.Nil(bbc)
	assert.EqualError(err, "failed to load BFD cert/key pair from file: open foo.pem: no such file or directory")
}

func (s *BfdTestSuite) TestNewBfdClientInvalidCertFile() {
	origCertFile := conf.GetAsString("bfd.clientCertFile")
	defer conf.SetEnv(s.T(), "bfd.clientCertFile", origCertFile)

	assert := assert.New(s.T())

	conf.SetEnv(s.T(), "bfd.clientCertFile", "testdata/emptyFile.pem")
	bbc, err := client.NewBfdClient(client.NewConfig("basePath"))
	assert.Nil(bbc)
	assert.EqualError(err, "failed to load BFD cert/key pair from file: tls: failed to find any PEM data in certificate input")

	conf.SetEnv(s.T(), "bfd.clientCertFile", "testdata/badPublic.pem")
	bbc, err = client.NewBfdClient(client.NewConfig("basePath"))
	assert.Nil(bbc)
	assert.EqualError(err, "failed to load BFD cert/key pair from file: tls: failed to find any PEM data in certificate input")
}

func (s *BfdTestSuite) TestNewBfdClientNoKeyFile() {
	origKeyFile := conf.GetAsString("bfd.clientKeyFile")
	defer conf.SetEnv(s.T(), "bfd.clientKeyFile", origKeyFile)

	assert := assert.New(s.T())

	conf.UnsetEnv(s.T(), "bfd.clientKeyFile")
	bbc, err := client.NewBfdClient(client.NewConfig("basePath"))
	assert.Nil(bbc)
	assert.EqualError(err, "failed to load BFD cert/key pair from file: open : no such file or directory")

	conf.SetEnv(s.T(), "bfd.clientKeyFile", "foo.pem")
	bbc, err = client.NewBfdClient(client.NewConfig("basePath"))
	assert.Nil(bbc)
	assert.EqualError(err, "failed to load BFD cert/key pair from file: open foo.pem: no such file or directory")
}

func (s *BfdTestSuite) TestNewBfdClientInvalidKeyFile() {
	origKeyFile := conf.GetAsString("bfd.clientKeyFile")
	defer conf.SetEnv(s.T(), "bfd.clientKeyFile", origKeyFile)

	assert := assert.New(s.T())

	conf.SetEnv(s.T(), "bfd.clientKeyFile", "testdata/emptyFile.pem")
	bbc, err := client.NewBfdClient(client.NewConfig("basePath"))
	assert.Nil(bbc)
	assert.EqualError(err, "failed to load BFD cert/key pair from file: tls: failed to find any PEM data in key input")

	conf.SetEnv(s.T(), "bfd.clientKeyFile", "testdata/badPublic.pem")
	bbc, err = client.NewBfdClient(client.NewConfig("basePath"))
	assert.Nil(bbc)
	assert.EqualError(err, "failed to load BFD cert/key pair from file: tls: failed to find any PEM data in key input")
}

func (s *BfdTestSuite) TestNewBfdClientInvalidB64KeyFile() {
	origKeyFile := conf.GetAsString("bfd.clientKeyFile")
	defer conf.SetEnv(s.T(), "bfd.clientKeyFile", origKeyFile)
	defer conf.UnsetEnv(s.T(), "bfd.ca")
	defer conf.UnsetEnv(s.T(), "bfd.clientCert")
	defer conf.UnsetEnv(s.T(), "bfd.clientKey")

	assert := assert.New(s.T())

	conf.SetEnv(s.T(), "bfd.ca", validFakeB64CA)
	conf.SetEnv(s.T(), "bfd.clientCert", validFakeB64Cert)
	conf.SetEnv(s.T(), "bfd.clientKey", "789hfodw8f8ndf78bfdnyf98ew7f98dsnyfodsafyb9adsfd")

	bbc, err := client.NewBfdClient(client.NewConfig("basePath"))
	assert.Nil(bbc)
	assert.EqualError(err, "failed to parse BFD cert/key pair from env vars.: tls: failed to find any PEM data in key input")

	conf.SetEnv(s.T(), "bfd.clientkey", "")
	bbc, err = client.NewBfdClient(client.NewConfig("basePath"))
	assert.Nil(bbc)
	assert.EqualError(err, "only one of (DPC_BFD_CLIENTCERT , DPC_BFD_CLIENTKEY) was provided. Both or none are required")
}

func (s *BfdTestSuite) TestNewBfdClientInvalidB64ClientFile() {
	origKeyFile := conf.GetAsString("bfd.clientKeyFile")
	defer conf.SetEnv(s.T(), "bfd.clientKeyFile", origKeyFile)
	defer conf.UnsetEnv(s.T(), "bfd.ca")
	defer conf.UnsetEnv(s.T(), "bfd.clientCert")
	defer conf.UnsetEnv(s.T(), "bfd.clientKey")

	assert := assert.New(s.T())

	conf.SetEnv(s.T(), "bfd.ca", validFakeB64CA)
	conf.SetEnv(s.T(), "bfd.clientCert", "789hfodw8f8ndf78bfdnyf98ew7f98dsnyfodsafyb9adsfd")
	conf.SetEnv(s.T(), "bfd.clientKey", validFakeB64Key)

	bbc, err := client.NewBfdClient(client.NewConfig("basePath"))
	assert.Nil(bbc)
	assert.EqualError(err, "failed to parse BFD cert/key pair from env vars.: tls: failed to find any PEM data in certificate input")

	conf.SetEnv(s.T(), "bfd.clientkey", "")
	bbc, err = client.NewBfdClient(client.NewConfig("basePath"))
	assert.Nil(bbc)
	assert.EqualError(err, "only one of (DPC_BFD_CLIENTCERT , DPC_BFD_CLIENTKEY) was provided. Both or none are required")
}

func (s *BfdTestSuite) TestNewBfdClientInvalidB64CAFile() {
	orgCheckCert := conf.GetAsString("bfd.checkCert")
	origKeyFile := conf.GetAsString("bfd.clientKeyFile")
	defer conf.SetEnv(s.T(), "bfd.clientKeyFile", origKeyFile)
	defer conf.SetEnv(s.T(), "bfd.checkCert", orgCheckCert)
	defer conf.UnsetEnv(s.T(), "bfd.ca")
	defer conf.UnsetEnv(s.T(), "bfd.clientCert")
	defer conf.UnsetEnv(s.T(), "bfd.clientKey")

	assert := assert.New(s.T())
	conf.UnsetEnv(s.T(), "bfd.checkCert")
	conf.SetEnv(s.T(), "bfd.ca", "fadfadfadsfadsf")
	conf.SetEnv(s.T(), "bfd.clientCert", validFakeB64Cert)
	conf.SetEnv(s.T(), "bfd.clientKey", validFakeB64Key)

	bbc, err := client.NewBfdClient(client.NewConfig("basePath"))
	assert.Nil(bbc)
	assert.EqualError(err, "could not retrieve BFD CA cert pool: could not base64 decode BFD CA cert")

	conf.SetEnv(s.T(), "bfd.clientkey", "")
	bbc, err = client.NewBfdClient(client.NewConfig("basePath"))
	assert.Nil(bbc)
	assert.EqualError(err, "only one of (DPC_BFD_CLIENTCERT , DPC_BFD_CLIENTKEY) was provided. Both or none are required")
}

func (s *BfdTestSuite) TestNewBfdClientNoCAFile() {
	origCAFile := conf.GetAsString("bfd.clientCAFile")
	origCheckCert := conf.GetAsString("bfd.checkCert")
	defer func() {
		conf.SetEnv(s.T(), "bfd.clientCAFile", origCAFile)
		conf.SetEnv(s.T(), "bfd.checkCert", origCheckCert)
	}()

	assert := assert.New(s.T())

	conf.UnsetEnv(s.T(), "bfd.clientCAFile")
	conf.UnsetEnv(s.T(), "bfd.checkCert")
	bbc, err := client.NewBfdClient(client.NewConfig("basePath"))
	assert.Nil(bbc)
	assert.EqualError(err, "could not retrieve BFD CA cert pool: missing Base64 BFD CA cert (DPC_BFD_CA) or BFD CA file path (DPC_bfd_clientCAFile)")

	conf.SetEnv(s.T(), "bfd.clientCAFile", "foo.pem")
	bbc, err = client.NewBfdClient(client.NewConfig("basePath"))
	assert.Nil(bbc)
	assert.EqualError(err, "could not retrieve BFD CA cert pool: could not read BFD CA file: open foo.pem: no such file or directory")
}

func (s *BfdTestSuite) TestNewBfdClientInvalidCAFile() {
	origCAFile := conf.GetAsString("bfd.clientCAFile")
	origCheckCert := conf.GetAsString("bfd.checkCert")
	defer func() {
		conf.SetEnv(s.T(), "bfd.clientCAFile", origCAFile)
		conf.SetEnv(s.T(), "bfd.checkCert", origCheckCert)
	}()

	assert := assert.New(s.T())

	conf.SetEnv(s.T(), "bfd.clientCAFile", "testdata/emptyFile.pem")
	conf.UnsetEnv(s.T(), "bfd.checkCert")
	bbc, err := client.NewBfdClient(client.NewConfig(""))
	assert.Nil(bbc)
	assert.EqualError(err, "could not retrieve BFD CA cert pool: could not append CA certificate(s)")

	conf.SetEnv(s.T(), "bfd.clientCAFile", "testdata/badPublic.pem")
	bbc, err = client.NewBfdClient(client.NewConfig("basePath"))
	assert.Nil(bbc)
	assert.EqualError(err, "could not retrieve BFD CA cert pool: could not append CA certificate(s)")
}

func (s *BfdTestSuite) TestGetDefaultParams() {
	params := client.GetDefaultParams()
	assert.Equal(s.T(), "application/fhir+json", params.Get("_format"))
	assert.Equal(s.T(), "", params.Get("patient"))
	assert.Equal(s.T(), "", params.Get("beneficiary"))

}

/* Tests that make requests, using clients configured with the 200 response and 500 response httptest.Servers initialized in SetupSuite() */
func (s *BfdRequestTestSuite) TestGetPatient() {
	p, err := s.bbClient.GetPatient("012345", "543210", "A0000", "", now)
	assert.Nil(s.T(), err)
	assert.Equal(s.T(), 1, len(p.Entries))
	assert.Equal(s.T(), "20000000000001", p.Entries[0]["resource"].(map[string]interface{})["id"])
}

func (s *BfdRequestTestSuite) TestGetPatient_500() {
	p, err := s.bbClient.GetPatient("012345", "543210", "A0000", "", now)
	assert.Regexp(s.T(), `BFD request failed \d+ time\(s\) failed to get bundle response`, err.Error())
	assert.Nil(s.T(), p)
}
func (s *BfdRequestTestSuite) TestGetCoverage() {
	c, err := s.bbClient.GetCoverage("012345", "543210", "A0000", since, now)
	assert.Nil(s.T(), err)
	assert.Equal(s.T(), 3, len(c.Entries))
	assert.Equal(s.T(), "part-b-20000000000001", c.Entries[1]["resource"].(map[string]interface{})["id"])
}

func (s *BfdRequestTestSuite) TestGetCoverage_500() {
	c, err := s.bbClient.GetCoverage("012345", "543210", "A0000", since, now)
	assert.Regexp(s.T(), `BFD request failed \d+ time\(s\) failed to get bundle response`, err.Error())
	assert.Nil(s.T(), c)
}

func (s *BfdRequestTestSuite) TestGetExplanationOfBenefit() {
	e, err := s.bbClient.GetExplanationOfBenefit("012345", "543210", "A0000", "", now, client.ClaimsWindow{})
	assert.Nil(s.T(), err)
	assert.Equal(s.T(), 33, len(e.Entries))
	assert.Equal(s.T(), "carrier-10525061996", e.Entries[3]["resource"].(map[string]interface{})["id"])
}

func (s *BfdRequestTestSuite) TestGetExplanationOfBenefit_500() {
	e, err := s.bbClient.GetExplanationOfBenefit("012345", "543210", "A0000", "", now, client.ClaimsWindow{})
	assert.Regexp(s.T(), `BFD request failed \d+ time\(s\) failed to get bundle response`, err.Error())
	assert.Nil(s.T(), e)
}

func (s *BfdRequestTestSuite) TestGetMetadata() {
	m, err := s.bbClient.GetMetadata()
	assert.Nil(s.T(), err)
	assert.Contains(s.T(), m, `"resourceType": "CapabilityStatement"`)
	assert.NotContains(s.T(), m, "excludeSAMHSA=true")
}

func (s *BfdRequestTestSuite) TestGetMetadata_500() {
	p, err := s.bbClient.GetMetadata()
	assert.Regexp(s.T(), `BFD request failed \d+ time\(s\) failed to get response`, err.Error())
	assert.Equal(s.T(), "", p)
}

func (s *BfdRequestTestSuite) TestGetPatientByIdentifierHash() {
	p, err := s.bbClient.GetPatientByIdentifierHash("hashedIdentifier")
	assert.Nil(s.T(), err)
	assert.Contains(s.T(), p, `"id": "20000000000001"`)
}

func (s *BfdRequestTestSuite) TearDownAllSuite() {
	s.ts.Close()
}

func (s *BfdRequestTestSuite) TestValidateRequest() {
	old := conf.GetAsInt("bfd.clientPageSize")
	defer conf.SetEnv(s.T(), "bfd.clientPageSize", old)
	conf.SetEnv(s.T(), "bfd.clientPageSize", "0") // Need to ensure that requests do not have the _count parameter

	tests := []struct {
		name          string
		funcUnderTest func(bbClient *client.BfdClient, jobID, cmsID string) (interface{}, error)
		// Lighter validation checks since we've already thoroughly tested the methods in other tests
		payloadChecker func(t *testing.T, payload interface{})
		pathCheckers   []func(t *testing.T, req *http.Request)
	}{
		{
			"GetExplanationOfBenefit",
			func(bbClient *client.BfdClient, jobID, cmsID string) (interface{}, error) {
				return bbClient.GetExplanationOfBenefit("patient1", jobID, cmsID, since, now, client.ClaimsWindow{})
			},
			func(t *testing.T, payload interface{}) {
				result, ok := payload.(*models.Bundle)
				assert.True(t, ok)
				assert.NotEmpty(t, result.Entries)
			},
			[]func(*testing.T, *http.Request){
				sinceChecker,
				nowChecker,
				excludeSAMHSAChecker,
				noServiceDateChecker,
				noIncludeAddressFieldsChecker,
				includeTaxNumbersChecker,
			},
		},
		{
			"GetExplanationOfBenefitNoSince",
			func(bbClient *client.BfdClient, jobID, cmsID string) (interface{}, error) {
				return bbClient.GetExplanationOfBenefit("patient1", jobID, cmsID, "", now, client.ClaimsWindow{})
			},
			func(t *testing.T, payload interface{}) {
				result, ok := payload.(*models.Bundle)
				assert.True(t, ok)
				assert.NotEmpty(t, result.Entries)
			},
			[]func(*testing.T, *http.Request){
				noSinceChecker,
				nowChecker,
				excludeSAMHSAChecker,
				noServiceDateChecker,
				noIncludeAddressFieldsChecker,
				includeTaxNumbersChecker,
			},
		},
		{
			"GetExplanationOfBenefitWithUpperBoundServiceDate",
			func(bbClient *client.BfdClient, jobID, cmsID string) (interface{}, error) {
				return bbClient.GetExplanationOfBenefit("patient1", jobID, cmsID, since, now, client.ClaimsWindow{UpperBound: claimsDate.UpperBound})
			},
			func(t *testing.T, payload interface{}) {
				result, ok := payload.(*models.Bundle)
				assert.True(t, ok)
				assert.NotEmpty(t, result.Entries)
			},
			[]func(*testing.T, *http.Request){
				sinceChecker,
				nowChecker,
				excludeSAMHSAChecker,
				serviceDateUpperBoundChecker,
				noServiceDateLowerBoundChecker,
				noIncludeAddressFieldsChecker,
				includeTaxNumbersChecker,
			},
		},
		{
			"GetExplanationOfBenefitWithLowerBoundServiceDate",
			func(bbClient *client.BfdClient, jobID, cmsID string) (interface{}, error) {
				return bbClient.GetExplanationOfBenefit("patient1", jobID, cmsID, since, now, client.ClaimsWindow{LowerBound: claimsDate.LowerBound})
			},
			func(t *testing.T, payload interface{}) {
				result, ok := payload.(*models.Bundle)
				assert.True(t, ok)
				assert.NotEmpty(t, result.Entries)
			},
			[]func(*testing.T, *http.Request){
				sinceChecker,
				nowChecker,
				excludeSAMHSAChecker,
				serviceDateLowerBoundChecker,
				noServiceDateUpperBoundChecker,
				noIncludeAddressFieldsChecker,
				includeTaxNumbersChecker,
			},
		},
		{
			"GetExplanationOfBenefitWithLowerAndUpperBoundServiceDate",
			func(bbClient *client.BfdClient, jobID, cmsID string) (interface{}, error) {
				return bbClient.GetExplanationOfBenefit("patient1", jobID, cmsID, since, now, claimsDate)
			},
			func(t *testing.T, payload interface{}) {
				result, ok := payload.(*models.Bundle)
				assert.True(t, ok)
				assert.NotEmpty(t, result.Entries)
			},
			[]func(*testing.T, *http.Request){
				sinceChecker,
				nowChecker,
				excludeSAMHSAChecker,
				serviceDateLowerBoundChecker,
				serviceDateUpperBoundChecker,
				noIncludeAddressFieldsChecker,
				includeTaxNumbersChecker,
			},
		},
		{
			"GetPatient",
			func(bbClient *client.BfdClient, jobID, cmsID string) (interface{}, error) {
				return bbClient.GetPatient("patient2", jobID, cmsID, since, now)
			},
			func(t *testing.T, payload interface{}) {
				result, ok := payload.(*models.Bundle)
				assert.True(t, ok)
				assert.NotEmpty(t, result.Entries)
			},
			[]func(*testing.T, *http.Request){
				sinceChecker,
				nowChecker,
				noExcludeSAMHSAChecker,
				includeAddressFieldsChecker,
				noIncludeTaxNumbersChecker,
			},
		},
		{
			"GetPatientNoSince",
			func(bbClient *client.BfdClient, jobID, cmsID string) (interface{}, error) {
				return bbClient.GetPatient("patient2", jobID, cmsID, "", now)
			},
			func(t *testing.T, payload interface{}) {
				result, ok := payload.(*models.Bundle)
				assert.True(t, ok)
				assert.NotEmpty(t, result.Entries)
			},
			[]func(*testing.T, *http.Request){
				noSinceChecker,
				nowChecker,
				noExcludeSAMHSAChecker,
				includeAddressFieldsChecker,
				noIncludeTaxNumbersChecker,
			},
		},
		{
			"GetCoverage",
			func(bbClient *client.BfdClient, jobID, cmsID string) (interface{}, error) {
				return bbClient.GetCoverage("beneID1", jobID, cmsID, since, now)
			},
			func(t *testing.T, payload interface{}) {
				result, ok := payload.(*models.Bundle)
				assert.True(t, ok)
				assert.NotEmpty(t, result.Entries)
			},
			[]func(*testing.T, *http.Request){
				sinceChecker,
				nowChecker,
				noExcludeSAMHSAChecker,
				noIncludeAddressFieldsChecker,
				noIncludeTaxNumbersChecker,
			},
		},
		{
			"GetCoverageNoSince",
			func(bbClient *client.BfdClient, jobID, cmsID string) (interface{}, error) {
				return bbClient.GetCoverage("beneID1", jobID, cmsID, "", now)
			},
			func(t *testing.T, payload interface{}) {
				result, ok := payload.(*models.Bundle)
				assert.True(t, ok)
				assert.NotEmpty(t, result.Entries)
			},
			[]func(*testing.T, *http.Request){
				noSinceChecker,
				nowChecker,
				noExcludeSAMHSAChecker,
				noIncludeAddressFieldsChecker,
				noIncludeTaxNumbersChecker,
			},
		},
		{
			"GetPatientByIdentifierHash",
			func(bbClient *client.BfdClient, jobID, cmsID string) (interface{}, error) {
				return bbClient.GetPatientByIdentifierHash("hashedIdentifier")
			},
			func(t *testing.T, payload interface{}) {
				result, ok := payload.(string)
				assert.True(t, ok)
				assert.NotEmpty(t, result)
			},
			[]func(*testing.T, *http.Request){
				noExcludeSAMHSAChecker,
				noIncludeAddressFieldsChecker,
				noIncludeTaxNumbersChecker,
			},
		},
	}

	for _, tt := range tests {
		s.T().Run(tt.name, func(t *testing.T) {
			var jobID, cmsID string

			// GetPatientByIdentifierHash does not send in jobID and cmsID as arguments
			// so we DO NOT expected the associated headers to be set.
			// Only set the fields if we pass those parameters in.
			if tt.name != "GetPatientByIdentifierHash" {
				jobID = strconv.FormatUint(rand.Uint64(), 10)
				cmsID = strconv.FormatUint(rand.Uint64(), 10)
			}

			tsValidation := httptest.NewTLSServer(http.HandlerFunc(func(w http.ResponseWriter, req *http.Request) {
				o, _ := uuid.Parse(req.Header.Get("BFD-OriginalQueryId"))
				assert.NotNil(t, o)
				assert.Equal(t, "1", req.Header.Get("BFD-OriginalQueryCounter"))

				assert.Empty(t, req.Header.Get("keep-alive"))
				assert.Nil(t, req.Header.Values("X-Forwarded-Proto"))
				assert.Nil(t, req.Header.Values("X-Forwarded-Host"))

				assert.True(t, strings.HasSuffix(req.Header.Get("BFD-OriginalUrl"), req.URL.String()),
					"%s does not end with %s", req.Header.Get("BFD-OriginalUrl"), req.URL.String())
				assert.Equal(t, req.URL.RawQuery, req.Header.Get("BFD-OriginalQuery"))

				assert.Equal(t, jobID, req.Header.Get(jobIDHeader))
				assert.Equal(t, cmsID, req.Header.Get(clientIDHeader))
				assert.Empty(t, req.Header.Get(oldJobIDHeader))
				assert.Empty(t, req.Header.Get(oldClientIDHeader))

				assert.Equal(t, "mbi", req.Header.Get("IncludeIdentifiers"))

				// Verify that we have compression enabled on these HTTP requests.
				// NOTE: This header should not be explicitly set on the client. It should be added by the http.Transport.
				// Details: https://golang.org/src/net/http/transport.go#L2432
				assert.Equal(t, "gzip", req.Header.Get("Accept-Encoding"))

				for _, checker := range tt.pathCheckers {
					checker(t, req)
				}

				handlerFunc(w, req, true)
			}))
			defer tsValidation.Close()

			config := client.BfdConfig{
				BfdServer: tsValidation.URL,
			}
			bbClient, err := client.NewBfdClient(config)
			if err != nil {
				assert.FailNow(t, err.Error())
			}

			data, err := tt.funcUnderTest(bbClient, jobID, cmsID)
			if err != nil {
				assert.FailNow(t, err.Error())
			}

			tt.payloadChecker(t, data)
		})
	}
}

func handlerFunc(w http.ResponseWriter, r *http.Request, useGZIP bool) {
	path := r.URL.Path
	var (
		file *os.File
		err  error
	)
	if strings.Contains(path, "Coverage") {
		file, err = os.Open("testdata/synthetic_beneficiary_data/Coverage")
	} else if strings.Contains(path, "ExplanationOfBenefit") {
		file, err = os.Open("testdata/synthetic_beneficiary_data/ExplanationOfBenefit")
	} else if strings.Contains(path, "metadata") {
		file, err = os.Open("./testdata/Metadata.json")
	} else if strings.Contains(path, "Patient") {
		file, err = os.Open("testdata/synthetic_beneficiary_data/Patient")
	} else {
		err = fmt.Errorf("unrecognized path supplied %s", path)
		http.Error(w, err.Error(), http.StatusBadRequest)
	}

	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	defer file.Close()

	w.Header().Set("Content-Type", r.URL.Query().Get("_format"))

	if useGZIP {
		w.Header().Set("Content-Encoding", "gzip")
		gz := gzip.NewWriter(w)
		defer gz.Close()
		if _, err := io.Copy(gz, file); err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
		}
	} else {
		if _, err := io.Copy(w, file); err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
		}
	}
}

func noSinceChecker(t *testing.T, req *http.Request) {
	assert.NotContains(t, req.URL.String(), "_lastUpdated=gt")
}
func sinceChecker(t *testing.T, req *http.Request) {
	assert.Contains(t, req.URL.String(), fmt.Sprintf("_lastUpdated=%s", since))
}
func noExcludeSAMHSAChecker(t *testing.T, req *http.Request) {
	assert.NotContains(t, req.URL.String(), "excludeSAMHSA=true")
}
func excludeSAMHSAChecker(t *testing.T, req *http.Request) {
	assert.Contains(t, req.URL.String(), "excludeSAMHSA=true")
}
func nowChecker(t *testing.T, req *http.Request) {
	assert.Contains(t, req.URL.String(), fmt.Sprintf("_lastUpdated=le%s", nowFormatted))
}
func noServiceDateChecker(t *testing.T, req *http.Request) {
	assert.Empty(t, req.URL.Query()["service-date"])
}
func serviceDateUpperBoundChecker(t *testing.T, req *http.Request) {
	// We expect that service date only contains YYYY-MM-DD
	assert.Contains(t, req.URL.Query()["service-date"], fmt.Sprintf("le%s", claimsDate.UpperBound.Format("2006-01-02")))
}
func noServiceDateUpperBoundChecker(t *testing.T, req *http.Request) {
	// We expect that service date only contains YYYY-MM-DD
	assert.NotContains(t, req.URL.Query()["service-date"], fmt.Sprintf("le%s", claimsDate.UpperBound.Format("2006-01-02")))
}
func serviceDateLowerBoundChecker(t *testing.T, req *http.Request) {
	// We expect that service date only contains YYYY-MM-DD
	assert.Contains(t, req.URL.Query()["service-date"], fmt.Sprintf("ge%s", claimsDate.LowerBound.Format("2006-01-02")))
}
func noServiceDateLowerBoundChecker(t *testing.T, req *http.Request) {
	// We expect that service date only contains YYYY-MM-DD
	assert.NotContains(t, req.URL.Query()["service-date"], fmt.Sprintf("ge%s", claimsDate.LowerBound.Format("2006-01-02")))
}
func noIncludeAddressFieldsChecker(t *testing.T, req *http.Request) {
	assert.Empty(t, req.Header.Get("IncludeAddressFields"))
}
func includeAddressFieldsChecker(t *testing.T, req *http.Request) {
	assert.Equal(t, "true", req.Header.Get("IncludeAddressFields"))
}
func noIncludeTaxNumbersChecker(t *testing.T, req *http.Request) {
	assert.Empty(t, req.Header.Get("IncludeTaxNumbers"))
}
func includeTaxNumbersChecker(t *testing.T, req *http.Request) {
	assert.Equal(t, "true", req.Header.Get("IncludeTaxNumbers"))
}

func TestBfdTestSuite(t *testing.T) {
	suite.Run(t, new(BfdTestSuite))
	suite.Run(t, new(BfdRequestTestSuite))
}
