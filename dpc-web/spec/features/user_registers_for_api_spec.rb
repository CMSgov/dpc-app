# frozen_string_literal: true

require './spec/shared_examples/authenticable_page'

RSpec.feature 'user visits beta registration' do
  it_behaves_like 'an authenticable page', '/dpc_registrations/new'

  context 'when non-registered' do
    let(:user) { build :user }

    before(:each) do
      visit new_dpc_registration_path
      click_link('sign-up')
      fill_in :user_first_name, with: user.first_name
      fill_in :user_last_name, with: user.last_name
      fill_in :user_email, with: user.email
      fill_in :user_password, with: user.password
      fill_in :user_password_confirmation, with: user.password_confirmation
      fill_in :user_organization, with: user.organization
      fill_in :user_address_1, with: user.address_1
      fill_in :user_address_2, with: user.address_2
      fill_in :user_city, with: user.city
      find('#user_state').find("option[value=#{user.state}]").select_option
      fill_in :user_zip, with: user.zip
      click_on('sign-up')
    end

    scenario 'creates account and is redirected to new registration page' do
      expect(page).to have_current_path(new_dpc_registration_path)
    end
  end

  context 'opts in and completes registration form' do
    let(:user) { create :user }
    let(:dpc_registration) { build :dpc_registration }

    before(:each) do
      login_as(user, scope: :user)
      visit new_dpc_registration_path

      check :dpc_registration_opt_in
      click_on('registration_register')
    end

    scenario 'creates registration and is redirected to show page' do
      registration = DpcRegistration.first
      expect(page).to have_current_path(dpc_registration_path(registration))
      expect(page).to have_content('Registration Successful')
      expect(page).to have_content("You've chosen to register for access to the DPC API")
      expect(page).to have_content('your status is pending')
    end
  end

  context 'opts out and completes registration form' do
    let(:user) { create :user }
    let(:dpc_registration) { build :dpc_registration }

    before(:each) do
      login_as(user, scope: :user)
      visit new_dpc_registration_path

      uncheck :dpc_registration_opt_in
      click_on('registration_register')
    end

    scenario 'creates registration and is redirected to show page' do
      registration = DpcRegistration.first
      expect(page).to have_current_path(dpc_registration_path(registration))
      expect(page).to have_content('Registration Successful')
      expect(page).to have_content("You've chosen NOT to register for access to the DPC API")
      expect(page).to_not have_content('your status is complete')
    end
  end
end
