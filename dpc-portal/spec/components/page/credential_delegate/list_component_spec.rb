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

    let(:component) { described_class.new(org, pending_invitations, expired_invitations, credential_delegates) }

    before do
      render_inline(component)
    end

    context 'No credential delegates' do
      let(:pending_invitations) { [] }
      let(:expired_invitations) { [] }
      let(:credential_delegates) { [] }

      let(:expected_html) do
        <<~HTML
          <div id="credential_delegates">
            <div>
              <div class="bg-white shadow-5 border-1px border-base-lighter radius-md margin-bottom-4 padding-x-3 padding-bottom-3">
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
                  <h2>Pending invitations</h2>
                  <p>There are no pending credential delegates.</p>
                </div>
                <div>
                  <h2>Expired invitations</h2>
                  <p>These invites expired. You can resend the invite to give them more time to accept.</p>
                  <p>You have no expired invitations.</p>
                </div>
              </div>
            </div>
          </div>
        HTML
      end

      it { is_expected.to match_html_fragment(expected_html) }
    end

    context 'Active credential delegate' do
      let(:user) { User.new(given_name: 'Bob', family_name: 'Hodges', email: 'bob@example.com') }
      let(:invitation) do
        Invitation.new(invited_given_name: 'Bob', invited_family_name: 'Hodges', invited_email: 'bob@example.com')
      end
      let(:pending_invitations) { [] }
      let(:expired_invitations) { [] }
      let(:credential_delegates) { [CdOrgLink.new(user:, invitation:)] }

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
              </tr>
            </thead>
        HTML
        is_expected.to include(normalize_space(expected_html))
      end

      it 'has a row' do
        activated = credential_delegates.first.created_at
        expected_html = <<~HTML
          <tr>
            <td data-sort-value="Bob Hodges">Bob Hodges</td>
            <td data-sort-value="bob@example.com">bob@example.com</td>
            <td data-sort-value="#{activated}">#{activated}</td>
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
      let(:pending_invitations) do
        [Invitation.new(invited_given_name: 'Bob', invited_family_name: 'Hodges', invited_email: 'bob@example.com',
                        id: 3, created_at: 1.day.ago)]
      end
      let(:expired_invitations) { [] }
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
                </th>
              </tr>
            </thead>
        HTML
        is_expected.to include(normalize_space(expected_html))
      end

      it 'has a row' do
        expected_html = <<~HTML
          <tr>
            <td data-sort-value="Bob Hodges">Bob Hodges</td>
            <td data-sort-value="bob@example.com">bob@example.com</td>
        HTML
        delete_invitation = <<~HTML
          <form class="button_to" method="post" action="/portal/organizations/2/credential_delegate_invitations/3">
           <input type="hidden" name="_method" value="delete" autocomplete="off" />
           <button class="usa-button" type="submit">Yes, delete invite</button>
          </form>
        HTML
        is_expected.to include(normalize_space(expected_html))
        is_expected.to include(normalize_space(delete_invitation))
      end

      it 'has no active credential delegates' do
        expected_html = '<p>There are no active credential delegates.</p>'
        is_expected.to include(normalize_space(expected_html))
      end

      it 'has no expired invitations' do
        expected_html = '<p>You have no expired invitations.</p>'
        is_expected.to include(normalize_space(expected_html))
      end
    end

    context 'Expired invitations' do
      let(:pending_invitations) { [] }
      let(:expired_invitations) do
        [Invitation.new(invited_given_name: 'Bob', invited_family_name: 'Hodges', invited_email: 'bob@example.com',
                        id: 3, created_at: 3.days.ago)]
      end
      let(:credential_delegates) { [] }

      it 'has a table' do
        expired_at = expired_invitations.first.expired_at
        expected_html = <<~HTML
          <table id="expired-invitation-table" class="width-full usa-table">
            <caption aria-hidden="true" hidden>Expired Invitation Table</caption>
            <thead>
              <tr>
                <th scope="row" role="columnheader">Name</th>
                <th scope="row" role="columnheader">Email</th>
                <th scope="row" role="columnheader">Expired on</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td data-sort-value="Bob Hodges">Bob Hodges</td>
                <td data-sort-value="bob@example.com">bob@example.com</td>
                <td data-sort-value="#{expired_at}">#{expired_at}</td>
              </tr>
            </tbody>
          </table>
        HTML
        is_expected.to include(normalize_space(expected_html))
      end

      it 'has a row' do
        expired_at = expired_invitations.first.expired_at
        expected_html = <<~HTML
          <tr>
            <td data-sort-value="Bob Hodges">Bob Hodges</td>
            <td data-sort-value="bob@example.com">bob@example.com</td>
            <td data-sort-value="#{expired_at}">#{expired_at}</td>
          </tr>
        HTML
        is_expected.to include(normalize_space(expected_html))
      end

      it 'has no pending credential delegates' do
        expected_html = '<p>There are no pending credential delegates.</p>'
        is_expected.to include(normalize_space(expected_html))
      end
    end
  end
end
