build: export GOOS=linux
build: export GOARCH=amd64
build: 
	go build -o bin/bootstrap main.go db.go
	@du -h bin/bootstrap
test:
	go test -v
run-local:
	ENV="local" DPC_DB_USER="postgres" DPC_DB_PASS="dpc-safe" DB_HOST="localhost" IS_TESTING=true go run main.go db.go