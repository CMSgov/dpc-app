build: export GOOS=linux
build: export GOARCH=amd64
build: 
	go build -o bin/bootstrap main.go db.go
	@du -h bin/bootstrap
test:
	go test -v ./...
run-local:
	ENV="local" DB_HOST="localhost" IS_TESTING=true LOCAL_STACK_ENDPOINT=http://localhost:4566 S3_UPLOAD_BUCKET=bfd-local-eft S3_UPLOAD_PATH=bfdeft01/dpc/out go run main.go db.go