# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::Invitations::SuccessComponent, type: :component do
  let(:component) { described_class.new(invitation.provider_organization, invitation) }
  before { render_inline(component) }
  describe 'ao' do
    let(:invitation) { create(:invitation, :ao) }
    it 'should have step component at step 4' do
      expect(page).to have_selector('.usa-step-indicator__current-step')
      expect(page.find('.usa-step-indicator__current-step').text).to eq '4'
    end
  end
end
