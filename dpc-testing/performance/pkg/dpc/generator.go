package dpc

import (
	"bytes"
	"crypto/rsa"
	"fmt"
	"io/ioutil"
	"net/url"
)

func templateBodyGenerator(templateFile string, replacementMap map[string]func() string) func() []byte {
	body, err := ioutil.ReadFile(templateFile)
	if err != nil {
		panic(err)
	}
	return func() []byte {
		output := body

		for k, v := range replacementMap {
			keyCount := bytes.Count(body, []byte(k))
			for i := 0; i < keyCount; i++ {
				value := v()
				output = bytes.Replace(output, []byte(k), []byte(value), i+1)
			}

		}
		return output
	}
}

func keyBodyGenerator(n int, fn func() (string, *rsa.PrivateKey, string)) func() []byte {
	var bodies [][]byte
	for i := 0; i < n; i++ {
		pubKeyStr, _, signature := fn()
		bodies = append(bodies, []byte(fmt.Sprintf(`{ "key": "%s", "signature": "%s"}`, pubKeyStr, signature)))
	}

	num := len(bodies)
	if num == 0 {
		return func() []byte { return []byte{} }
	}
	i := 0
	return func() []byte {
		if i >= num {
			return []byte{}
		}

		body := bodies[i]
		i++
		return body
	}
}

func authBodyGenerator(authTokens [][]byte) func() []byte {
	var bodies [][]byte
	for _, authToken := range authTokens {
		bodies = append(bodies, []byte(
			url.Values{
				"scope":                 {"system/*.*"},
				"grant_type":            {"client_credentials"},
				"client_assertion_type": {"urn:ietf:params:oauth:client-assertion-type:jwt-bearer"},
				"client_assertion":      {string(authToken)},
			}.Encode()),
		)
	}

	num := len(bodies)
	if num == 0 {
		return func() []byte { return []byte{} }
	}
	i := 0
	return func() []byte {
		if i >= num {
			return []byte{}
		}

		body := bodies[i]
		i++
		return body
	}
}

func byteArrayGenerator(arrayOfBytes [][]byte) func() []byte {
	numOfBytes := len(arrayOfBytes)
	if numOfBytes == 0 {
		return func() []byte { return []byte{} }
	}
	i := 0
	return func() []byte {
		if i >= numOfBytes {
			return []byte{}
		}

		token := arrayOfBytes[i]
		i++
		return token
	}
}
