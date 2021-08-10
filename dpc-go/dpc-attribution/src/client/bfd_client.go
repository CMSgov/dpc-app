package client

import (
	"context"
	"crypto/sha256"
	"crypto/tls"
	"crypto/x509"
	"encoding/hex"
	"fmt"
	"github.com/sjsdfg/common-lang-in-go/StringUtils"
	"io/ioutil"
	"net/http"
	"net/url"
	"path/filepath"
	"strings"
	"time"

	b64 "encoding/base64"
	"github.com/CMSgov/dpc/attribution/client/fhir"
	"github.com/CMSgov/dpc/attribution/conf"
	"github.com/CMSgov/dpc/attribution/logger"
	models "github.com/CMSgov/dpc/attribution/model/fhir"
	"github.com/cenkalti/backoff/v4"
	"github.com/go-chi/chi/middleware"
	"github.com/google/uuid"
	"github.com/pkg/errors"
	"go.uber.org/zap"
	"go.uber.org/zap/zapcore"
	"golang.org/x/crypto/pbkdf2"
)

const (
	clientIDHeader = "BULK-CLIENTID"
	jobIDHeader    = "BULK-JOBID"
)

// BfdConfig holds the configuration settings needed to create a BfdClient
type BfdConfig struct {
	BfdServer   string
	BfdBasePath string
}

// NewConfig generates a new BfdConfig using various environment variables.
func NewConfig(basePath string) BfdConfig {
	return BfdConfig{
		BfdServer:   conf.GetAsString("bfd.serverLocation"),
		BfdBasePath: basePath,
	}
}

// ClaimsWindow is the time frame for the claims window
type ClaimsWindow struct {
	LowerBound time.Time
	UpperBound time.Time
}

// APIClient is an interface for the API Client
type APIClient interface {
	GetExplanationOfBenefit(patientID, jobID, cmsID, since string, transactionTime time.Time, claimsWindow ClaimsWindow) (*models.Bundle, error)
	GetPatient(patientID, jobID, cmsID, since string, transactionTime time.Time) (*models.Bundle, error)
	GetCoverage(beneficiaryID, jobID, cmsID, since string, transactionTime time.Time) (*models.Bundle, error)
	GetPatientByIdentifierHash(hashedIdentifier string) (string, error)
}

// BfdClient is an interface for the BFD Client
type BfdClient struct {
	client fhir.Client

	maxTries      uint64
	retryInterval time.Duration

	bfdServer   string
	bfdBasePath string
}

// Ensure BfdClient satisfies the interface
var _ APIClient = &BfdClient{}

// Set Logger
var log = logger.WithContext(context.Background())

// NewBfdClient creates a new BFD Client
func NewBfdClient(config BfdConfig) (*BfdClient, error) {
	pageSize := conf.GetAsInt("bfd.clientPageSize", 0)
	cert, err := getClientCert()
	if err != nil {
		return nil, err
	}

	tlsConfig := &tls.Config{Certificates: []tls.Certificate{cert}, MinVersion: tls.VersionTLS12}

	if strings.ToLower(conf.GetAsString("bfd.checkCert")) != "false" {
		caPool, err := getCaCertPool()
		if err != nil {
			return nil, errors.Wrap(err, "could not retrieve BFD CA cert pool")
		}
		tlsConfig.RootCAs = caPool
	} else {
		tlsConfig.InsecureSkipVerify = true
		log.Warn("BFD certificate check disabled")
	}

	transport := &http.Transport{
		TLSClientConfig: tlsConfig,
		// Ensure that we have compression enabled. This allows the transport to request for gzip content
		// and handle the decompression transparently.
		// See: https://golang.org/src/net/http/transport.go?s=3396:10950#L182 for more information
		DisableCompression: false,
	}
	timeout := conf.GetAsInt("bfd.timeoutMS", 500)

	hl := &httpLogger{transport, log}
	httpClient := &http.Client{Transport: hl, Timeout: time.Duration(timeout) * time.Millisecond}
	client := fhir.NewClient(httpClient, pageSize)
	maxTries := uint64(conf.GetAsInt("bfd.requestMaxTries", 3))
	retryInterval := time.Duration(conf.GetAsInt("bfd.requestRetryIntervalMS", 1000)) * time.Millisecond
	return &BfdClient{client, maxTries, retryInterval, config.BfdServer, config.BfdBasePath}, nil
}

