module github.com/CMSgov/dpc-app/dpc-testing/performance

go 1.15

require (
	github.com/CMSgov/dpc-app/dpcclient v0.0.0-20211220174319-4469d0cadfff
	github.com/bmizerany/perks v0.0.0-20141205001514-d9a9656a3a4b // indirect
	github.com/dgryski/go-gk v0.0.0-20200319235926-a69029f61654 // indirect
	github.com/gbrlsnchs/jwt/v3 v3.0.1 // indirect
	github.com/go-pg/pg v8.0.7+incompatible
	github.com/google/gxui v0.0.0-20151028112939-f85e0a97b3a4 // indirect
	github.com/google/uuid v1.3.0
	github.com/influxdata/tdigest v0.0.1 // indirect
	github.com/jinzhu/inflection v1.0.0 // indirect
	github.com/joeljunstrom/go-luhn v0.0.0-20190413165225-1e071b33b576
	github.com/magefile/mage v1.12.1 // indirect
	github.com/mailru/easyjson v0.7.7 // indirect
	github.com/onsi/ginkgo v1.16.5 // indirect
	github.com/onsi/gomega v1.17.0 // indirect
	github.com/smartystreets/goconvey v1.7.2 // indirect
	github.com/streadway/quantile v0.0.0-20150917103942-b0c588724d25 // indirect
	github.com/stretchr/testify v1.7.0
	github.com/tsenart/vegeta v12.7.0+incompatible
	github.com/zach-klippenstein/goregen v0.0.0-20160303162051-795b5e3961ea
	golang.org/x/crypto v0.0.0-20211215153901-e495a2d5b3d3 // indirect
	golang.org/x/net v0.7.0 // indirect
	mellium.im/sasl v0.2.1 // indirect
)

replace github.com/CMSgov/dpc-app/dpcclient => ../../dpcclient
