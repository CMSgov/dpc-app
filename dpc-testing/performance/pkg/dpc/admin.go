package dpc

import (
	"fmt"
	"io/ioutil"
	"net/http"
)

type AdminAPI struct {
	URL string
}

func (admin *AdminAPI) GetClientToken(orgIDs ...string) []byte {
	reqURL := fmt.Sprintf("%s/generate-token", admin.URL)
	if len(orgIDs) > 0 {
		reqURL = fmt.Sprintf("%s?organization=%s", reqURL, orgIDs[0])
	}
	resp, err := http.Post(reqURL, "", nil)
	if err != nil {
		cleanAndPanic(err)
	}
	defer resp.Body.Close()
	clientToken, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		cleanAndPanic(err)
	}
	return clientToken
}
