AWS_ACCESS_KEY_ID ?= 1234
AWS_SECRET_ACCESS_KEY ?= 1234

build: export GOOS=linux
build: export GOARCH=amd64
build: 
	go build -o bin/bootstrap main.go
	@du -h bin/bootstrap
test:
	go test -v
