require 'uri'
require 'net/http'
require 'json'


def get_test_org_bundle(npi) 
    return {
        "resourceType": "Parameters",
        "parameter": [
        {
            "resource": {
            "resourceType": "Bundle",
            "type": "collection",
            "entry": [
                {
                "resource": {
                    "resourceType": "Organization",
                    "identifier": [
                    {
                        "system": "http://hl7.org/fhir/sid/us-npi",
                        "value": "#{npi}"
                    }
                    ],
                    "name": "Decent Shepherd Community Care",
                    "address": [
                    {
                        "use": "work",
                        "type": "both",
                        "line": [
                        "160 Wells Ave"
                        ],
                        "city": "Newton",
                        "state": "MA",
                        "postalCode": "02459",
                        "country": "US"
                    }
                    ],
                    "contact": [
                    {
                        "name": {
                        "use": "usual",
                        "family": "Lai",
                        "given": [
                            "Guanpi"
                        ]
                        },
                        "address": {
                        "use": "work",
                        "type": "both",
                        "line": [
                            "160 Wells Ave"
                        ],
                        "city": "Newton",
                        "state": "MA",
                        "postalCode": "02459",
                        "country": "US"
                        },
                        "telecom": [
                        {
                            "system": "phone",
                            "use": "work",
                            "value": "480-262-5629"
                        },
                        {
                            "system": "email",
                            "use": "work",
                            "value": "guanpi.lai@acclivityhealth.com"
                        }
                        ]
                    }
                    ]
                }
                }
            ]
            }
        }
        ]
    }
end

def request_access_token(env, jwt)
    url = URI("https://#{env}.dpc.cms.gov/api/v1/Token/auth")

    https = Net::HTTP.new(url.host, url.port)
    https.use_ssl = true

    request = Net::HTTP::Post.new(url)
    request["Content-Type"] = "application/x-www-form-urlencoded"
    request["Accept"] = "application/json"
    request.body = "grant_type=client_credentials&scope=system%2F*.*&client_assertion_type=urn%3Aietf%3Aparams%3Aoauth%3Aclient-assertion-type%3Ajwt-bearer&client_assertion=#{jwt}"

    response = https.request(request)

    result = {}
    if response.code == "200"
        result = JSON.parse(response.body)
        return result["access_token"]
      else
        raise "ERROR: #{response.read_body}"
      end
end

def register_organization(env, golden_macaroon, org_bundle)
    url = URI("https://#{env}.dpc.cms.gov/api/v1/Organization/$submit")

    https = Net::HTTP.new(url.host, url.port)
    https.use_ssl = true

    request = Net::HTTP::Post.new(url)
    request["Content-Type"] = "application/fhir+json"
    request["Authorization"] = "Bearer " + golden_macaroon
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

def upload_public_key(env, access_token, org_id, public_key_label, key, signature)
    key_signature = {
        "key": key,
        "signature": signature
    }

    url = URI("https://#{env}.dpc.cms.gov/api/tasks/upload-key?organization=#{org_id}&label=#{label}")

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

def generate_token(env, access_token, org_id, public_key_label, expiration)
    url = URI("https://#{env}.dpc.cms.gov/api/tasks/generate-token?organization=#{org_id}&label=#{public_key_label}&expiration=#{expiration}")

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
    system("openssl rand -base64 64 > ~/Desktop/password.txt")
    random_password = File.read(ENV["HOME"]+"/Desktop/password.txt")
    system("cd ~/Desktop; zip -P '#{random_password}' 'dpc-credentials' dpc-credentials.txt")
    system("cd ~/Desktop; openssl pkeyutl -encrypt -inkey #{path_to_org_pub_key} -pubin -in password.txt -out encrypted_password.enc")
end

def generate_credentials(env, jwt, golden_macaroon, org_bundle, public_key_label, key, signature, path_to_org_pub_key)
    access_token = request_access_token(env, jwt)
    org_id = register_organization(env, golden_macaroon, org_bundle)
    public_key = upload_public_key(env, access_token, org_id, public_key_label, key, signature)
    generated_token = generate_token(env, access_token, org_id, public_key_label)
    create_encrypted_zip_file(path_to_org_pub_key)
end
