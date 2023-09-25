set -ex -o pipefail

# Generate a golden macaroon
macaroon=$(curl -X POST http://localhost:9903/tasks/generate-token)

# Create organization using golden macaroon
result=$(curl -X POST -H "Authorization: Bearer $macaroon" -H "Content-Type: application/fhir+json" -H "Accept: application/fhir+json" --data @organization-bundle.json 'http://localhost:3002/v1/Organization/$submit' -v)
org_id=$(echo $result | jq -r '.id')

# Generate client token
curl -X POST -H 'Content-Type: application/json' "http://localhost:9903/tasks/generate-token?organization=$org_id&label=test-token"

# Generate pub/private keypair and signature
openssl genrsa -out private.pem 4096
openssl rsa -in private.pem -outform PEM -pubout -out public.pem
openssl dgst -sign private.pem -sha256 -out snippet.txt.sig dpc-web/public/snippet.txt
openssl dgst -verify public.pem -sha256 -signature snippet.txt.sig snippet.txt
openssl base64 -in snippet.txt.sig -out signature.sig

key_payload=$(
    cat <<EOM
{ "key" : "$(cat public.pem)", "signature" : "$(cat signature.sig)" }
EOM
)

echo $key_payload >key_payload.json

result=$(curl -X POST -H 'Content-Type: application/json' "http://localhost:9903/tasks/upload-key?organization=$org_id&label=test-key" -d@key_payload.json)
key_id=$(echo $result | jq -r '.id')

# Generate short-lived JWT manually
jwt=TODO

# Generate short-lived access token
result=$(curl -v "http://localhost:3002/v1/Token/auth" -H 'Content-Type: application/x-www-form-urlencoded' -H 'Accept: application/json' -X POST -d "grant_type=client_credentials&scope=system%2F*.*&client_assertion_type=urn%3Aietf%3Aparams%3Aoauth%3Aclient-assertion-type%3Ajwt-bearer&client_assertion=$jwt")
token=$(echo $result | jq -r '.access_token')

# Finally call endpoint
curl -X POST -H "Authorization: Bearer $token" -H "Content-Type: application/fhir+json" -H "Accept: application/fhir+json" 'http://localhost:3002/v1/Endpoint' -d@endpoint.json -v
