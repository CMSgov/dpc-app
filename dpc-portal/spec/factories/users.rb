# frozen_string_literal: true

FactoryBot.define do
  factory :user, aliases: %i[invited_by] do
    given_name { 'John' }
    family_name { 'Smith' }
    # uid, provider, email removed — now live on csp_user/user_emails
  end
end