func getClientCert() (tls.Certificate, error) {
	hasB64 := StringUtils.IsNotBlank(conf.GetAsString("BFD.CLIENTCERT")) || StringUtils.IsNotBlank(conf.GetAsString("BFD.CLIENTKEY"))
	hasFilePaths := StringUtils.IsNotBlank(conf.GetAsString("bfd.clientCertFile")) || StringUtils.IsNotBlank(conf.GetAsString("bfd.clientKeyFile"))

	if hasB64 {
		if StringUtils.IsBlank(conf.GetAsString("BFD.CLIENTCERT")) || StringUtils.IsBlank(conf.GetAsString("BFD.CLIENTKEY")) {
			return tls.Certificate{}, errors.New("only one of (DPC_BFD_CLIENTCERT , DPC_BFD_CLIENTKEY) was provided. Both or none are required.")
		}
		log.Info("Using BFD clients certs found in env variables (DPC_BFD_CLIENTCERT , DPC_BFD_CLIENTKEY)")
		return getClientCertFromEnv()
	} else if hasFilePaths {
		log.Info("Using BFD clients certs file paths")
		return getClientCertFromFiles()
	} else {
		return tls.Certificate{}, errors.New("missing Base64 BFD Certs (DPC_BFD_CLIENTCERT , DPC_BFD_CLIENTKEY) or BFD Cert file paths (DPC_bfd_clientCertFile , DPC_bfd_clientKeyFile)")
	}
}

func getCaCertPool() (*x509.CertPool, error) {
	hasB64 := StringUtils.IsNotBlank(conf.GetAsString("BFD.CA"))
	hasFilePath := StringUtils.IsNotBlank(conf.GetAsString("bfd.clientCAFile"))

	if hasB64 {
		log.Info("Using BFD CA cert found in env variables (DPC_BFD_CA)")
		return getCAPoolFromEnv()
	} else if hasFilePath {
		log.Info("Using BFD CA cert file path")
		return getCAPoolFromFile()
	} else {
		return nil, errors.New("missing Base64 BFD CA cert (DPC_BFD_CA) or BFD CA file path (DPC_bfd_clientCAFile)")
	}
}

// getCAPoolFromFile retrieves the bfd ca file from a local path
func getCAPoolFromFile() (*x509.CertPool, error) {
	caFile := conf.GetAsString("bfd.clientCAFile")
	caCert, err := ioutil.ReadFile(filepath.Clean(caFile))
	if err != nil {
		return nil, errors.Wrap(err, "could not read BFD CA file")
	}
	caCertPool := x509.NewCertPool()
	if ok := caCertPool.AppendCertsFromPEM(caCert); !ok {
		return nil, errors.New("could not append CA certificate(s)")
	}
	return caCertPool, nil
}

func getClientCertFromFiles() (tls.Certificate, error) {
	certFile := conf.GetAsString("bfd.clientCertFile")
	keyFile := conf.GetAsString("bfd.clientKeyFile")

	cert, err := tls.LoadX509KeyPair(certFile, keyFile)
	if err != nil {
		return tls.Certificate{}, errors.Wrap(err, "failed to load BFD cert/key pair from file")
	}
	return cert, nil
}

func getCAPoolFromEnv() (*x509.CertPool, error) {
	caB, err := b64.StdEncoding.DecodeString(conf.GetAsString("BFD.CA"))
	if err != nil {
		return nil, errors.New("could not base64 decode BFD CA cert")
	}

	caCertStr := string(caB)
	certPool := x509.NewCertPool()
	ok := certPool.AppendCertsFromPEM([]byte(caCertStr))
	if !ok {
		return nil, errors.New("failed to parse BFD ca cert")
	}
	return certPool, nil
}

