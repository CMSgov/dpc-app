# frozen_string_literal: true

require './spec/shared_examples/authenticable_page'

RSpec.feature 'internal user signs in' do
  let(:internal_user) { build :internal_user }

  # before(:each) do
  #   visit new_internal_user_session_path
  #   click_link 'sign-up'
  # end
end