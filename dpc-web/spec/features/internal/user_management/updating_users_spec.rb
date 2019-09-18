# frozen_string_literal: true

require 'rails_helper'

RSpec.feature 'updating users' do
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

  scenario 'adding and removing tags from a user' do
    crabby = create(:user, first_name: 'Crab', last_name: 'Olsen', email: 'co@beach.com')

    red_tag = create(:tag, name: 'Red')
    create(:tag, name: 'Yellow')

    visit internal_user_path(crabby)

    within('[data-test="user-tags"]') do
      expect(page).to have_content('No tags')
    end

    select 'Red', from: 'tagging_tag_id'
    find('[data-test="add-tag-submit"]').click

    within('[data-test="user-tags"]') do
      expect(page).to have_content('Red')
    end

    select 'Yellow', from: 'tagging_tag_id'
    find('[data-test="add-tag-submit"]').click

    within('[data-test="user-tags"]') do
      expect(page).to have_content('Red')
      expect(page).to have_content('Yellow')
    end

    tagging = crabby.taggings.find_by(tag_id: red_tag.id)
    find("[data-test=\"delete-tag-#{tagging.id}\"]").click

    within('[data-test="user-tags"]') do
      expect(page).to have_content('Yellow')
    end
  end
end