func getClientCertFromEnv() (tls.Certificate, error) {
	crtB, err := b64.StdEncoding.DecodeString(conf.GetAsString("BFD.CLIENTCERT"))
	if err != nil {
		return tls.Certificate{}, errors.Wrap(err, "could not base64 decode BFD cert")
	}
	keyB, err := b64.StdEncoding.DecodeString(conf.GetAsString("BFD.CLIENTKEY"))
	if err != nil {
		return tls.Certificate{}, errors.Wrap(err, "could not base64 decode BFD cert key")
	}

	clCertStr := string(crtB)
	clKeyStr := string(keyB)

	crt, err := tls.X509KeyPair([]byte(clCertStr), []byte(clKeyStr))
	if err != nil {
		return tls.Certificate{}, errors.Wrap(err, "failed to parse BFD cert/key pair from env vars.")
	}

	return crt, nil
}

// GetPatient is a method to get patient data using a patient ID
func (bfd *BfdClient) GetPatient(patientID, jobID, cmsID, since string, transactionTime time.Time) (*models.Bundle, error) {
	header := make(http.Header)
	header.Add("IncludeAddressFields", "true")
	params := GetDefaultParams()
	params.Set("_id", patientID)
	updateParamWithLastUpdated(&params, since, transactionTime)

	u, err := bfd.getURL("Patient", params)
	if err != nil {
		return nil, err
	}

	return bfd.getBundleData(u, jobID, cmsID, header)
}

// GetPatientByIdentifierHash is a method to get patient data using a hashed MBI
func (bfd *BfdClient) GetPatientByIdentifierHash(hashedIdentifier string) (string, error) {
	params := GetDefaultParams()

	// FHIR spec requires a FULLY qualified namespace so this is in fact the argument, not a URL
	params.Set("identifier", fmt.Sprintf("https://bluebutton.cms.gov/resources/identifier/%s|%v", "mbi-hash", hashedIdentifier))

	u, err := bfd.getURL("Patient", params)
	if err != nil {
		return "", err
	}

	return bfd.getRawData(u)
}

// GetCoverage is a method to get coverage data using a bene ID
func (bfd *BfdClient) GetCoverage(beneficiaryID, jobID, cmsID, since string, transactionTime time.Time) (*models.Bundle, error) {
	params := GetDefaultParams()
	params.Set("beneficiary", beneficiaryID)
	updateParamWithLastUpdated(&params, since, transactionTime)

	u, err := bfd.getURL("Coverage", params)
	if err != nil {
		return nil, err
	}

	return bfd.getBundleData(u, jobID, cmsID, nil)
}

// GetExplanationOfBenefit is a method to get EOB data using a patient ID
func (bfd *BfdClient) GetExplanationOfBenefit(patientID, jobID, cmsID, since string, transactionTime time.Time, claimsWindow ClaimsWindow) (*models.Bundle, error) {
	// ServiceDate only uses yyyy-mm-dd
	const svcDateFmt = "2006-01-02"

	header := make(http.Header)
	header.Add("IncludeTaxNumbers", "true")
	params := GetDefaultParams()
	params.Set("patient", patientID)
	params.Set("excludeSAMHSA", "true")

	if !claimsWindow.LowerBound.IsZero() {
		params.Add("service-date", fmt.Sprintf("ge%s", claimsWindow.LowerBound.Format(svcDateFmt)))
	}
	if !claimsWindow.UpperBound.IsZero() {
		params.Add("service-date", fmt.Sprintf("le%s", claimsWindow.UpperBound.Format(svcDateFmt)))
	}

	updateParamWithLastUpdated(&params, since, transactionTime)

	u, err := bfd.getURL("ExplanationOfBenefit", params)
	if err != nil {
		return nil, err
	}

	return bfd.getBundleData(u, jobID, cmsID, header)
}

