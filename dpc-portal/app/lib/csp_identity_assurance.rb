# frozen_string_literal: true

module CspIdentityAssurance
  # General NIST/OIDC
  IAL1 = 'http://idmanagement.gov/ns/assurance/ial/1'
  IAL2 = 'http://idmanagement.gov/ns/assurance/ial/2'

  # Specific to login.gov
  VERIFIED = 'urn:acr.login.gov:verified'
  FACIAL_MATCH_REQUIRED = 'urn:acr.login.gov:verified-facial-match-required'
end
