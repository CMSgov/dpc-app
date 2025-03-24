# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::Organization::CompoundShowComponent, type: :component do
  include ComponentSupport
  describe 'html' do
    subject(:html) do
      render_inline(component)
      normalize_space(rendered_content)
    end
    let(:org) { build(:provider_organization, name: 'Health Hut', npi: '11111111', id: 2) }
    let(:invitation) { build(:invitation, status: :accepted)}
    let(:component) { described_class.new(org, delegate_info, credential_start, role, invitation) }

    before do
      render_inline(component)
    end

    context 'cd tab' do
      let(:delegate_info) { { active: [], pending: [], expired: [] } }
      let(:role) { 'Authorized Official' }
      context 'credential delegate start' do
        let(:credential_start) { false }
        it 'Should have org name' do
          is_expected.to include("<h1>#{org.name}</h1>")
        end
        it 'Should have npi' do
          is_expected.to include("<span class=\"text-bold\">NPI-2:</span> #{org.npi}")
        end
        it 'Should have role' do
          is_expected.to include("<span class=\"text-bold\">Role:</span> #{role}")
        end
        it 'Should have status' do
          is_expected.to include("<span class=\"text-bold\">Status:</span> #{invitation.status.capitalize}")
        end
        it 'Should have AO' do
          allow(org).to receive(:ao).and_return('John Doe')
          is_expected.to include("<span class=\"text-bold\">Authorized Official:</span> John Doe")
        end
        it 'Should have org id' do
          is_expected.to include("<span class=\"text-bold\">Organization ID:</span> #{org.dpc_api_organization_id}")
        end
        it 'Should have script tag' do
          is_expected.to include('<script')
        end
        it 'Should have header tag' do
          is_expected.to include('<header')
        end
        it 'Should have credential delegates' do
          is_expected.to include('<div id="credential_delegates">')
        end
        it 'Should have credentials' do
          is_expected.to include('<div id="credentials">')
        end
        it 'Should start on credential delegate page' do
          is_expected.to include('}make_current(0);')
          is_expected.to_not include('}make_current(1);')
        end
        it 'Should not show warning icon' do
          is_expected.not_to include('.svg#warning')
        end

        context 'status is not accepted' do
          let(:invitation) { build(:invitation, status: :expired)}
          it 'Should show warning icon' do
            is_expected.to include('.svg#warning')
          end
        end
      end
    end

    context 'credentials tab' do
      let(:delegate_info) { {} }
      let(:credential_start) { true }
      let(:role) { 'Credential Delegate' }
      it 'Should have org name' do
        is_expected.to include("<h1>#{org.name}</h1>")
      end
      it 'Should have npi' do
        is_expected.to include("<span class=\"text-bold\">NPI-2:</span> #{org.npi}")
      end
      it 'Should have role' do
        is_expected.to include("<span class=\"text-bold\">Role:</span> #{role}")
      end
      it 'Should have status' do
        invitation.status = :accepted
        is_expected.to include("<span class=\"text-bold\">Status:</span> #{invitation.status.capitalize}")
      end
      it 'Should have AO' do
        allow(org).to receive(:ao).and_return('John Doe')
        is_expected.to include("<span class=\"text-bold\">Authorized Official:</span> John Doe")
      end
      it 'Should have org id' do
        is_expected.to include("<span class=\"text-bold\">Organization ID:</span> #{org.dpc_api_organization_id}")
      end
      it 'Should not have script tag' do
        is_expected.to_not include('<script')
      end
      it 'Should not have header tag' do
        is_expected.to_not include('<header')
      end
      it 'Should not have credential delegates' do
        is_expected.to_not include('<div id="credential_delegates">')
      end
      it 'Should have credentials' do
        is_expected.to include('<div id="credentials">')
      end
      it 'Should not show warning icon' do
        is_expected.not_to include('.svg#warning')
      end

      context 'status is not accepted' do
        let(:invitation) { build(:invitation, status: :expired)}
        it 'Should show warning icon' do
          is_expected.to include('.svg#warning')
        end
      end
    end
  end
end
