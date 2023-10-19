# ApiClient
This gem provides a client for connecting to the DPC API from rails applications.

## Usage
First, you need to make sure the API_METADATA_URL and GOLDEN_MACAROON environment variables are set.
The client itself requires no parameters.
```
dpc_client = DpcClient.new
```

## Installation
Add this line to your application's Gemfile:

```ruby
gem 'api_client', github: 'CMSgov/dpc-app', branch: 'master', glob: 'engines/api_client/api_client.gemspec'
```

And then execute:
```bash
$ bundle install
```
