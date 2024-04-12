# frozen_string_literal: true

require 'spec_helper'
require 'rails_helper'

describe AoInvitationService do
  let(:client) { instance_double(CpiApiGatewayClient) }
  let(:service) { AoInvitationService.new }
  let(:organization_npi) { '3624913885' }
  before do
    allow(CpiApiGatewayClient).to receive(:new).and_return(client)
  end

  describe 'org_name' do
    it 'fetches the name' do
      stub_cpi_api_gw
      name = service.org_name(organization_npi)
      expect(name).to eq 'Health Hut'
    end

    it 'raises exception if no org' do
      expect(client).to receive(:org_info)
        .with(organization_npi)
        .and_return({ 'code' => '404' })
      expect do
        service.org_name(organization_npi)
      end.to raise_error(AoInvitationServiceError)
    end

    it 'raises exception if bad npi' do
      luhnacy = class_double('Luhnacy').as_stubbed_const
      expect(luhnacy).to receive(:valid?)
        .with("80840#{organization_npi}")
        .and_return(false)
      expect do
        service.org_name(organization_npi)
      end.to raise_error(AoInvitationServiceError)
    end
  end

  describe 'create invitation' do
    let(:params) { %w[Bob Hoskins bob@example.com] }

    it 'creates org if not exist' do
      stub_cpi_api_gw
      expect do
        service.create_invitation(*params, organization_npi)
      end.to change { ProviderOrganization.count }.by 1
    end

    it 'does not create if org with npi exists' do
      create(:provider_organization, npi: organization_npi)
      expect do
        service.create_invitation(*params, organization_npi)
      end.to change { ProviderOrganization.count }.by 0
    end

    it 'creates org wich PECOS name' do
      stub_cpi_api_gw
      service.create_invitation(*params, organization_npi)
      organization = ProviderOrganization.find_by(npi: organization_npi)
      expect(organization.name).to eq 'Health Hut'
    end

    it 'creates invitation' do
      stub_cpi_api_gw
      expect do
        service.create_invitation(*params, organization_npi)
      end.to change { Invitation.count }.by 1
      organization = ProviderOrganization.find_by(npi: organization_npi)
      matching_invitation = Invitation.where(invited_given_name: params[0],
                                             invited_family_name: params[1],
                                             invited_email: params[2],
                                             provider_organization: organization,
                                             invitation_type: 'authorized_official')
      expect(matching_invitation.count).to eq 1
    end

    it 'sends an invitation email on success' do
      stub_cpi_api_gw
      mailer = double(InvitationMailer)
      expect(InvitationMailer).to receive(:with).with(invitation: instance_of(Invitation)).and_return(mailer)
      expect(mailer).to receive(:invite_ao).and_return(mailer)
      expect(mailer).to receive(:deliver_now)
      service.create_invitation(*params, organization_npi)
    end
  end

  def stub_cpi_api_gw
    expect(client).to receive(:org_info)
      .with(organization_npi)
      .and_return(org_stanza)
  end

  def org_stanza
    {
      'provider' => {
        'providerType' => 'org',
        'orgName' => 'Health Hut',
        'npi' => organization_npi.to_s,
        'addresses' => [
          {
            'addressLine1' => '1234 Company Ln.',
            'addressLine2' => '#2222',
            'city' => 'Fairfax',
            'stateCode' => 'VA',
            'zip' => '23230',
            'dataIndicator' => 'CURRENT'
          }
        ]
      }
    }
  end
end
