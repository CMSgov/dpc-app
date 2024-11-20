# frozen_string_literal: true

require 'rails_helper'

RSpec.describe InvitationMailer, type: :mailer do
  describe :invite_cd do
    let(:invited_by) { build(:user) }
    let(:provider_organization) { build(:provider_organization, id: 2, name: 'Health Hut') }
    let(:invitation) { build(:invitation, id: 4, invited_by:, provider_organization:) }
    it 'has link to invitation' do
      expected_url = 'http://localhost:3100/portal/organizations/2/invitations/4'

      mailer = InvitationMailer.with(invitation:).invite_cd
      expect(mailer.body).to match(expected_url)
    end
    it 'uses https for invitation link if it thinks it is prod' do
      expect(Rails.env).to receive(:production?).and_return true
      expected_url = 'https://localhost:3100/portal/organizations/2/invitations/4'

      mailer = InvitationMailer.with(invitation:).invite_cd
      expect(mailer.body).to match(expected_url)
    end
    it 'has organization name and NPI' do
      mailer = InvitationMailer.with(invitation:).invite_cd
      expect(mailer.body).to match(provider_organization.name)
      expect(mailer.body).to match("NPI #{provider_organization.npi})")
    end
  end
  describe :invite_ao do
    let(:provider_organization) { build(:provider_organization, id: 2, name: 'Health Hut') }
    let(:invitation) { build(:invitation, id: 4, provider_organization:) }
    let(:given_name) { '' }
    let(:family_name) { '' }
    it 'has link to invitation' do
      expected_url = 'http://localhost:3100/portal/organizations/2/invitations/4'

      mailer = InvitationMailer.with(invitation:, given_name:, family_name:).invite_ao
      html = mailer.body.parts.select { |part| part.content_type.match 'text/html' }.first
      expect(html.body).to match(expected_url)
    end
    it 'uses https for invitation link if it thinks it is prod' do
      expect(Rails.env).to receive(:production?).and_return true
      expected_url = 'https://localhost:3100/portal/organizations/2/invitations/4'

      mailer = InvitationMailer.with(invitation:, given_name:, family_name:).invite_ao
      html = mailer.body.parts.select { |part| part.content_type.match 'text/html' }.first
      expect(html.body).to match(expected_url)
    end
    it 'has organization name and NPI' do
      mailer = InvitationMailer.with(invitation:, given_name:, family_name:).invite_ao
      html = mailer.body.parts.select { |part| part.content_type.match 'text/html' }.first
      expect(html.body).to match(provider_organization.name)
      expect(html.body).to match("NPI #{provider_organization.npi})")
    end
  end
  describe :cd_accepted do
    let(:invited_by) { build(:user) }
    let(:provider_organization) { build(:provider_organization, id: 2, name: 'Health Hut') }
    let(:invitation) { build(:invitation, id: 4, invited_by:, provider_organization:) }
    let(:invited_given_name) { '' }
    let(:invited_family_name) { '' }
    it 'has link to organization' do
      expected_url = 'http://localhost:3100/portal/organizations/2'

      mailer = InvitationMailer.with(invitation:, invited_given_name:, invited_family_name:).cd_accepted
      expect(mailer.body).to match(expected_url)
    end
    it 'uses https for organization link if it thinks it is prod' do
      expect(Rails.env).to receive(:production?).and_return true
      expected_url = 'https://localhost:3100/portal/organizations/2'

      mailer = InvitationMailer.with(invitation:, invited_given_name:, invited_family_name:).cd_accepted
      expect(mailer.body).to match(expected_url)
    end
    it 'has organization name and NPI' do
      mailer = InvitationMailer.with(invitation:, invited_given_name:, invited_family_name:).cd_accepted
      expect(mailer.body).to match(provider_organization.name)
      expect(mailer.body).to match("NPI #{provider_organization.npi})")
    end
  end
end
