# frozen_string_literal: true

require 'rails_helper'

RSpec.feature 'searching and filtering users' do
  let!(:internal_user) { create :internal_user }

  before(:each) do
    sign_in internal_user, scope: :internal_user
  end

  scenario 'searching by first name, last name, and email with the text box' do
    create(:user, first_name: 'Crab', last_name: 'Olsen', email: 'co@beach.com')
    create(:user, first_name: 'Fish', last_name: 'Olsen', email: 'fo@boat.com')
    create(:user, first_name: 'Land', last_name: 'Lubber', email: 'll@boat.com')

    visit internal_users_path

    expect(page.body).to have_content('Crab Olsen')
    expect(page.body).to have_content('Fish Olsen')
    expect(page.body).to have_content('Land Lubber')

    fill_in 'keyword', with: 'crab'
    find('[data-test="users-keyword-search-submit"]').click

    expect(page.body).to have_content('Crab Olsen')
    expect(page.body).not_to have_content('Fish Olsen')
    expect(page.body).not_to have_content('Land Lubber')

    fill_in 'keyword', with: 'ols'
    find('[data-test="users-keyword-search-submit"]').click

    expect(page.body).to have_content('Crab Olsen')
    expect(page.body).to have_content('Fish Olsen')
    expect(page.body).not_to have_content('Land Lubber')

    fill_in 'keyword', with: 'boat'
    find('[data-test="users-keyword-search-submit"]').click

    expect(page.body).not_to have_content('Crab Olsen')
    expect(page.body).to have_content('Fish Olsen')
    expect(page.body).to have_content('Land Lubber')

    fill_in 'keyword', with: ''
    find('[data-test="users-keyword-search-submit"]').click

    expect(page.body).to have_content('Crab Olsen')
    expect(page.body).to have_content('Fish Olsen')
    expect(page.body).to have_content('Land Lubber')
  end

  scenario 'filtering users by created_at and requested org with the modal' do
    create(:user, requested_organization: 'Primary Claw', requested_organization_type: 'primary_care_clinic', created_at: 10.days.ago)
    create(:user, requested_organization: 'FeederFish Dental', requested_organization_type: 'speciality_clinic', created_at: 5.days.ago)
    create(:user, requested_organization: 'As Fast As Legs Can Carry', requested_organization_type: 'urgent_care', created_at: 2.days.ago)

    visit internal_users_path

    expect(page.body).to have_content('FeederFish Dental')
    expect(page.body).to have_content('Primary Claw')
    expect(page.body).to have_content('As Fast As Legs Can Carry')

    find('[data-test="filter-modal-trigger"]').click
    fill_in 'requested_org', with: 'feederfish'
    find('[data-test="users-filter-submit"]').click

    expect(page.body).to have_content('FeederFish Dental')
    expect(page.body).not_to have_content('Primary Claw')
    expect(page.body).not_to have_content('As Fast As Legs Can Carry')

    find('[data-test="filter-modal-trigger"]').click
    fill_in 'requested_org', with: ''
    select 'Primary Care Clinic', from: 'requested_org_type'
    find('[data-test="users-filter-submit"]').click

    expect(page.body).not_to have_content('FeederFish Dental')
    expect(page.body).to have_content('Primary Claw')
    expect(page.body).not_to have_content('As Fast As Legs Can Carry')

    find('[data-test="filter-modal-trigger"]').click
    fill_in 'requested_org', with: ''
    select 'All organization types', from: 'requested_org_type'
    fill_in 'created_after', with: 6.days.ago
    fill_in 'created_before', with: 4.days.ago
    find('[data-test="users-filter-submit"]').click

    expect(page.body).to have_content('FeederFish Dental')
    expect(page.body).not_to have_content('Primary Claw')
    expect(page.body).not_to have_content('As Fast As Legs Can Carry')
  end

  scenario 'filter users by tags' do
    user1 = create(:user, first_name: 'Jean Luc', last_name: 'Picard', email: 'picard@gmail.com')
    user2 = create(:user, first_name: 'James T.', last_name: 'Kirk', email: 'kirk@gmail.com')
    user3 = create(:user, first_name: 'Data', last_name: 'Soong', email: 'data@gmail.com')

    tag1 = create(:tag, name: 'TOS')
    tag2 = create(:tag, name: 'TNG')

    user1.tags << tag2
    user2.tags << tag1
    user3.tags << tag2

    tag_id = "#tags_" + tag1.id.to_s

    visit internal_users_path

    expect(page.body).to have_content('Jean Luc Picard')
    expect(page.body).to have_content('James T. Kirk')
    expect(page.body).to have_content('Data Soong')
    expect(page.body).to have_content('TOS')
    expect(page.body).to have_content('TNG')

    find('[data-test="filter-modal-trigger"]').click

    expect(page.body).to have_content('TOS')
    expect(page.body).to have_content('TNG')

    find(:css, tag_id, visible: false).set(true)
    find('[data-test="users-filter-submit"]').click

    expect(page.body).to have_content('James T. Kirk')
    expect(page.body).not_to have_content('Jean Luc Picard')
    expect(page.body).not_to have_content('Data Soong')

    find('[data-test="filter-modal-trigger"]').click
    find('[data-test="users-filter-submit"]').click

    expect(page.body).to have_content('Jean Luc Picard')
    expect(page.body).to have_content('James T. Kirk')
    expect(page.body).to have_content('Data Soong')
  end
end
