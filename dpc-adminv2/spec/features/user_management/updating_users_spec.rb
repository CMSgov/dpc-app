# frozen_string_literal: true

require 'rails_helper'

RSpec.feature 'updating users' do
  let!(:admin) { create :admin }

  before(:each) do
    sign_in admin
  end

  scenario 'succesfully updating a user\'s attributes' do
    spongebob = create(:user, first_name: 'Spongebob', last_name: 'Squarepants', email: 'spongebob@gmail.com')

    visit user_path(spongebob)

    expect(page.body).to have_content('Spongebob Squarepants')
    expect(page.body).to have_content('spongebob@gmail.com')

    fill_in 'user_first_name', visible: false, with: 'Patrick'
    fill_in 'user_last_name', visible: false, with: 'Star'
    fill_in 'user_email', visible: false, with: 'patrick@gmail.com'

    find('[data-test="user-form-submit"]', visible: false).click

    expect(page.body).to have_content('Patrick Star')
    expect(page.body).to have_content('patrick@gmail.com')
  end

  scenario 'trying to update a user with invalid attributes' do
    spongebob = create(:user, first_name: 'Spongebob', last_name: 'Squarepants', email: 'spongebob@gmail.com')

    visit user_path(spongebob)

    expect(page.body).to have_content('Spongebob Squarepants')
    expect(page.body).to have_content('spongebob@gmail.com')

    fill_in 'user_first_name', visible: false, with: ''
    fill_in 'user_last_name', visible: false, with: ''
    fill_in 'user_email', visible: false, with: 'plankton@example.com'

    find('[data-test="user-form-submit"]', visible: false).click

    expect(page.body).to have_content('Please correct errors:')
    expect(page.body).to have_content('Last name can\'t be blank')
    expect(page.body).to have_content('First name can\'t be blank')
  end
end
