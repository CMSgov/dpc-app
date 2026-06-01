# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::Session::LoginComponent, type: :component do
  include ComponentSupport

  describe 'login component' do
    let(:url) { '/' }
    let(:sandbox_url) { 'https://sandbox.dpc.cms.gov/' }
    let(:component) { described_class.new(url) }
    before { render_inline(component) }
    it 'should be a usa section' do
      expect(page).to have_selector('section.usa-section')
    end

    it 'should have login buttons' do
      # login.gov
      expect(page).to have_selector('button.usa-button span.lg-login-button__logo')
      expect(page.find('button.usa-button span.lg-login-button__logo')).to have_content('Login.gov')

      # Clear
      expect(page).to have_selector('button.usa-button span.clear-login-button__logo')
      expect(page.find('button.usa-button span.clear-login-button__logo')).to have_content('CLEAR')

      # ID.me
      expect(page).to have_selector('button.usa-button span.idme-login-button__logo')
      expect(page.find('button.usa-button span.idme-login-button__logo')).to have_content('ID.me')
    end

    it 'should render a link to the System Use Agreement' do
      expect(page).to have_link('System Use Agreement',
                                href: Rails.application.routes.url_helpers.system_use_agreement_path)
    end

    it 'login.gov button should post to appropriate url' do
      expect(page.find('form', match: :first)[:action]).to eq url
      expect(page.find('form', match: :first)[:method]).to eq 'post'
    end

    it 'test data button should link to sandbox url' do
      expect(page.find('a.usa-button--outline.usa-button')[:href]).to eq sandbox_url
    end

    it 'should have two columns' do
      expect(page.find_all('.grid-col-12').size).to eq 2
    end
  end

  describe 'last used csp was CLEAR' do
    let(:url) { '/' }
    let(:component) { described_class.new(url, last_used_csp: :clear) }
    before { render_inline(component) }

    it 'wraps only the CLEAR button' do
      # Check that the right button has the "last used" wrapper.
      expect(page).to have_css('.last-used-login-wrapper .clear-login-button__logo')

      # Make sure the other two don't.
      expect(page).not_to have_css('.last-used-login-wrapper .idme-login-button__logo')
      expect(page).not_to have_css('.last-used-login-wrapper .lg-login-button__logo')
    end
  end

  describe 'last used csp was ID.me' do
    let(:url) { '/' }
    let(:component) { described_class.new(url, last_used_csp: :id_me) }
    before { render_inline(component) }

    it 'wraps only the ID.me button' do
      # Check that the right button has the "last used" wrapper.
      expect(page).to have_css('.last-used-login-wrapper .idme-login-button__logo')

      # Make sure the other two don't.
      expect(page).not_to have_css('.last-used-login-wrapper .clear-login-button__logo')
      expect(page).not_to have_css('.last-used-login-wrapper .lg-login-button__logo')
    end
  end

  describe 'last used csp was Login.gov' do
    let(:url) { '/' }
    let(:component) { described_class.new(url, last_used_csp: :login_dot_gov) }
    before { render_inline(component) }

    it 'wraps only the Login.gov button' do
      # Check that the right button has the "last used" wrapper.
      expect(page).to have_css('.last-used-login-wrapper .lg-login-button__logo')

      # Make sure the other two don't.
      expect(page).not_to have_css('.last-used-login-wrapper .clear-login-button__logo')
      expect(page).not_to have_css('.last-used-login-wrapper .idme-login-button__logo')
    end

    describe 'no last used csp' do
      let(:url) { '/' }
      let(:component) { described_class.new(url, last_used_csp: nil) }
      before { render_inline(component) }

      it "doesn't wrap any buttons" do
        expect(page).not_to have_css('.last-used-login-wrapper .clear-login-button__logo')
        expect(page).not_to have_css('.last-used-login-wrapper .idme-login-button__logo')
        expect(page).not_to have_css('.last-used-login-wrapper .lg-login-button__logo')
      end
    end
  end
end