// GetMetadata is a method to get metadata from the BFD response
func (bfd *BfdClient) GetMetadata() (string, error) {
	u, err := bfd.getURL("metadata", GetDefaultParams())
	if err != nil {
		return "", err
	}

	return bfd.getRawData(u)
}

func (bfd *BfdClient) getBundleData(u *url.URL, jobID, cmsID string, headers http.Header) (*models.Bundle, error) {
	var b *models.Bundle
	for ok := true; ok; {
		result, nextURL, err := bfd.tryBundleRequest(u, jobID, cmsID, headers)
		if err != nil {
			return nil, err
		}

		if b == nil {
			b = result
		} else {
			b.Entries = append(b.Entries, result.Entries...)
		}

		u = nextURL
		ok = nextURL != nil
	}

	return b, nil
}

func (bfd *BfdClient) tryBundleRequest(u *url.URL, jobID, cmsID string, headers http.Header) (*models.Bundle, *url.URL, error) {
	var (
		result  *models.Bundle
		nextURL *url.URL
		err     error
	)

	eb := backoff.NewExponentialBackOff()
	eb.InitialInterval = bfd.retryInterval
	b := backoff.WithMaxRetries(eb, bfd.maxTries)

	err = backoff.RetryNotify(func() error {
		req, err := http.NewRequest("GET", u.String(), nil)
		if err != nil {
			log.Error(err.Error())
			return err
		}

		queryID := uuid.New()
		addRequestHeaders(req, queryID, jobID, cmsID, headers)

		result, nextURL, err = bfd.client.DoBundleRequest(req)
		if err != nil {
			log.Error(err.Error())
		}
		return err
	},
		b,
		func(err error, d time.Duration) {
			log.Info(fmt.Sprintf("BFD request failed %s. Retry in %s", err.Error(), d.String()))
		},
	)

	if err != nil {
		return nil, nil, fmt.Errorf("BFD request failed %d time(s) %s", bfd.maxTries, err.Error())
	}

	return result, nextURL, nil
}

func (bfd *BfdClient) getRawData(u *url.URL) (string, error) {
	eb := backoff.NewExponentialBackOff()
	eb.InitialInterval = bfd.retryInterval
	b := backoff.WithMaxRetries(eb, bfd.maxTries)

	var result string

	err := backoff.RetryNotify(func() error {
		req, err := http.NewRequest("GET", u.String(), nil)
		if err != nil {
			log.Error(err.Error())
			return err
		}
		addRequestHeaders(req, uuid.New(), "", "", nil)

		result, err = bfd.client.DoRaw(req)
		if err != nil {
			log.Error(err.Error())
		}
		return err
	},
		b,
		func(err error, d time.Duration) {
			log.Info(fmt.Sprintf("BFD request failed %s. Retry in %s", err, d.String()))
		},
	)

	if err != nil {
		return "", fmt.Errorf("BFD request failed %d time(s) %s", bfd.maxTries, err.Error())
	}

	return result, nil
}

func (bfd *BfdClient) getURL(path string, params url.Values) (*url.URL, error) {
	u, err := url.Parse(fmt.Sprintf("%s%s/%s/", bfd.bfdServer, bfd.bfdBasePath, path))
	if err != nil {
		return nil, err
	}
	u.RawQuery = params.Encode()

	return u, nil
}

