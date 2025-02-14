# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Core::SiteHeader::Component, type: :component do
  describe 'site header' do
    context 'while logged out' do
      let(:component) { described_class.new(logged_in: false) }

      it 'should render gov banner' do
        render_inline(component)
        expect(page).to have_selector('.usa-banner')
      end

      it 'should render site name' do
        render_inline(component)
        expect(page.find('.usa-logo--text')).to have_content('Data at the Point of Care')
      end

      it 'should not render logout button' do
        render_inline(component)
        expect(page).not_to have_selector('.usa-header button')
      end
    end

    context 'while logged in' do
      let(:component) { described_class.new(logged_in: true) }

      it 'should render gov banner' do
        render_inline(component)
        expect(page).to have_selector('.usa-banner')
      end

      it 'should render site name' do
        render_inline(component)
        expect(page.find('.usa-logo--text')).to have_content('Data at the Point of Care')
      end

      it 'should render logout button' do
        render_inline(component)
        expect(page).to have_selector('header button')
        expect(page.find('.usa-header button')).to have_content('Log Out')
      end
    end

    context 'border toggle is true' do
      let(:component) { described_class.new(border: true) }

      it 'should render a bottom border' do
        render_inline(component)
        expect(page).to have_selector('.height-2.width-full.bg-base-dark')
      end
    end

    context 'border toggle is false' do
      let(:component) { described_class.new(border: false) }

      it 'should not render a bottom border' do
        render_inline(component)
        expect(page).not_to have_selector('.height-2.width-full.bg-base-dark')
      end
    end
  end
end
