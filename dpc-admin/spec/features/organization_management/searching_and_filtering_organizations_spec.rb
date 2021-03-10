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

    visit organizations_path

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

  scenario 'filter orgs by tags' do
    org1 = create(:organization, name: 'Spongebob')
    org2 = create(:organization, name: 'Fairly Odd Parents')
    org3 = create(:organization, name: 'Grimm Adventures of Billy & Mandy')

    tag1 = create(:tag, name: 'Nick')
    tag2 = create(:tag, name: 'CN')

    org1.tags << tag1
    org2.tags << tag1
    org3.tags << tag2

    tag_id = '#tags_' + tag2.id.to_s

    visit organizations_path

    expect(page.body).to have_content('Spongebob')
    expect(page.body).to have_content('Fairly Odd Parents')
    expect(page.body).to have_content('Grimm Adventures of Billy & Mandy')
    expect(page.body).to have_content('Nick')
    expect(page.body).to have_content('CN')

    find('[data-test="filter-modal-trigger"]').click

    expect(page.body).to have_content('Nick')
    expect(page.body).to have_content('CN')

    find(:css, tag_id, visible: false).set(true)
    find('[data-test="users-filter-submit"]').click

    expect(page.body).not_to have_content('Spongebob')
    expect(page.body).not_to have_content('Fairly Odd Parents')
    expect(page.body).to have_content('Grimm Adventures of Billy & Mandy')

    find('[data-test="filter-modal-trigger"]').click
    find('[data-test="users-filter-submit"]').click

    expect(page.body).to have_content('Spongebob')
    expect(page.body).to have_content('Fairly Odd Parents')
    expect(page.body).to have_content('Grimm Adventures of Billy & Mandy')
  end
end
