package middleware

import (
	"fmt"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

var sanitizeTests = []struct {
	in  string
	out bool
}{
	{"<img src=x onerror=prompt(1234)>", false},
	{"hello", true},
	{"hello@gmail.com", true},
	{"<script/>", false},
	{"", true},
	{"<SCRIPT SRC=http://xss.rocks/xss.js></SCRIPT>", false},
	{"javascript:/*--></title></style></textarea></script></xmp><svg/onload='+/\"/+/onmouseover=1/+/[*/[]/+alert(1)//'>", false},
	{"<IMG SRC=\"javascript:alert('XSS');\">", false},
	{"<IMG SRC=javascript:alert('XSS')>", false},
	{"<IMG SRC=JaVaScRiPt:alert('XSS')>", false},
	{"<IMG SRC=javascript:alert(&quot;XSS&quot;)>", false},
	{"<IMG SRC=`javascript:alert(\"RSnake says, 'XSS'\")`>", false},
	{"\\<a onmouseover=\"alert(document.cookie)\"\\>xxs link\\</a\\>", false},
	{"\\<a onmouseover=alert(document.cookie)\\>xxs link\\</a\\>", false},
	{"<IMG \"\"\"><SCRIPT>alert(\"XSS\")</SCRIPT>\"\\>", false},
	{"<IMG SRC=javascript:alert(String.fromCharCode(88,83,83))>", false},
	{"perl -e 'print \"<IMG SRC=java\\0script:alert(\\\"XSS\\\")>\";' > out", false},
	{"<BODY onload!#$%&()*~+-_.,:;?@[/|\\]^`=alert(\"XSS\")>", false},
	{"<<SCRIPT>alert(\"XSS\");//\\<</SCRIPT>", false},
	{"<STYLE>li {list-style-image: url(\"javascript:alert('XSS')\");}</STYLE><UL><LI>XSS</br>\n", false},
	{"<!--[if gte IE 4]>\n" +
		"<SCRIPT>alert('XSS');</SCRIPT>\n" +
		"<![endif]-->", false},
	{"&#X000003C;", false},
	{"&#0000060;", false},
	{"/?param=<data:text/html;base64,PHNjcmlwdD5hbGVydCgnWFNTJyk8L3NjcmlwdD4=", false},
	{"Sherlock & Watson", true},
}

type SanitizeMiddlewareTestSuite struct {
	suite.Suite
}

func TestSanitizeMiddlewareTestSuite(t *testing.T) {
	suite.Run(t, new(SanitizeMiddlewareTestSuite))
}

func (suite *SanitizeMiddlewareTestSuite) TestSanitization() {

	for _, tt := range sanitizeTests {
		suite.T().Run(tt.in, func(t *testing.T) {
			nextHandler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
				b, _ := ioutil.ReadAll(r.Body)
				_, _ = w.Write(b)
			})

			req := httptest.NewRequest(http.MethodPost, "http://www.your-domain.com", strings.NewReader(fmt.Sprintf(`{"foo":"%s"}`, tt.in)))
			res := httptest.NewRecorder()

			fm := Sanitize(nextHandler)
			fm.ServeHTTP(res, req)

			w := res.Result()
			if !tt.out {
				assert.Equal(suite.T(), http.StatusInternalServerError, w.StatusCode)
			} else {
				b, _ := ioutil.ReadAll(w.Body)
				assert.Equal(suite.T(), fmt.Sprintf(`{"foo":"%s"}`, tt.in), string(b))
			}
		})
	}
}
