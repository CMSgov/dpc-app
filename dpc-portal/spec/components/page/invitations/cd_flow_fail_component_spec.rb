# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::Invitations::CdFlowFailComponent, type: :component do
  let(:invitation) { create(:invitation, :cd) }
  let(:reason) { :bad_npi }
  let(:step) { 1 }
  let(:component) { described_class.new(invitation, reason, step) }
  before { render_inline(component) }

  describe 'steps' do
    context 'step 1' do
      let(:step) { 1 }

      it 'shows the correct step header' do
        expected_text = 'Step 2 of 4'
        node = page.find('.usa-step-indicator__heading')
        result_text = node.text.split.map(&:strip).join(' ')
        expect(result_text).to include(expected_text)
      end
    end

    context 'step 2' do
      let(:step) { 2 }

      it 'should show CD step 2 header' do
        expected_text = 'Step 3 of 4'
        node = page.find('.usa-step-indicator__heading')
        result_text = node.text.split.map(&:strip).join(' ')
        expect(result_text).to include(expected_text)
      end
    end
  end

  describe 'name does not match invite' do
    let(:reason) { 'pii_mismatch' }

    it 'should show pii_mismatch error message' do
      node = page.find('.usa-alert__text')
      expect(node.text).to include(I18n.t('verification.pii_mismatch_status'))
    end
  end

  describe 'fail to proof' do
    let(:reason) { 'fail_to_proof' }

    it 'should have url to login' do
      path = "organizations/#{invitation.provider_organization.id}/invitations/#{invitation.id}/login"
      url = "http://test.host/portal/#{path}"
      expect(page.find('form')[:action]).to eq(url)
      expect(page.find('form')[:method]).to eq('post')
    end
  end
end
