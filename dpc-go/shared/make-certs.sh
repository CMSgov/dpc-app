#!/bin/bash
mkdir -p b64
#Generate CA
openssl req -newkey rsa:2048 -nodes -x509 -days 365 -out local.ca.crt -keyout local.ca.key -subj "/C=US/ST=CA/L=LosAngeles/O=ACME/CN=localhost"
base64 -i local.ca.crt -o b64/b64.local.ca.crt

#Generate Attribution Server Cert
openssl genrsa -out local.attribution.key 2048
openssl req -new -key local.attribution.key -days 365 -out local.attribution.csr -subj "/C=US/ST=CA/L=LosAngeles/O=ACME Attr/CN=localhost"
openssl x509  -req -in local.attribution.csr -CA local.ca.crt -CAkey local.ca.key -CAcreateserial -out local.attribution.crt -days 365 -sha256 -extfile <(printf "subjectAltName=DNS:localhost\nbasicConstraints = CA:true")
rm local.attribution.csr
base64 -i local.attribution.crt -o b64/b64.local.attribution.crt
base64 -i local.attribution.key -o b64/b64.local.attribution.key

#Generate API Server Cert
openssl genrsa -out local.api.key 2048
openssl req -new -key local.api.key -days 365 -out local.api.csr -subj "/C=US/ST=CA/L=LosAngeles/O=ACME API/CN=localhost"
openssl x509  -req -in local.api.csr -CA local.ca.crt -CAkey local.ca.key -out local.api.crt -days 365 -sha256 -CAcreateserial -extfile <(printf "subjectAltName=DNS:localhost\nbasicConstraints = CA:true")
rm local.api.csr
base64 -i local.api.crt -o b64/b64.local.api.crt
base64 -i local.api.key -o b64/b64.local.api.key

#Generate Portal's Client Cert (Used to access DPC-API)
openssl genrsa -out local.portal.key 2048
openssl req -new -key local.portal.key -days 365 -out local.portal.csr -subj "/C=US/ST=CA/L=LosAngeles/O=ACME portal-client/CN=localhost"
openssl x509  -req -in local.portal.csr -CA local.api.crt -CAkey local.api.key -out local.portal.crt -days 365 -sha256 -CAcreateserial -extfile <(printf "subjectAltName=DNS:localhost")
rm local.portal.csr
base64 -i local.portal.crt -o b64/b64.local.portal.crt
base64 -i local.portal.key -o b64/b64.local.portal.key

#Generate API's Client Cert (Used to access DPC-Attr)
openssl genrsa -out local.api.attr.client.key 2048
openssl req -new -key local.api.attr.client.key -days 365 -out local.api.attr.client.csr -subj "/C=US/ST=CA/L=LosAngeles/O=ACME api-client/CN=localhost"
openssl x509  -req -in local.api.attr.client.csr -CA local.attribution.crt -CAkey local.attribution.key -out local.api.attr.client.crt -days 365 -sha256 -CAcreateserial -extfile <(printf "subjectAltName=DNS:localhost")
rm local.api.attr.client.csr
base64 -i local.api.attr.client.crt -o b64/b64.local.api.attr.client.crt
base64 -i local.api.attr.client.key -o b64/b64.local.api.attr.client.key