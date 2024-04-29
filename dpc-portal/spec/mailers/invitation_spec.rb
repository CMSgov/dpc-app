# frozen_string_literal: true

require 'rails_helper'

RSpec.describe InvitationMailer, type: :mailer do
  describe :invite_cd do
    it 'has link to invitation' do
      invited_by = build(:user)
      provider_organization = build(:provider_organization, id: 2)
      invitation = build(:invitation, id: 4, invited_by:, provider_organization:)
      mailer = InvitationMailer.with(invitation:).invite_cd
      expected_url = 'http://localhost:3100/portal/organizations/2/invitations/4/accept'
      expect(mailer.body).to match(expected_url)
    end
  end
  describe :invite_ao do
    it 'has link to invitation' do
      provider_organization = build(:provider_organization, id: 2)
      invitation = build(:invitation, id: 4, provider_organization:)
      given_name = family_name = ''
      mailer = InvitationMailer.with(invitation:, given_name:, family_name:).invite_ao
      expected_url = 'http://localhost:3100/portal/organizations/2/invitations/4/accept'
      expect(mailer.body).to match(expected_url)
    end
  end
end
