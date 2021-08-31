#!/bin/bash
#Generate CA
openssl req -newkey rsa:2048 -nodes -x509 -days 365 -out local.ca.crt -keyout local.ca.key -subj "/C=US/ST=CA/L=LosAngeles/O=ACME/CN=localhost"

#Generate Attribution Cert
openssl genrsa -out local.attribution.key 2048
openssl req -new -key local.attribution.key -days 365 -out local.attribution.csr -subj "/C=US/ST=CA/L=LosAngeles/O=ACME Attr/CN=localhost"
openssl x509  -req -in local.attribution.csr -CA local.ca.crt -CAkey local.ca.key -CAcreateserial -out local.attribution.crt -days 365 -sha256 -extfile <(printf "subjectAltName=DNS:localhost,DNS:local.attribution.dpc.cms.gov")
rm local.attribution.csr

#Generate API Cert
openssl genrsa -out local.api.key 2048
openssl req -new -key local.api.key -days 365 -out local.api.csr -subj "/C=US/ST=CA/L=LosAngeles/O=ACME API/CN=localhost"
openssl x509  -req -in local.api.csr -CA local.ca.crt -CAkey local.ca.key -out local.api.crt -days 365 -sha256 -CAcreateserial  -extfile <(printf "subjectAltName=DNS:localhost,DNS:local.api.dpc.cms.gov")
rm local.api.csr

#Generate portal Cert
openssl genrsa -out local.portal.key 2048
openssl req -new -key local.portal.key -days 365 -out local.portal.csr -subj "/C=US/ST=CA/L=LosAngeles/O=ACME portal/CN=localhost"
openssl x509  -req -in local.portal.csr -CA local.ca.crt -CAkey local.ca.key -out local.portal.crt -days 365 -sha256 -CAcreateserial -extfile <(printf "subjectAltName=DNS:localhost,DNS:local.portal.dpc.cms.gov")
rm local.portal.csr
