# frozen_string_literal: true

FactoryBot.define do
  factory :invitation do
    invited_given_name { 'Bob' }
    invited_family_name { 'Hodges' }
    phone_raw { '111-111-1111' }
    invited_email { 'bob@testy.com' }
    invited_email_confirmation { 'bob@testy.com' }
    invitation_type { 'credential_delegate' }
  end
end
