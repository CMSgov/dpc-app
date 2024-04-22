# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::Session::InvitationLoginComponent, type: :component do
  include ComponentSupport
  describe 'login component' do
    let(:invitation) { create(:invitation, :cd) }
    let(:component) { described_class.new(invitation) }
    before { render_inline(component) }
    it 'should be a usa section' do
      expect(page).to have_selector('section.usa-section')
    end

    it 'should have a login button' do
      expect(page).to have_selector('button.usa-button')
      expect(page.find('button.usa-button')).to have_content('Sign in with')
      expect(page).to have_selector('button.usa-button span.login-button__logo')
      expect(page.find('button.usa-button span.login-button__logo')).to have_content('Login.gov')
    end

    it 'should post to appropriate url' do
      path = "organizations/#{invitation.provider_organization.id}/invitations/#{invitation.id}/login"
      url = "http://test.host/portal/#{path}"
      expect(page.find('form')[:action]).to eq url
      expect(page.find('form')[:method]).to eq 'post'
    end

    it 'should have two columns' do
      expect(page.find_all('.grid-col-12').size).to eq 2
    end
  end
end
