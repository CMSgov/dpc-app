# frozen_string_literal: true

module CspIdentityAssurance
  # General NIST/OIDC
  LEGACY_IAL1 = 'http://idmanagement.gov/ns/assurance/ial/1'
  LEGACY_IAL2 = 'http://idmanagement.gov/ns/assurance/ial/2'

  # Specific to login.gov
  AUTH_ONLY = 'urn:acr.login.gov:auth-only'
  VERIFIED = 'urn:acr.login.gov:verified'
  FACIAL_MATCH_REQUIRED = 'urn:acr.login.gov:verified-facial-match-required'
  FACIAL_MATCH_PREFERRED = 'urn:acr.login.gov:verified-facial-match-preferred'
end
