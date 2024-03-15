# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::CredentialDelegate::InvitationSuccessComponent, type: :component do
  include ComponentSupport
  describe 'html' do
    subject(:html) do
      render_inline(component)
      normalize_space(rendered_content)
    end

    let(:org) { ComponentSupport::MockOrg.new }

    let(:component) { described_class.new(org) }
    let(:expected_html) do
      <<~HTML
        <div>
          <div class="margin-bottom-5">← <a href="/portal/organizations/#{org.path_id}">#{org.name}</a></div>
           <h1>Credential delegate invite sent</h1>
           <div class="usa-alert usa-alert--success">
             <div class="usa-alert__body">
               <p class="usa-alert__text">
                 This is a succinct, helpful success message.
               </p>
            </div>
          </div>
          <div class="margin-top-5">
            <a class="usa-button usa-button--outline" href="/portal/">Return home</a>
          </div>
        </div>
      HTML
    end
    before do
      render_inline(component)
    end

    it { is_expected.to match_html_fragment(expected_html) }
  end
end
