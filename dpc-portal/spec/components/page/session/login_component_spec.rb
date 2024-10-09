# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::Session::LoginComponent, type: :component do
  include ComponentSupport
  describe 'login component' do
    let(:url) { '/portal/' }
    let(:sandbox_url) { 'https://sandbox.dpc.cms.gov/' }
    let(:component) { described_class.new(url) }
    before { render_inline(component) }
    it 'should be a usa section' do
      expect(page).to have_selector('section.usa-section')
    end

    it 'should have a login button' do
      expect(page).to have_selector('button.usa-button')
      expect(page.find('button.usa-button.width-full')).to have_content('Sign in with')
      expect(page).to have_selector('button.usa-button span.login-button__logo')
      expect(page.find('button.usa-button span.login-button__logo')).to have_content('Login.gov')
    end

    it 'login.gov button should post to appropriate url' do
      expect(page.find('form', match: :first)[:action]).to eq url
      expect(page.find('form', match: :first)[:method]).to eq 'post'
    end

    it 'test data button should post to sandbox url' do
      expect(page.find('form.sandbox_url')[:action]).to eq sandbox_url
      expect(page.find('form.sandbox_url')[:method]).to eq 'get'
    end

    it 'should have two columns' do
      expect(page.find_all('.grid-col-12').size).to eq 2
    end
  end
end
