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
    context 'ao invalid invitation' do
      let(:component) { described_class.new(invitation, 'invalid') }
      it 'should match header' do
        header = <<~HTML
          <h1>#{I18n.t('verification.invalid_status')}</h1>
        HTML
        is_expected.to include(normalize_space(header))
      end
    end

    context 'cd invalid invitation' do
      let(:invitation) { create(:invitation, :cd, provider_organization:) }
      let(:component) { described_class.new(invitation, 'invalid') }
      it 'should match header' do
        header = <<~HTML
          <h1>#{I18n.t('verification.invalid_status')}</h1>
        HTML
        is_expected.to include(normalize_space(header))
      end
    end

    context 'PII mismatch' do
      let(:component) { described_class.new(invitation, 'pii_mismatch') }
      it 'should match header' do
        header = <<~HTML
          <h1>#{CGI.escapeHTML(I18n.t('verification.pii_mismatch_status'))}</h1>
        HTML
        is_expected.to include(normalize_space(header))
      end
      it 'should have logout button' do
        button_url = "/logout?invitation_id=#{invitation.id}"
        is_expected.to include(button_url)
      end
    end

    context 'Email mismatch' do
      let(:invitation) { create(:invitation, :cd, provider_organization:) }
      let(:component) { described_class.new(invitation, 'email_mismatch') }
      it 'should match header' do
        header = <<~HTML
          <h1>#{CGI.escapeHTML(I18n.t('verification.email_mismatch_status'))}</h1>
        HTML
        is_expected.to include(normalize_space(header))
      end
      it 'should have logout button' do
        button_url = "/logout?invitation_id=#{invitation.id}"
        is_expected.to include(button_url)
      end
    end

    context 'AO already accepted' do
      let(:component) { described_class.new(invitation, 'ao_accepted') }
      it 'should match header' do
        header = <<~HTML
          <h1>#{I18n.t('verification.ao_accepted_status')}</h1>
        HTML
        is_expected.to include(normalize_space(header))
      end
    end

    context 'CD already accepted' do
      let(:component) { described_class.new(invitation, 'cd_accepted') }
      it 'should match header' do
        header = <<~HTML
          <h1>#{I18n.t('verification.cd_accepted_status')}</h1>
        HTML
        is_expected.to include(normalize_space(header))
      end

      it 'should have Go to DPC home button' do
        button_url = '/portal/'
        is_expected.to include(button_url)
      end
    end

    context 'AO expired' do
      let(:status) { :pending }
      let(:invitation) { create(:invitation, :ao, provider_organization:, status:) }
      let(:component) { described_class.new(invitation, 'ao_expired') }
      it 'should match header' do
        header = <<~HTML
          <h1>#{I18n.t('verification.ao_expired_status')}</h1>
        HTML
        is_expected.to include(normalize_space(header))
      end
      it 'should have renew button' do
        button_url = "/organizations/#{provider_organization.id}/invitations/#{invitation.id}/renew"
        is_expected.to include(button_url)
      end
      context 'already renewed' do
        let(:status) { :renewed }
        let(:component) { described_class.new(invitation, 'ao_renewed') }
        it 'should match header' do
          header = <<~HTML
            <h1>#{I18n.t('verification.ao_renewed_status')}</h1>
          HTML
          is_expected.to include(normalize_space(header))
        end
        it 'should have no renew button' do
          button_url = "/organizations/#{provider_organization.id}/invitations/#{invitation.id}/renew"
          is_expected.not_to include(button_url)
        end
      end
    end

    context 'CD expired' do
      let(:status) { :pending }
      let(:invitation) { create(:invitation, :cd, provider_organization:, status:) }
      let(:component) { described_class.new(invitation, 'cd_expired') }
      it 'should match header' do
        header = <<~HTML
          <h1>#{I18n.t('verification.cd_expired_status')}</h1>
        HTML
        is_expected.to include(normalize_space(header))
      end
    end

    context 'Server error' do
      let(:component) { described_class.new(invitation, 'server_error') }
      it 'should match header' do
        header = <<~HTML
          <h1>#{I18n.t('verification.server_error_status')}</h1>
        HTML
        is_expected.to include(normalize_space(header))
      end
    end
  end
end
