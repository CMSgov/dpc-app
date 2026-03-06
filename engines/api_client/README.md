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

## Debugging and Development
To build an image, use the `make` command in the project root directory.
```bash
make api-client
```


To ssh into a Docker container with the dpc_client code, use the `make` command in the project root directory.
```bash
make api-client-sh
```

## Testing
Test using `make` commands in the project root directory.

### Unit Tests
```bash
make ci-api-client
```

### Integration tests with the API
```bash
make ci-api-client-integration
```
