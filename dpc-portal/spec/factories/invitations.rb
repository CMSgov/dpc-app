# frozen_string_literal: true

FactoryBot.define do
  factory :invitation do
    provider_organization
    invited_email { 'bob@testy.com' }
    invited_email_confirmation { 'bob@testy.com' }
    trait :ao do
      invitation_type { :authorized_official }
    end

    trait :cd do
      invited_by
      invited_given_name { 'Bob' }
      invited_family_name { 'Hodges' }
      phone_raw { '111-111-1111' }
      invitation_type { :credential_delegate }
    end
  end
end
