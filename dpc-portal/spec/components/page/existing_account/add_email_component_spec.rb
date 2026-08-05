# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::ExistingAccount::AddEmailComponent, type: :component do
  include ComponentSupport

  CspUtils::CODES_TO_DISPLAY.each do |csp_name, display|
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
          <p><strong>#{EmailMask.masked(email)}</strong> and <strong>#{display}</strong></p>
        HTML
        is_expected.to include(normalize_space(alert_email))
      end

      it 'should render the options list' do
        options = <<~HTML
          <ul class="merge-options">
        HTML
        is_expected.to include(normalize_space(options))
      end

      it 'should include the add new email option' do
        is_expected.to include('Add the new email address you just used to your DPC account.')
      end

      it 'should include the sign in with existing email option' do
        is_expected.to include('Sign in with an email already associated with your account.')
      end

      it 'should render the add new email button' do
        button = <<~HTML
          <span>Add new email</span>
        HTML
        is_expected.to include(normalize_space(button))
      end

      it "should render a button to sign in with #{display}" do
        is_expected.to include(%(action="/auth/#{csp_name}"))
      end
    end
  end
end
