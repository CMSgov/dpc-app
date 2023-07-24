require 'uri'
require 'net/http'
require 'json'
require 'openssl'

class Settings
    def initialize(env, jwt, golden_macaroon, public_key_label, key, signature, path_to_org_pub_key)
        @env = env
        @jwt = jwt
        @golden_macaroon = golden_macaroon
        @public_key_label = public_key_label
        @key = key
        @signature = signature
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

def request_access_token(settings)
    url = URI("https://#{settings.env}.dpc.cms.gov/api/v1/Token/auth")

    https = Net::HTTP.new(url.host, url.port)
    https.use_ssl = true

    request = Net::HTTP::Post.new(url)
    request["Content-Type"] = "application/x-www-form-urlencoded"
    request["Accept"] = "application/json"
    request.body = "grant_type=client_credentials&scope=system%2F*.*&client_assertion_type=urn%3Aietf%3Aparams%3Aoauth%3Aclient-assertion-type%3Ajwt-bearer&client_assertion=#{settings.jwt}"

    response = https.request(request)

    result = {}
    if response.code == "200"
        result = JSON.parse(response.body)
        return result["access_token"]
      else
        raise "ERROR: #{response.read_body}"
      end
end

def register_organization(settings, org_bundle)
    url = URI("https://#{settings.env}.dpc.cms.gov/api/v1/Organization/$submit")

    https = Net::HTTP.new(url.host, url.port)
    https.use_ssl = true

    request = Net::HTTP::Post.new(url)
    request["Content-Type"] = "application/fhir+json"
    request["Authorization"] = "Bearer " + settings.golden_macaroon
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

def upload_public_key(settings, access_token, org_id)
    key_signature = {
        "key": settings.key,
        "signature": settings.signature
    }

    url = URI("https://#{settings.env}.dpc.cms.gov/api/tasks/upload-key?organization=#{org_id}&label=#{settings.public_key_label}")

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

def generate_token(settings, access_token, org_id, expiration)
    url = URI("https://#{settings.env}.dpc.cms.gov/api/tasks/generate-token?organization=#{org_id}&label=#{settings.public_key_label}&expiration=#{expiration}")

    https = Net::HTTP.new(url.host, url.port)
    https.use_ssl = true

    request = Net::HTTP::Post.new(url)
    request["Content-Type"] = "application/json"
    request["Authorization"] = "Bearer " + access_token

    response = https.request(request)
    
    result = {}
    if response.code == "200"
        result = JSON.parse(response.body)
        return result
      else
        raise "ERROR: #{response.read_body}"
    end
end

def create_credential_file(org_id, public_key_id, expiration, client_token)
    credential_file = File.new(ENV["HOME"]+"/Desktop/dpc-credentials.txt", "w")
    credential_file.puts("Registered Organization ID: #{org_id}\n")
    credential_file.puts("Public Key ID: PUBLIC_KEY_ID: #{public_key_id}\n")
    credential_file.puts("Organization Token Expiration Date: #{expiration}\n")
    credential_file.puts("Organization Client Token: #{client_token}\n")
    credential_file.close()
end

def create_encrypted_zip_file(path_to_org_pub_key)
    system("openssl rand -base64 64 | tr -d '\n' > ~/Desktop/password.txt")
    random_password = File.read(ENV["HOME"]+"/Desktop/password.txt")
    system("cd ~/Desktop; zip -P '#{random_password}' 'dpc-credentials' dpc-credentials.txt")
    system("cd ~/Desktop; openssl pkeyutl -encrypt -inkey #{path_to_org_pub_key} -pubin -in password.txt -out encrypted_password.enc")
end

def generate_credentials(env, jwt, golden_macaroon, org_bundle, public_key_label, key, signature, path_to_org_pub_key)
    settings = Settings.new(env, jwt, golden_macaroon, public_key_label, key, signature, path_to_org_pub_key)
    access_token = request_access_token(settings)
    org_id = register_organization(settings, org_bundle)
    public_key = upload_public_key(settings, access_token, org_id)
    generated_token = generate_token(settings, access_token, org_id)
    create_encrypted_zip_file(settings.path_to_org_pub_key)
end
