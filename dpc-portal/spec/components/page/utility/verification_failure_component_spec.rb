# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::Utility::VerificationFailureComponent, type: :component do
  context 'when CSP is LoginDotGov' do
    let(:component) { described_class.new(nil, 'login_dot_gov') }
    before { render_inline(component) }

    it 'should show the Login.gov-specific heading' do
      expect(page).to have_text("We couldn't verify your identity")
    end

    it 'should explain why verification failed' do
      expect(page).to have_text('We were unable to verify your identity with Login.gov.')
      expect(page).to have_text('This may be due to incorrect information or a technical issue.')
    end

    it 'should have list of next steps' do
      expect(page).to have_css('ol li', count: 3)
      expect(page).to have_text('If you are having issues creating or logging into a Login.gov account')
      expect(page).to have_text('Try another identity service provider')
      expect(page).to have_text("If you still can't verify your identity")
    end

    it 'should link to help page' do
      expect(page).to have_link("Login.gov's Help Section",
                                href: 'https://www.login.gov/help/verify-your-identity/overview/')
    end

    it 'should link to contact page' do
      expect(page).to have_link('contact them', href: 'https://www.login.gov/contact/')
    end

    it 'should link to contact dpcinfo' do
      expect(page).to have_link('dpcinfo@cms.hhs.gov', href: 'mailto:dpcinfo@cms.hhs.gov')
    end
  end

  context 'when CSP is IdMe' do
    let(:component) { described_class.new(nil, 'id_me') }
    before { render_inline(component) }

    it 'should specify IdMe on page' do
      expect(page).to have_text('Your identity could not be verified with ID.me')
    end
  end

  context 'when CSP is CLEAR' do
    let(:component) { described_class.new(nil, 'clear') }
    before { render_inline(component) }

    it 'should specify CLEAR on page' do
      expect(page).to have_text('Your identity could not be verified with CLEAR')
    end

    it 'should link to support' do
      expect(page).to have_link('CLEAR Support', href: 'https://www.clearme.com/support')
    end
  end
end
