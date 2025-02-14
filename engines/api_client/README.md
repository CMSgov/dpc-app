# ApiClient
This gem provides a client for connecting to the DPC API from rails applications.

## Usage
First, you need to make sure the API_METADATA_URL, API_ADMIN_URL and GOLDEN_MACAROON environment variables are set.
The client itself requires no parameters.
```
dpc_client = DpcClient.new
```

## Installation
Add this line to your application's Gemfile:

```ruby
gem 'api_client', github: 'CMSgov/dpc-app', branch: 'main', glob: 'engines/api_client/api_client.gemspec'
```

And then execute:
```bash
$ bundle install
```

## Testing
Build the docker image

In the api_client directory
```bash
$ docker build . -t api_client
```

Run the tests until they pass
```
$ docker run --rm -v ${PWD}:/api-client -w /api-client api_client bundle exec rspec
$ docker run --rm -v ${PWD}:/api-client -w /api-client api_client bundle exec rubocop
```