func addRequestHeaders(req *http.Request, reqID uuid.UUID, jobID, cmsID string, otherHeaders http.Header) {
	for key, values := range otherHeaders {
		for _, value := range values {
			req.Header.Add(key, value)
		}
	}
	// Info for BFD backend: https://jira.cms.gov/browse/BLUEBUTTON-483
	req.Header.Add("BFD-OriginalQueryTimestamp", time.Now().String())
	req.Header.Add("BFD-OriginalQueryId", reqID.String())
	req.Header.Add("BFD-OriginalQueryCounter", "1")
	req.Header.Add("keep-alive", "")
	req.Header.Add("BFD-OriginalUrl", req.URL.String())
	req.Header.Add("BFD-OriginalQuery", req.URL.RawQuery)
	req.Header.Add(jobIDHeader, jobID)
	req.Header.Add(clientIDHeader, cmsID)
	req.Header.Add("IncludeIdentifiers", "mbi")

	// We SHOULD NOT be specifying "Accept-Encoding: gzip" on the request header.
	// If we specify this header at the client level, then we must be responsible for decompressing the response.
	// This header should be automatically set by the underlying http.Transport which will handle the decompression transparently
	// Details: https://golang.org/src/net/http/transport.go#L2432
	// req.Header.Add("Accept-Encoding", "gzip")

	// Do not set BFD-specific headers with blank values
	// Leaving them here, commented out, in case we want to set them to real values in future
	//req.Header.Add("BFD-BeneficiaryId", "")
	//req.Header.Add("BFD-OriginatingIpAddress", "")
	//req.Header.Add("BFD-BackendCall", "")

}

// GetDefaultParams is a method to get default params
func GetDefaultParams() (params url.Values) {
	params = url.Values{}
	params.Set("_format", "application/fhir+json")
	return params
}

// HashIdentifier is a method to decode a hashed MBI
func HashIdentifier(toHash string) (hashedValue string) {
	bfdPepper := conf.GetAsString("bfd.hashPepper")
	bfdIter := conf.GetAsInt("bb.hashIter", 1000)

	pepper, err := hex.DecodeString(bfdPepper)
	// not sure how this can happen
	if err != nil {
		return ""
	}
	return hex.EncodeToString(pbkdf2.Key([]byte(toHash), pepper, bfdIter, 32, sha256.New))
}

func updateParamWithLastUpdated(params *url.Values, since string, transactionTime time.Time) {
	// upper bound will always be set
	params.Set("_lastUpdated", "le"+transactionTime.Format(time.RFC3339Nano))

	// only set the lower bound parameter if it exists and begins with "gt" (to align with what is expected in _lastUpdated)
	if len(since) > 0 && strings.HasPrefix(since, "gt") {
		params.Add("_lastUpdated", since)
	}
}

type httpLogger struct {
	t *http.Transport
	l *zap.Logger
}

func (h *httpLogger) RoundTrip(req *http.Request) (*http.Response, error) {
	go h.logRequest(req.Clone(context.Background()))
	resp, err := h.t.RoundTrip(req)
	if resp != nil {
		h.logResponse(req, resp)
	}
	return resp, err
}

func (h *httpLogger) logRequest(req *http.Request) {
	rqID := req.Context().Value(middleware.RequestIDKey)
	if rqID == nil {
		rqID = ""
	}
	h.l.With(
		createField("bfd_query_id", req.Header.Get("BFD-OriginalQueryId")),
		createField("bfd_query_ts", req.Header.Get("BFD-OriginalQueryTimestamp")),
		createField("bfd_uri", req.URL.String()),
		createField("job_id", req.Header.Get(jobIDHeader)),
		createField("cms_id", req.Header.Get(clientIDHeader)),
		createField("rqId", rqID.(string)),
	).Info("request")
}

func (h *httpLogger) logResponse(req *http.Request, resp *http.Response) {
	h.l.With(
		createField("resp_code", fmt.Sprint(resp.StatusCode)),
		createField("bfd_query_id", req.Header.Get("BFD-OriginalQueryId")),
		createField("bfd_query_ts", req.Header.Get("BFD-OriginalQueryTimestamp")),
		createField("bfd_uri", req.URL.String()),
		createField("job_id", req.Header.Get(jobIDHeader)),
		createField("cms_id", req.Header.Get(clientIDHeader)),
	).Info("response")
}

func createField(key string, val string) zap.Field {
	return zap.Field{Key: key, Type: zapcore.StringType, String: val}
}
