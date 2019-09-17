# frozen_string_literal: true

RSpec.feature 'searching and filtering users' do
  let!(:internal_user) { create :internal_user }

  before(:each) do
    sign_in internal_user, scope: :internal_user
  end

  scenario 'successfully updating a user\'s attributes ' do
    crabby = create(:user, first_name: 'Crab', last_name: 'Olsen', email: 'co@beach.com')

    visit edit_internal_user_path(crabby)

    expect(page.body).to have_content('Crab Olsen')

    fill_in 'user_first_name', with: 'Crabby'
    fill_in 'user_last_name', with: 'Graham'
    fill_in 'user_email', with: 'newemail@example.com'

    find('[data-test="user-form-submit"]').click

    # No longer on edit page
    expect(page).not_to have_css('[data-test="user-form-submit"]')
    expect(page.body).to have_content('Crabby Graham')
    expect(page.body).to have_content('newemail@example.com')
  end

  scenario 'trying to update a user with invalid attributes ' do
    crabby = create(:user, first_name: 'Crab', last_name: 'Olsen', email: 'co@beach.com')

    visit edit_internal_user_path(crabby)

    expect(page.body).to have_content('Crab Olsen')

    fill_in 'user_first_name', with: ''
    fill_in 'user_last_name', with: 'Graham'

    find('[data-test="user-form-submit"]').click

    # Still on edit page
    expect(page).to have_css('[data-test="user-form-submit"]')
  end
end
