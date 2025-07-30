build:
	docker build . -t api_client
test:
	docker run --rm -v ${PWD}:/api-client -w /api-client api_client bundle exec rspec
	docker run --rm -v ${PWD}:/api-client -w /api-client api_client bundle exec rubocop

run:
	docker run -it --rm -v ${PWD}:/api-client -w /api-client api_client sh
