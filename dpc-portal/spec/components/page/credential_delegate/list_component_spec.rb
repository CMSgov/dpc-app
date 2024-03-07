# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::CredentialDelegate::ListComponent, type: :component do
  include ComponentSupport
  describe 'html' do
    subject(:html) do
      render_inline(component)
      normalize_space(rendered_content)
    end

    let(:org) { ComponentSupport::MockOrg.new }

    let(:component) { described_class.new(org, invitations, credential_delegates) }

    before do
      render_inline(component)
    end

    context 'No credential delegates' do
      let(:invitations) { [] }
      let(:credential_delegates) { [] }

      let(:expected_html) do
        <<~HTML
          <div>
            <h1>#{org.name}</h1>
            <div class="margin-bottom-5">NPI: #{org.npi}</div>
            <div>
              <div class="shadow-5 border-1px border-base-lighter radius-md margin-bottom-4 padding-x-3 padding-bottom-3">
                <div class="display-flex flex-row flex-justify">
                  <div class="flex-align-self-center">
                    <h2>Credential delegates</h2>
                  </div>
                  <div class="flex-align-self-center">
                    <form class="button_to" method="get" action="/portal/organizations/#{org.path_id}/credential_delegate_invitations/new"><button class="usa-button" type="submit">Assign CD</button></form>
                  </div>
                </div>
                <hr />
                <p>A credential delegate (CD) manages secure API login information. You can assign anyone as a CD.</p>
                <div>
                  <h2>Active</h2>
                  <p>There are no active credential delegates.</p>
                </div>
                <div>
                  <h2>Pending</h2>
                  <p>You will need to send an invited Credential Delegate their invite code when they accept the organization invite. Please do not send the code via email.</p>
                  <p>There are no pending credential delegates.</p>
                </div>
              </div>
            </div>
          </div>
        HTML
      end

      it { is_expected.to match_html_fragment(expected_html) }
    end

    context 'Active credential delegate' do
      let(:invitations) { [] }
      let(:credential_delegates) do
        [CdOrgLink.new]
      end

      it 'has a table' do
        expected_html = <<~HTML
          <table id="active-cd-table" class="width-full usa-table">
            <caption aria-hidden="true" hidden>Active Credential Delegate Table</caption>
            <thead>
              <tr>
                <th scope="row" role="columnheader">
                  Name
                </th>
                <th scope="row" role="columnheader">
                  Email
                </th>
                <th scope="row" role="columnheader">
                  Active since
                </th>
                <th scope="row" role="columnheader">
                </th>
              </tr>
            </thead>
        HTML
        is_expected.to include(normalize_space(expected_html))
      end

      it 'has a row' do
        activated = 1.day.ago.strftime('%m/%d/%Y at %l:%M%p UTC')
        expected_html = <<~HTML
          <tr>
            <td data-sort-value="Bob Hodges">Bob Hodges</td>
            <td data-sort-value="bob@example.com">bob@example.com</td>
            <td data-sort-value="#{activated}">#{activated}</td>
            <td data-sort-value="X">X</td>
          </tr>
        HTML
        is_expected.to include(normalize_space(expected_html))
      end

      it 'has no pending credential delegates' do
        expected_html = '<p>There are no pending credential delegates.</p>'
        is_expected.to include(normalize_space(expected_html))
      end
    end
    context 'Pending credential delegate' do
      let(:invitations) do
        [Invitation.new(invited_given_name: 'Bob', invited_family_name: 'Hodges', invited_email: 'bob@example.com',
                        verification_code: 'ABC123')]
      end
      let(:credential_delegates) { [] }

      it 'has a table' do
        expected_html = <<~HTML
          <table id="pending-cd-table" class="width-full usa-table">
            <caption aria-hidden="true" hidden>Pending Credential Delegate Table</caption>
            <thead>
              <tr>
                <th scope="row" role="columnheader">
                  Name
                </th>
                <th scope="row" role="columnheader">
                  Email
                </th>
                <th scope="row" role="columnheader">
                  Invite code
                </th>
                <th scope="row" role="columnheader">
                </th>
              </tr>
            </thead>
        HTML
        is_expected.to include(normalize_space(expected_html))
      end

      it 'has a row' do
        1.day.ago.strftime('%m/%d/%Y at %l:%M%p UTC')
        expected_html = <<~HTML
          <tr>
            <td data-sort-value="Bob Hodges">Bob Hodges</td>
            <td data-sort-value="bob@example.com">bob@example.com</td>
            <td data-sort-value="ABC123">ABC123</td>
            <td data-sort-value="X">X</td>
          </tr>
        HTML
        is_expected.to include(normalize_space(expected_html))
      end

      it 'has no pending credential delegates' do
        expected_html = '<p>There are no active credential delegates.</p>'
        is_expected.to include(normalize_space(expected_html))
      end
    end
  end
end
