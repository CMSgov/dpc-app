# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Core::Navigation::CspHelpLinksComponent, type: :component do
  let(:component) { described_class.new }
  it 'renders Login.gov help link' do
    render_inline(component)
    expect(page).to have_link('Login.gov Help Center', href: 'https://www.login.gov/help')
  end

  it 'renders CLEAR support link' do
    render_inline(component)
    expect(page).to have_link('CLEAR Support', href: 'https://www.clearme.com/support')
  end

  it 'renders ID.me help link' do
    render_inline(component)
    expect(page).to have_link('ID.me Help Center', href: 'https://help.id.me/hc/en-us')
  end
end
