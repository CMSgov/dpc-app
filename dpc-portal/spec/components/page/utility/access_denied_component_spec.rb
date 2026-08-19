# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::Utility::AccessDeniedComponent, type: :component do
  describe 'html' do
    subject(:html) do
      render_inline(component)
      rendered_content
    end

    before do
      render_inline(component)
    end

    let(:sprite_path) { ActionController::Base.helpers.asset_path('@uswds/uswds/dist/img/sprite.svg') }

    context 'with organization' do
      let(:component) do
        org = ProviderOrganization.new(name: 'name', npi: 'npi', id: '5', dpc_api_organization_id: '5',
                                       terms_of_service_accepted_at: 2.days.ago)
        allow(org).to receive(:ao).and_return('ao')
        status_display = ['lock', %i[text-gray-50], 'verification.access_denied']
        described_class.new(organization: org, failure_code: 'verification.no_approved_enrollment',
                            status_display:, role: 'role')
      end
      let(:expected_html) do
        <<~HTML
          <div>
            <div class="margin-bottom-5">← <a href="/organizations">Return to my organizations</a></div>
            <h1>name</h1>
            <div class="margin-bottom-3 display-flex flex-row flex-start" style="gap:20px;">
              <div><span class="text-bold">NPI-2:</span> npi</div>
              <div><span class="text-bold">Role:</span> role</div>
              <div><span class="text-bold">Status:</span>
                <svg class="text-gray-50 usa-icon" aria-hidden="true">
                  <use xlink:href="#{sprite_path}#lock"></use>
                </svg>
                <span class="margin-top-neg-2px">verification.access_denied</span>
              </div>
            </div>
            <div class="margin-bottom-3"><span class="text-bold">Authorized Official:</span> ao</div>
            <div class="margin-bottom-5"><span class="text-bold">Organization ID:</span> 5</div>
            <div class="font-body-lg text-bold">#{I18n.t('verification.no_approved_enrollment_status')}</div>
            <p>#{I18n.t('verification.no_approved_enrollment_text')}</p>
          </div>
        HTML
      end

      it { is_expected.to match_html_fragment(expected_html) }
    end
    context 'without organization' do
      let(:component) do
        described_class.new(failure_code: 'verification.ao_med_sanctions')
      end
      let(:expected_html) do
        <<~HTML
          <div>
            <h1>Access Denied</h1>
            <div class="font-body-lg text-bold">#{I18n.t('verification.ao_med_sanctions_status')}</div>
            <p>#{I18n.t('verification.ao_med_sanctions_text')}</p>
          </div>
        HTML
      end

      it { is_expected.to match_html_fragment(expected_html) }
    end
  end
end
