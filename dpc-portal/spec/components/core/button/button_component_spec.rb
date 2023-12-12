# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Core::Button::ButtonComponent, type: :component do
  subject(:component) { described_class.new(text: 'text', on_click: 'on_click') }

  it 'is a button' do
    render_component
    expect(page).to have_selector('button.usa-button')
  end
end
