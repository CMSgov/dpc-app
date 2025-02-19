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

    context 'with organization' do
      let(:component) do
        org = ProviderOrganization.new(name: 'name', npi: 'npi', id: '5', terms_of_service_accepted_at: 2.days.ago)
        described_class.new(organization: org, failure_code: 'verification.no_approved_enrollment')
      end
      let(:expected_html) do
        <<~HTML
          <div>
            <div class="margin-bottom-5">← <a href="/portal/organizations">Return to my organizations</a></div>
            <h1>name</h1>
            <p><span class="text-bold">NPI:</span> npi</p>
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
            <div class="font-body-lg text-bold">#{I18n.t('verification.ao_med_sanctions_status')}</div>
            <p>#{I18n.t('verification.ao_med_sanctions_text')}</p>
          </div>
        HTML
      end

      it { is_expected.to match_html_fragment(expected_html) }
    end
  end
end
