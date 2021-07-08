# frozen_string_literal: true

require 'rails_helper'

RSpec.feature 'deleting users' do
  let!(:admin) { create :admin }

  before(:each) do
    sign_in admin
  end

  scenario 'successfully deleted an external user account' do
    user = create(:user)

    visit user_path(user)

    find('[data-test="delete-user-account"]').click

    expect(page.body).to include('User successfully deleted')
  end
end
