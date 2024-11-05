# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Accessibility', type: :system do
  include Devise::Test::IntegrationHelpers
  include DpcClientSupport
  before do
    driven_by(:selenium_headless)
  end
  let(:dpc_api_organization_id) { 'some-gnarly-guid' }
  context 'login' do
    it 'shows login page ok' do
      visit '/users/sign_in'
      expect(page).to_not have_text('You need to sign in or sign up before continuing.')
      expect(page).to have_text('Sign in')
      expect(page).to be_axe_clean
    end
  end
end
