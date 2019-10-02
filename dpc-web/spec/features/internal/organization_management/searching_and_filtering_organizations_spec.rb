# frozen_string_literal: true

require 'rails_helper'

RSpec.feature 'searching and filtering organizations' do
  let!(:internal_user) { create :internal_user }

  before(:each) do
    sign_in internal_user, scope: :internal_user
  end

  scenario 'searching by name with the text box' do
    create(:organization, name: 'Crabby Health')
    create(:organization, name: 'Fishy Health')
    create(:organization, name: 'Unrelated Specialty Doc')

    visit internal_organizations_path

    expect(page.body).to have_content('Crabby Health')
    expect(page.body).to have_content('Fishy Health')
    expect(page.body).to have_content('Unrelated Specialty Doc')

    fill_in 'keyword', with: 'crab'
    find('[data-test="organizations-keyword-search-submit"]').click


    expect(page.body).to have_content('Crabby Health')
    expect(page.body).not_to have_content('Fishy Health')
    expect(page.body).not_to have_content('Unrelated Specialty Doc')

    fill_in 'keyword', with: 'healt'
    find('[data-test="organizations-keyword-search-submit"]').click

    expect(page.body).to have_content('Crabby Health')
    expect(page.body).to have_content('Fishy Health')
    expect(page.body).not_to have_content('Unrelated Specialty Doc')

    fill_in 'keyword', with: ''
    find('[data-test="organizations-keyword-search-submit"]').click

    expect(page.body).to have_content('Crabby Health')
    expect(page.body).to have_content('Fishy Health')
    expect(page.body).to have_content('Unrelated Specialty Doc')
  end
end
