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
    let(:component) { described_class.new(org, [], [], [], show_cds) }

    before do
      render_inline(component)
    end

    context 'show cds' do
      let(:show_cds) { true }
      it 'Should have org name' do
        is_expected.to include("<h1>#{org.name}</h1>")
      end
      it 'Should have npi' do
        is_expected.to include(%(<div class="margin-bottom-5">NPI: #{org.npi}</div>))
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
    end

    context 'not show cds' do
      let(:show_cds) { false }
      it 'Should have org name' do
        is_expected.to include("<h1>#{org.name}</h1>")
      end
      it 'Should have npi' do
        is_expected.to include(%(<div class="margin-bottom-5">NPI: #{org.npi}</div>))
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
    end
  end
end
