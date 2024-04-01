# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::Session::LoginComponent, type: :component do
  include ComponentSupport
  describe 'html' do
    subject(:html) do
      render_inline(component)
      rendered_content
    end

    let(:component) { described_class.new('/portal/') }
    let(:expected_html) do
      <<~HTML
        <div class="grid-container">
          <div class="grid-row">
            <div class="grid-col">
              <div class="border-1px border-base-lighter margin-bottom-4 padding-x-3 padding-bottom-3">
                <div class="display-flex flex-row flex-justify">
                  <div class="flex-align-self-center">
                    <h2>Sign in to your account</h2>
                    <p>You can access or create your account by signing in below.</p>
                    <form class="button_to" method="post" action="/portal/"><button class="usa-button width-full" data-turbo="false" type="submit">
                        Sign in with <span class="login-button__logo">Login.gov</span>
                      </button>
                    </form>
                  </div>
                </div>
              </div>
            </div>
            <div class="grid-col">
              <div class="margin-bottom-4 padding-x-3 padding-bottom-3">
                <h2>Welcome to the production protal for the  Data at the Point of Care (DPC) API</h2>
                <p>Use this portal to get production credentials, manage X, and do X. If you're an
                  Authorized Official, you'll need to take the following steps:</p>
                <div>
                  <ul class="usa-list">
                    <li><strong>Add an organization.</strong> Vivavmus nec velit sed leo scelerisque laoreet vestibulum.</li>
                    <li><strong>Invite a credential delegate.</strong> Vivavmus nec velit sed leo scelerisque laoreet vestibulum.</li>
                    <li><strong>Lorem ipsum.</strong> Vivavmus nec velit sed leo scelerisque laoreet vestibulum.</li>
                  </ul>
                </div>
              </div>
            </div>
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
