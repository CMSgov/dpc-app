# frozen_string_literal: true

require 'rails_helper'

CSP_CONFIG = {
  clear: { logo_class: 'clear-login-button__logo', display_name: 'CLEAR' },
  id_me: { logo_class: 'idme-login-button__logo', display_name: 'ID.me' },
  login_dot_gov: { logo_class: 'lg-login-button__logo', display_name: 'Login.gov' }
}.freeze

RSpec.describe Page::Session::LoginComponent, type: :component do
  include ComponentSupport

  describe 'login component' do
    let(:sandbox_url) { 'https://sandbox.dpc.cms.gov/' }
    let(:component) { described_class.new }
    before { render_inline(component) }
    it 'should be a usa section' do
      expect(page).to have_selector('section.usa-section')
    end

    it 'should have login buttons' do
      CSP_CONFIG.each_value do |csp|
        expect(page).to have_selector("button.usa-button span.#{csp[:logo_class]}")
        expect(page.find("button.usa-button span.#{csp[:logo_class]}")).to have_content(csp[:display_name])
      end
    end

    it 'should render a link to the System Use Agreement' do
      expect(page).to have_link('System Use Agreement',
                                href: Rails.application.routes.url_helpers.system_use_agreement_path)
    end

    it 'CSP buttons should post to appropriate urls' do
      expect(page.find('form[action="/auth/login_dot_gov"]')[:method]).to eq 'post'
      expect(page.find('form[action="/auth/clear"]')[:method]).to eq 'post'
      expect(page.find('form[action="/auth/id_me"]')[:method]).to eq 'post'
    end

    it 'test data button should link to sandbox url' do
      expect(page.find('a.usa-button--outline.usa-button')[:href]).to eq sandbox_url
    end

    it 'should have two columns' do
      expect(page.find_all('.grid-col-12').size).to eq 2
    end
  end

  CSP_CONFIG.each do |csp_code, csp|
    describe "last used csp was #{csp[:display_name]}" do
      let(:component) { described_class.new(last_used_csp: csp_code) }
      before { render_inline(component) }

      it "wraps only the #{csp[:display_name]} button" do
        # Check that the right button has the "last used" wrapper.
        expect(page).to have_css(".last-used-login-wrapper .#{csp[:logo_class]}")

        # Make sure the other two don't.
        CSP_CONFIG.except(csp_code).each_value do |other_csp|
          expect(page).not_to have_css(".last-used-login-wrapper .#{other_csp[:logo_class]}")
        end
      end

      it "outlines all buttons except for #{csp[:display_name]}" do
        expect(find_button_for(csp[:logo_class])[:class]).not_to include('usa-button--outline')

        CSP_CONFIG.except(csp_code).each_value do |other_csp|
          expect(find_button_for(other_csp[:logo_class])[:class]).to include('usa-button--outline')
        end
      end
    end
  end

  describe 'no last used csp' do
    let(:component) { described_class.new(last_used_csp: nil) }
    before { render_inline(component) }

    it "doesn't wrap any buttons" do
      CSP_CONFIG.each_value do |csp|
        expect(page).not_to have_css(".last-used-login-wrapper .#{csp[:logo_class]}")
      end
    end

    it "doesn't outline any buttons" do
      CSP_CONFIG.each_value do |csp|
        expect(find_button_for(csp[:logo_class])[:class]).not_to include('usa-button--outline')
      end
    end
  end

  def find_button_for(logo_class)
    page.find("button.usa-button span.#{logo_class}").find(:xpath, '..')
  end
end
