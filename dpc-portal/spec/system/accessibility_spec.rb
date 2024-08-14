require 'rails_helper'

RSpec.describe 'Accessibility', type: :system do
  include  Devise::Test::IntegrationHelpers
  before do
    driven_by(:selenium_headless)
  end
  context do :login
    it 'shows login page ok' do
      visit '/users/sign_in'
      expect(page).to have_text('Sign in')
      #      expect(page).to be_axe_clean
    end
  end

  context do :organizations
    let!(:user) { create(:user) }
    let!(:org) { create(:provider_organization) }
    before { sign_in user }
    it 'should work' do
      visit '/organizations'
      expect(page).to have_text("You don't have any organizations to show.")
      #      expect(page).to be_axe_clean
    end
    context :with_organizations do
      let!(:ao_org_link) { create(:ao_org_link, user:, provider_organization: org) }
      it 'should show organizations' do
        visit '/organizations'
        expect(page).to_not have_text("You don't have any organizations to show.")
        #        expect(page).to be_axe_clean
      end
      it 'should show tos' do
        visit "/organizations/#{org.id}"
        expect(page).to have_text("Terms of Service")
      end
      context :after_tos do
        before { org.update!(terms_of_service_accepted_by: user) }
        it 'should show organization page with no cds or credentials' do
          visit "/organizations/#{org.id}"
          expect(page).to have_text("You can assign anyone as a CD")
          expect(page).to have_css('#credential_delegates')
          expect(page).to_not have_css('#credentials')
          expect(page).to_not have_css('#active-cd-table')
          expect(page).to_not have_css('#pending-cd-table')
          page.execute_script('make_current(1)')
          expect(page).to have_text('you must create a unique client token')
          expect(page).to_not have_css('#credential_delegates')
          expect(page).to have_css('#credentials')
        end
        context :with_credential_delegates do
          let(:active_cd) { create(:user) }
          let!(:active_link) { create(:cd_org_link, user: active_cd, provider_organization: org) }
          let!(:invitation) { create(:invitation, :cd, provider_organization: org, invited_by: user) }
          it 'should show credential delegate tables' do
            visit "/organizations/#{org.id}"
            expect(page).to have_text("You can assign anyone as a CD")
            expect(page).to have_css('#active-cd-table')
            expect(page).to have_css('#pending-cd-table')
          end
        end
        context :with_credentials do
        end
      end
    end
  end
end
