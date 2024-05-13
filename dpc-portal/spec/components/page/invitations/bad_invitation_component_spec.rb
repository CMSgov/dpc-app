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

    context 'invalid invitation' do
      let(:component) { described_class.new('invalid', 'warning') }
      it 'should match header' do
        header = <<~HTML
          <h1>Invitation is invalid</h1>
            <div class="usa-alert usa-alert--warning margin-bottom-4">
        HTML
        is_expected.to include(normalize_space(header))
      end
    end

    context 'PII mismatch' do
      let(:component) { described_class.new('pii_mismatch', 'error') }
      it 'should match header' do
        header = <<~HTML
          <h1>Invitation denied</h1>
            <div class="usa-alert usa-alert--error margin-bottom-4">
        HTML
        is_expected.to include(normalize_space(header))
      end
    end
  end
end
