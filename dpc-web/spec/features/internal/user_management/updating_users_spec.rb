# frozen_string_literal: true

require 'rails_helper'

RSpec.feature 'updating users' do
  let!(:internal_user) { create :internal_user }

  before(:each) do
    sign_in internal_user, scope: :internal_user
  end

  scenario 'successfully updating a user\'s attributes ' do
    crabby = create(:user, first_name: 'Crab', last_name: 'Olsen', email: 'co@beach.com')
    org = create(:organization)

    visit edit_internal_user_path(crabby)

    expect(page.body).to have_content('Crab Olsen')

    select org.name, from: 'user_organization_ids'
    fill_in 'user_first_name', with: 'Crabby'
    fill_in 'user_last_name', with: 'Graham'
    fill_in 'user_email', with: 'newemail@example.com'

    find('[data-test="user-form-submit"]').click

    # No longer on edit page
    expect(page).not_to have_css('[data-test="user-form-submit"]')
    expect(page.body).to have_content('Crabby Graham')
    expect(page.body).to have_content('newemail@example.com')
    expect(page.body).to have_content(org.name)
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

  scenario 'assigning new organization from a user\'s requested organization' do
    crabby = create(:user, first_name: 'Crab', last_name: 'Olsen', email: 'co@beach.com',
                           requested_organization: 'Happy Health', requested_organization_type: 3,
                           address_1: '55 Euphoria Dr', address_2: '#1', city: 'Blytheville',
                           state: 'CO', zip: '80313', requested_num_providers: 999)

    visit internal_user_path(crabby)

    find('[data-test="assign-org-link"]').click

    # On edit user page
    expect(page.body).to have_content('Happy Health')
    expect(page.body).to have_content('Inpatient Facility')

    find('[data-test="convert-org-link"]').click

    # On new org page
    expect(page).to have_css('[data-test="new-org-page"]')
    name_field = find(:css, 'input#organization_name').value
    expect(name_field).to eq('Happy Health')
    type_field = find(:css, 'select#organization_organization_type').value
    expect(type_field).to eq('inpatient_facility')
    num_field = find(:css, 'input#organization_num_providers').value
    expect(num_field).to eq('999')
    address_1_field = find(:css, 'input#organization_address_attributes_street').value
    expect(address_1_field).to have_content('55 Euphoria Dr')
    address_2_field = find(:css, 'input#organization_address_attributes_street_2').value
    expect(address_2_field).to have_content('#1')
    city_field = find(:css, 'input#organization_address_attributes_city').value
    expect(city_field).to have_content('Blytheville')
    state_field = find(:css, 'select#organization_address_attributes_state').value
    expect(state_field).to have_content('CO')
    zip_field = find(:css, 'input#organization_address_attributes_zip').value
    expect(zip_field).to have_content('80313')
    find('[data-test="form-submit"]').click

    # Back on user edit page
    expect(page).to have_select('user_organization_ids', selected: 'Happy Health')
    find('[data-test="user-form-submit"]').click

    expect(page).not_to have_css('[data-test="user-form-submit"]')
    expect(page).not_to have_css('[data-test="assign-org-link"]')
    expect(page.body).to have_content('Happy Health')

    click_link 'Happy Health'

    expect(page).to have_content('Inpatient Facility')
    expect(page).to have_content('55 Euphoria Dr')
    expect(page).to have_content('#1')
    expect(page).to have_content('Blytheville')
    expect(page).to have_content('CO')
    expect(page).to have_content('80313')
    expect(page).to have_content('999')
  end

  scenario 'sending sandbox email to user added to a sandbox org' do
    crabby = create(:user, first_name: 'Crab', last_name: 'Olsen', email: 'co@beach.com')
    org = create(:organization, :sandbox_enabled)

    mailer = double(UserMailer)
    allow(UserMailer).to receive(:with).with(user: crabby, organization: org).and_return(mailer)
    allow(mailer).to receive(:organization_sandbox_email).and_return(mailer)
    allow(mailer).to receive(:deliver_later)

    visit edit_internal_user_path(crabby)

    expect(page.body).to have_content('Crab Olsen')

    select org.name, from: 'user_organization_ids'

    find('[data-test="user-form-submit"]').click

    expect(page).not_to have_css('[data-test="user-form-submit"]')
    expect(mailer).to have_received(:organization_sandbox_email).once
  end
end
