# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::Organization::NewOrganizationSuccessComponent, type: :component do
  include ComponentSupport
  describe 'html' do
    subject(:html) do
      render_inline(component)
      normalize_space(rendered_content)
    end

    let(:component) { described_class.new('1234567890') }
    let(:expected_html) do
      <<~HTML
            <div>
              <div class="margin-bottom-5">← <a href="/portal/">path</a></div>
               <h1>Add new organization</h1>
               <h2>Organization added!</h2>
               <div class="usa-alert usa-alert--success">
                 <div class="usa-alert__body">
                   <h4 class="usa-alert__heading">Success status</h4>
                   <p class="usa-alert__text">
                     This is a succinct, helpful success message.
                   </p>
                </div>
              </div>
              <p>
              To manage your organization's credentials, you can assign a Credential Delegate (CD).#{' '}
        This person will be responsible for submitting your organization's technical requirements
         and managing your access to the DPC API.
              </p>
              <p>Learn more about the Credential Delegate role.</p>
              <div class="margin-top-5">
                <div class="display-flex flex-row flex-start" style="gap:20px;">
                    <div class="flex-align-self-center">
                        <form class="button_to" method="get" action="/portal/"><button class="usa-button" type="submit">Assign CD now</button></form>

                    </div>
                    <div class="flex-align-self-center">
                        <a href="/portal/">Assign CD later</a>
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
