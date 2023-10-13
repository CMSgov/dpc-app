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
gem 'api_client', path: '../engines/api_client'
```

And then execute:
```bash
$ bundle
```

## License
The gem is available as open source under the terms of the [MIT License](https://opensource.org/licenses/MIT).
