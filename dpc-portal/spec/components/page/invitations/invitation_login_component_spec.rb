# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::Invitations::InvitationLoginComponent, type: :component do
  include ComponentSupport

  describe 'login component' do
    let(:provider_organization) { build(:provider_organization, dpc_api_organization_id: 'foo') }
    let(:invitation) { create(:invitation, :cd, provider_organization:) }
    let(:component) { described_class.new(invitation) }
    before { render_inline(component) }

    it 'should have verify identity buttons for all csps' do
      expect(page).to have_selector('button.usa-button span.lg-login-button__logo', text: 'Verify with Login.gov')
      expect(page).to have_selector('button.usa-button span.clear-login-button__logo', text: 'Verify with CLEAR')
      expect(page).to have_selector('button.usa-button span.idme-login-button__logo', text: 'Verify with ID.me')
    end

    it 'should render a link to the How to verify your identity url' do
      expect(page).to have_link('How to verify your identity',
                                href: 'https://login.gov/help/verify-your-identity/how-to-verify-your-identity/')
    end

    it 'should post each CSP to the appropriate url' do
      expect(page).to have_selector("form[action*='?provider=clear'][method='post']")
      expect(page).to have_selector("form[action*='?provider=id_me'][method='post']")
      expect(page).to have_selector("form[action*='?provider=login_dot_gov'][method='post']")
    end
  end

  describe 'ao' do
    let(:invitation) { create(:invitation, :ao) }
    let(:component) { described_class.new(invitation) }
    before { render_inline(component) }
    it 'should have step component at step 2' do
      expect(page).to have_selector('.usa-step-indicator__current-step')
      expect(page.find('.usa-step-indicator__current-step').text).to eq '2'
    end
  end

  describe 'cd' do
    let(:invitation) { create(:invitation, :cd) }
    let(:component) { described_class.new(invitation) }
    before { render_inline(component) }
    it 'should have step component at step 2' do
      expect(page).to have_selector('.usa-step-indicator__current-step')
      expect(page.find('.usa-step-indicator__current-step').text).to eq '2'
    end
  end
end
