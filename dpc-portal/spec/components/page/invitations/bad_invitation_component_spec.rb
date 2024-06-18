# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::Invitations::BadInvitationComponent, type: :component do
  include ComponentSupport

  describe 'html' do
    subject(:html) do
      render_inline(component)
      normalize_space(rendered_content)
    end

    before do
      render_inline(component)
    end

    let(:provider_organization) { create(:provider_organization, name: 'Health Hut') }
    let(:invitation) { create(:invitation, :ao, provider_organization:) }
    context 'invalid invitation' do
      let(:component) { described_class.new(invitation, 'invalid', 'warning') }
      it 'should match header' do
        header = <<~HTML
          <h1>Invitation is invalid</h1>
            <div class="usa-alert usa-alert--warning margin-bottom-4">
        HTML
        is_expected.to include(normalize_space(header))
      end
    end

    context 'PII mismatch' do
      let(:component) { described_class.new(invitation, 'pii_mismatch', 'error') }
      it 'should match header' do
        header = <<~HTML
          <h1>Invitation denied</h1>
            <div class="usa-alert usa-alert--error margin-bottom-4">
        HTML
        is_expected.to include(normalize_space(header))
      end
    end

    context 'AO expired' do
      let(:status) { :pending }
      let(:invitation) { create(:invitation, :ao, provider_organization:, status:) }
      let(:component) { described_class.new(invitation, 'ao_expired', 'error') }
      it 'should have organization name' do
        is_expected.to include(invitation.provider_organization.name)
      end
      it 'should have renew button' do
        button_url = "/organizations/#{provider_organization.id}/invitations/#{invitation.id}/renew"
        is_expected.to include(button_url)
      end
      context 'already renewed' do
        let(:status) { :renewed }
        it 'should have disabled renew button' do
          button_url = "/organizations/#{provider_organization.id}/invitations/#{invitation.id}/renew"
          disabled = %(<button class="usa-button" disabled="disabled" type="submit">)
          is_expected.to include(button_url)
          is_expected.to include(disabled)
        end
      end
    end
  end
end
