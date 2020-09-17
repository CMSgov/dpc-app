module github.com/CMSgov/dpc-app/dpc-testing/performance

go 1.15

require (
	github.com/CMSgov/dpc-app/dpcclient v0.0.0-20200916142145-e3d929a5e689
	github.com/bmizerany/perks v0.0.0-20141205001514-d9a9656a3a4b // indirect
	github.com/c2h5oh/datasize v0.0.0-20200825124411-48ed595a09d2 // indirect
	github.com/dgryski/go-gk v0.0.0-20200319235926-a69029f61654 // indirect
	github.com/dgryski/go-lttb v0.0.0-20180810165845-318fcdf10a77 // indirect
	github.com/influxdata/tdigest v0.0.1 // indirect
	github.com/mailru/easyjson v0.7.6 // indirect
	github.com/streadway/quantile v0.0.0-20150917103942-b0c588724d25 // indirect
	github.com/tsenart/go-tsz v0.0.0-20180814235614-0bd30b3df1c3 // indirect
	github.com/tsenart/vegeta v12.7.0+incompatible
)

replace github.com/CMSgov/dpc-app/dpcclient => ../../dpcclient
