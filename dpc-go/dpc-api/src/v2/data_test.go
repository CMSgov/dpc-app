package v2

import (
	"context"
	"encoding/json"
	"fmt"
	"github.com/CMSgov/dpc/api/conf"
	"github.com/CMSgov/dpc/api/middleware"
	"github.com/CMSgov/dpc/api/model"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"strings"
	"testing"
)

type DataControllerTestSuite struct {
	suite.Suite
	data *DataController
	mac  *MockAttributionClient
	file *os.File
}

func (suite *DataControllerTestSuite) SetupTest() {
	f, _ := ioutil.TempFile("/tmp", "filename.*.ndjson")
	text := []byte("hello")
	_, _ = f.Write(text)
	_ = f.Close()

	suite.file = f
	conf.NewConfig("../../configs")
	mac := new(MockAttributionClient)
	suite.mac = mac
	suite.data = NewDataController(mac)
}

func (suite *DataControllerTestSuite) TearDownTest() {
	_ = os.Remove(suite.file.Name())
}

func TestDataControllerTestSuite(t *testing.T) {
	suite.Run(t, new(DataControllerTestSuite))
}

func (suite *DataControllerTestSuite) TestGetFile() {
	f := strings.TrimPrefix(strings.TrimSuffix(suite.file.Name(), filepath.Ext(suite.file.Name())), "/tmp/")
	req := httptest.NewRequest(http.MethodGet, "http://blah.com", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware.ContextKeyFileName, f)
	req = req.WithContext(ctx)
	w := httptest.NewRecorder()

	fi := model.FileInfo{
		FileName:     f,
		FileLength:   0,
		FileCheckSum: nil,
	}
	b, _ := json.Marshal(fi)
	suite.mac.On("Data", mock.Anything, mock.MatchedBy(func(key string) bool {
		return key == fmt.Sprintf("validityCheck/%s", f)
	})).Return(b, nil)

	suite.data.GetFile(w, req)

	resp := w.Result()

	b, _ = ioutil.ReadAll(resp.Body)

	assert.Equal(suite.T(), "hello", string(b))
	assert.Equal(suite.T(), resp.Header.Get("Content-Disposition"), fmt.Sprintf("attachment; filename=\"%s.ndjson\"", f))
}
