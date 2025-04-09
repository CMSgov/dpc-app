# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::Organization::OrganizationListComponent, type: :component do
  describe 'html' do
    subject(:html) do
      render_inline(component)
      normalize_space(rendered_content)
    end

    let(:ao_or_cd_ref) { 'as an Authorized Official (AO) and/or Credential Delegate (CD)' }
    def normalize_space(str)
      str.gsub(/^ +/, '').gsub("\n", '')
    end

    context 'when is ao' do
      let(:component) { described_class.new(ao_or_cd: :ao, links: []) }

      before do
        render_inline(component)
      end

      it 'should have reference to Authorized Official' do
        is_expected.to include(normalize_space(ao_or_cd_ref))
      end
    end
    context 'when has no options and is cd' do
      let(:component) { described_class.new(ao_or_cd: :cd, links: []) }

      before do
        render_inline(component)
      end

      it 'should have generic AO/CD message' do
        is_expected.to include(normalize_space(ao_or_cd_ref))
      end

      it 'should say no organizations' do
        empty_npi_search = "<p>You don't have any organizations to show.</p>"
        is_expected.to include(normalize_space(empty_npi_search))
      end
    end

    context 'when has one option' do
      let(:component) do
        org = ProviderOrganization.new(name: 'name', npi: 'npi', id: '5', terms_of_service_accepted_at: 2.days.ago)
        link = CdOrgLink.new(provider_organization: org)
        described_class.new(ao_or_cd: :cd, links: [link])
      end
      let(:expected_html) do
        <<~HTML
                     <table class="usa-table usa-table--borderless organizations-list table-bg-transparent" style="table-layout: fixed; width: 100%;">
                       <thead>
                         <tr>
                           <th scope="col" style="width: 50%;">Organization Name</th>
                           <th scope="col" style="width: 16.67%;">NPI-2</th>
                           <th scope="col" style="width: 8.33%;">Role</th>
                           <th scope="col" style="width: 25%;">Status</th>
                         </tr>
                       </thead>
                       <tbody>
                         <tr class="organizations-list-row">
                           <th scope="row" style="width: 50%;">
                             <a class="display-block maxw-full visited:text-blue text-underline truncate-text-ellipsis" href="/portal/organizations/5">
                               name
                             </a>
                           </th>
                         <td style="width: 16.67%;"> npi </td>
                         <td style="width: 8.33%;">
                             CD
                         </td>
                         <td style="width: 25%;">
                             <div class="clearfix">
        HTML
      end

      before do
        render_inline(component)
      end

      it 'should have one row in table' do
        is_expected.to include(normalize_space(expected_html))
      end
    end
  end
end
