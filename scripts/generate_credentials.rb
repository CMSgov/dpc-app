require 'uri'
require 'net/http'
require 'json'
require 'bundler/inline'

gemfile do
    source 'https://rubygems.org'
    gem 'macaroons'
    gem 'activesupport'
end

require 'macaroons'
require 'active_support/core_ext/numeric/time.rb'


class Credentials
    def initialize(env, jwt, golden_macaroon, org_key, org_signature, path_to_org_pub_key)
        @env = env
        @jwt = jwt
        @golden_macaroon = golden_macaroon
        @key = org_key
        @signature = org_signature
        @path_to_org_pub_key = path_to_org_pub_key
    end

    def env
        @env
    end

    def jwt
        @jwt
    end

    def golden_macaroon
        @golden_macaroon
    end

    def public_key_label
        @public_key_label
    end

    def key
        @key
    end

    def signature
        @signature
    end

    def path_to_org_pub_key
        @path_to_org_pub_key
    end
end

def get_test_org_bundle(npi) 
    file = File.read("./scripts/test_org_bundle.json")
    org_bundle = JSON.parse(file)
    org_bundle['parameter'][0]['resource']['entry'][0]['resource']['identifier'][0]['value'] = "#{npi}"
    return org_bundle
end

def request_access_token(credentials)
    url = URI("https://#{credentials.env}.dpc.cms.gov/api/v1/Token/auth")

    https = Net::HTTP.new(url.host, url.port)
    https.use_ssl = true

    request = Net::HTTP::Post.new(url)
    request["Content-Type"] = "application/x-www-form-urlencoded"
    request["Accept"] = "application/json"
    request.body = "grant_type=client_credentials&scope=system%2F*.*&client_assertion_type=urn%3Aietf%3Aparams%3Aoauth%3Aclient-assertion-type%3Ajwt-bearer&client_assertion=#{credentials.jwt}"

    response = https.request(request)

    result = {}
    if response.code == "200"
        result = JSON.parse(response.body)
        return result["access_token"]
      else
        raise "ERROR: #{response.read_body}"
      end
end

def register_organization(credentials, org_bundle)
    url = URI("https://#{credentials.env}.dpc.cms.gov/api/v1/Organization/$submit")

    https = Net::HTTP.new(url.host, url.port)
    https.use_ssl = true

    request = Net::HTTP::Post.new(url)
    request["Content-Type"] = "application/fhir+json"
    request["Authorization"] = "Bearer " + credentials.golden_macaroon
    request.body = JSON.dump(org_bundle)

    response = https.request(request)
    
    result = {}
    if response.code == "200"
        result = JSON.parse(response.body)
        return result
      else
        raise "ERROR: #{response.read_body}"
      end
end

def upload_public_key(credentials, access_token, org_id)
    key_signature = {
        "key": credentials.key,
        "signature": credentials.signature
    }

    url = URI("https://#{credentials.env}.dpc.cms.gov/api/tasks/upload-key?organization=#{org_id}&label=#{credentials.public_key_label}")

    https = Net::HTTP.new(url.host, url.port)
    https.use_ssl = true

    request = Net::HTTP::Post.new(url)
    request["Content-Type"] = "application/json"
    request["Authorization"] = "Bearer " + access_token
    request.body = JSON.dump(key_signature)

    response = https.request(request)
    
    result = {}
    if response.code == "200"
        result = JSON.parse(response.body)
        return result
      else
        raise "ERROR: #{response.read_body}"
      end
end

def delegated_macaroon(golden_macaroon, reg_org_api_id, expiration=2.minutes.from_now.iso8601)
    m = Macaroon.from_binary(golden_macaroon)
    m.add_first_party_caveat("organization_id = #{reg_org_api_id}")
    m.add_first_party_caveat("expires = #{expiration}")
    m.add_first_party_caveat('dpc_macaroon_version = 1')
    return m.serialize
end

def generate_token(env, delegated_macaroon, public_key_label)
    url = URI("https://#{env}.dpc.cms.gov/api/v1/Token?label=#{public_key_label}")

    https = Net::HTTP.new(url.host, url.port)
    https.use_ssl = true

    request = Net::HTTP::Post.new(url)
    request["Content-Type"] = "application/json"
    request["Authorization"] = "Bearer " + delegated_macaroon

    response = https.request(request)
    
    result = {}
    if response.code == "200"
        result = JSON.parse(response.body)
        return result
      else
        raise "ERROR: #{response.read_body}"
    end
end

def create_credential_file(org_id, public_key_id, generated_token_obj)
    credential_file = File.new(ENV["HOME"]+"/Desktop/dpc-credentials.txt", "w")
    credential_file.puts("Registered Organization ID: #{org_id}\n")
    credential_file.puts("Public Key ID: PUBLIC_KEY_ID: #{public_key_id}\n")
    credential_file.puts("Organization Token Expiration Date: #{generated_token_obj['expiresAt']}\n")
    credential_file.puts("Organization Client Token: #{generated_token_obj['token']}\n")
    credential_file.close()
end

def create_encrypted_zip_file(path_to_org_pub_key)
    system("openssl rand -base64 64 | tr -d '\n' > ~/Desktop/password.txt")
    random_password = File.read(ENV["HOME"]+"/Desktop/password.txt")
    system("cd ~/Desktop; zip -P '#{random_password}' 'dpc-credentials' dpc-credentials.txt")
    system("cd ~/Desktop; openssl pkeyutl -encrypt -inkey #{path_to_org_pub_key} -pubin -in password.txt -out encrypted_password.enc")
end

#credentials is an instance of Credentials class
def generate_credentials(credentials, org_bundle, public_key_label, path_to_org_pub_key, expiration)
    access_token = request_access_token(credentials)
    org_id = register_organization(credentials, org_bundle)
    public_key = upload_public_key(credentials, access_token, org_id)
    delegated_macaroon = delegated_macaroon(credentials.golden_macaroon, org_id, expiration)
    generated_token_obj = generate_token(credentials.env, delegated_macaroon, org_id, public_key_label)
    create_credential_file(org_id, public_key, generated_token_obj)
    create_encrypted_zip_file(credentials.path_to_org_pub_key)
end
