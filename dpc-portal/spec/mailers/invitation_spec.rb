# frozen_string_literal: true

require 'rails_helper'

RSpec.describe InvitationMailer, type: :mailer do
  describe :invite_cd do
    it 'has link to invitation' do
      invited_by = build(:user)
      provider_organization = build(:provider_organization, id: 2)
      invitation = build(:invitation, id: 4, invited_by:, provider_organization:)
      mailer = InvitationMailer.with(invitation:).invite_cd
      expected_url = 'http://localhost:3100/portal/organizations/2/credential_delegate_invitations/4/accept'
      expect(mailer.body).to match(expected_url)
    end
  end
end
