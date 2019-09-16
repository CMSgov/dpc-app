# frozen_string_literal: true

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
    create(:user, organization: 'Primary Claw', organization_type: 'primary_care_clinic', created_at: 10.days.ago)
    create(:user, organization: 'FeederFish Dental', organization_type: 'speciality_clinic', created_at: 5.days.ago)
    create(:user, organization: 'As Fast As Legs Can Carry', organization_type: 'urgent_care', created_at: 2.days.ago)

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
    select '', from: 'requested_org_type'
    fill_in 'created_after', with: 6.days.ago
    fill_in 'created_before', with: 4.days.ago
    find('[data-test="users-filter-submit"]').click

    expect(page.body).to have_content('FeederFish Dental')
    expect(page.body).not_to have_content('Primary Claw')
    expect(page.body).not_to have_content('As Fast As Legs Can Carry')
  end
end
