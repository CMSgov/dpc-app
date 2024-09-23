# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::Invitations::SuccessComponent, type: :component do
  describe 'ao' do
    let(:invitation) { create(:invitation, :ao) }
    let(:component) { described_class.new(invitation.provider_organization, invitation, 'Paola', 'Pineiro') }

    before { render_inline(component) }

    it 'should have step component at step 4' do
      expect(page).to have_selector('.usa-step-indicator__current-step')
      expect(page.find('.usa-step-indicator__current-step').text).to eq '4'
    end

    it "should not have the invited user's name" do
      expect(page).not_to have_text('Paola Pineiro')
    end
  end

  describe 'cd' do
    let(:invitation) { create(:invitation, :cd) }
    let(:org) { ComponentSupport::MockOrg.new }
    let(:component) { described_class.new(org, invitation, 'Paola', 'Pineiro') }

    before { render_inline(component) }

    it 'should have step component at step 4' do
      expect(page).to have_selector('.usa-step-indicator__current-step')
      expect(page.find('.usa-step-indicator__current-step').text).to eq '4'
    end

    it "should have the organization's name" do
      expect(page).to have_text(org.name)
    end
  end
end
