module github.com/CMSgov/dpc-app/dpc-testing/performance

go 1.15

require (
	github.com/CMSgov/dpc-app/dpcclient v0.0.0-20200916142145-e3d929a5e689
	github.com/bmizerany/perks v0.0.0-20141205001514-d9a9656a3a4b // indirect
	github.com/dgryski/go-gk v0.0.0-20200319235926-a69029f61654 // indirect
	github.com/hashicorp/go-retryablehttp v0.6.8
	github.com/influxdata/tdigest v0.0.1 // indirect
	github.com/joeljunstrom/go-luhn v0.0.0-20190413165225-1e071b33b576
	github.com/mailru/easyjson v0.7.6 // indirect
	github.com/streadway/quantile v0.0.0-20150917103942-b0c588724d25 // indirect
	github.com/stretchr/testify v1.3.0
	github.com/tsenart/vegeta/v12 v12.8.4
	github.com/zach-klippenstein/goregen v0.0.0-20160303162051-795b5e3961ea
)

replace github.com/CMSgov/dpc-app/dpcclient => ../../dpcclient
