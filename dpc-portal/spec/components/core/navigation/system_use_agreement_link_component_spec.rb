# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Core::Navigation::SystemUseAgreementLinkComponent, type: :component do
  describe 'SystemUseAgreementLinkComponent' do
    let(:component) { described_class.new }
    it 'renders a link' do
      render_inline(component)
      expect(page).to have_link('System Use Agreement',
                                href: Rails.application.routes.url_helpers.system_use_agreement_path)
    end
  end
end
