# frozen_string_literal: true

require 'rails_helper'
require 'support/login_support'

RSpec.describe Page::ExistingAccount::LinkAccountComponent, type: :component do
  include ComponentSupport
  include LoginSupport

  LoginSupport::CSP_MAP.each do |csp_name, display|
    context "as #{display}" do
      let(:email) { 'bob@example.com' }
      let(:component) { described_class.new(email, csp_name) }

      subject(:html) do
        render_inline(component)
        normalize_space(rendered_content)
      end

      it 'should match heading' do
        header = <<~HTML
          <h1>Existing account found</h1>
        HTML
        is_expected.to include(normalize_space(header))
      end

      it 'should render an info alert' do
        alert = <<~HTML
          <div class="usa-alert usa-alert--info margin-bottom-4">
            <div class="usa-alert__body">
        HTML
        is_expected.to include(normalize_space(alert))
      end

      it 'should include alert body text' do
        alert_text = <<~HTML
          <p class="usa-alert__text">This account was already created using:</p>
        HTML
        is_expected.to include(normalize_space(alert_text))
      end

      it 'should include the email address in the alert' do
        alert_email = <<~HTML
          <p><strong>#{email}</strong> and <strong>#{display}</strong></p>
        HTML
        is_expected.to include(normalize_space(alert_email))
      end

      it 'should render the options list' do
        options = <<~HTML
          <ul class="merge-options">
        HTML
        is_expected.to include(normalize_space(options))
      end

      it 'should include the link identity provider option' do
        is_expected.to include('Link this new identity provider to your account.')
      end

      it 'should include the start over option' do
        is_expected.to include('Start over with your original provider.')
      end

      it 'should render the link to existing account button' do
        button = <<~HTML
          <span>Link to existing account</span>
        HTML
        is_expected.to include(normalize_space(button))
      end

      it 'should render a button targeting the omniauth authorize path' do
        is_expected.to include(%(action="/auth/#{csp_name}"))
      end

      context 'with a different email address' do
        let(:email) { 'different@example.com' }

        it 'should render the updated email address' do
          is_expected.to include('different@example.com')
        end

        it 'should not include the default email address' do
          is_expected.not_to include('bob@example.com')
        end
      end
    end
  end
end
