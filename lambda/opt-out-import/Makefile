AWS_ACCESS_KEY_ID ?= 1234
AWS_SECRET_ACCESS_KEY ?= 1234

build: export GOOS=linux
build: export GOARCH=amd64
build: 
	go build -o bin/bootstrap main.go parsers.go utils.go models.go db.go
	@du -h bin/bootstrap
test:
	IS_TESTING=true DB_USER_DPC_CONSENT="postgres" DB_PASS_DPC_CONSENT="dpc-safe" go test -v -covermode=count -coverprofile=coverage/unit_coverage.out
	go tool cover -html=coverage/unit_coverage.out -o coverage/unit_coverage.html
run-local:
	ENV="local" DB_USER_DPC_CONSENT="postgres" DB_PASS_DPC_CONSENT="dpc-safe" DB_HOST="localhost" IS_TESTING=true go run main.go parsers.go utils.go models.go db.go
run:
	IS_TESTING=false go run main.go parsers.go utils.go models.go db.go
integration-test:
	go build -cover -o bin/bootstrap main.go parsers.go utils.go models.go db.go
	rm -f coverage/*cov*
	GOCOVERDIR=coverage ENV="local" DB_USER_DPC_CONSENT="postgres" DB_PASS_DPC_CONSENT="dpc-safe" DB_HOST="localhost" IS_TESTING=true ./bin/main
	go tool covdata percent -i=coverage
	go tool covdata textfmt -i=coverage -o=coverage/int_coverage.out
	go tool cover -html=coverage/int_coverage.out -o coverage/int_coverage.html
