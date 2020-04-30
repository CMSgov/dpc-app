# frozen_string_literal: true

require 'rails_helper'

RSpec.feature 'deleting users' do
  include APIClientSupport
  let!(:internal_user) { create :internal_user }

  before(:each) do
    sign_in internal_user, scope: :internal_user
  end

  scenario 'successfully deleted an external user account' do
    user = create(:user)

    visit internal_user_path(user)

    find('[data-test="delete-user-account"]').click
    page.driver.browser.switch_to.alert.accept

    expect(page.body).to include('User successfully deleted.')
  end
end
