# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::Session::LoginComponent, type: :component do
  include ComponentSupport
  describe 'login component' do
    let(:url) { '/portal/' }
    let(:component) { described_class.new(url) }
    before { render_inline(component) }
    it 'should be a section grid' do
      expect(page).to have_selector('section.grid-container')
    end

    it 'should have a login button' do
      expect(page).to have_selector('button.usa-button')
      expect(page.find('button.usa-button')).to have_content('Sign in with')
      expect(page).to have_selector('button.usa-button span.login-button__logo')
      expect(page.find('button.usa-button span.login-button__logo')).to have_content('Login.gov')
    end

    it 'should post to appropriate url' do
      expect(page.find('form')[:action]).to eq url
      expect(page.find('form')[:method]).to eq 'post'
    end

    it 'should have two columns' do
      expect(page.find_all('.grid-col-12').size).to eq 2
    end
  end
end
