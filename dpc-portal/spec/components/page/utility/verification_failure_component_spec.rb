# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::Utility::VerificationFailureComponent, type: :component do
  context 'when CSP is LoginDotGov' do
    let(:component) { described_class.new(nil, 'login_dot_gov') }
    before { render_inline(component) }

    it 'should link to help page' do
      expect(page).to have_link('Get help from Login.gov',
                                href: 'https://www.login.gov/help/verify-your-identity/overview/')
    end

    it 'should link to contact page' do
      expect(page).to have_link('Contact Login.gov', href: 'https://www.login.gov/contact/')
    end

    it 'should specify LoginDotGov on page' do
      expect(page).to have_text('Your identity could not be verified with Login.gov')
    end
  end

  context 'when CSP is IdMe' do
    let(:component) { described_class.new(nil, 'id_me') }
    before { render_inline(component) }

    it 'should specify IdMe on page' do
      expect(page).to have_text('Your identity could not be verified with ID.me')
    end
  end
end
