# frozen_string_literal: true

require 'spec_helper'
require 'rails_helper'

describe AoInvitationService do
  let(:client) { CpiApiGatewayClient.new }
  let(:service) { AoInvitationService.new }
  let(:organization_npi) { '3624913885' }
  let(:invitation_id) { 123 }

  describe 'org_name' do
    it 'fetches the name' do
      name = service.org_name(organization_npi)
      expect(name).to eq "Organization #{organization_npi}"
    end

    it 'raises exception if no org' do
      missing_org_npi = '3299073577'
      expect do
        service.org_name(missing_org_npi)
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

    it 'creates org with PECOS name' do
      service.create_invitation(*params, organization_npi)
      organization = ProviderOrganization.find_by(npi: organization_npi)
      expect(organization.name).to eq "Organization #{organization_npi}"
    end

    it 'creates invitation' do
      expect do
        service.create_invitation(*params, organization_npi)
      end.to change { Invitation.count }.by 1
      organization = ProviderOrganization.find_by(npi: organization_npi)
      matching_invitation = Invitation.where(invited_email: params[2],
                                             provider_organization: organization,
                                             invitation_type: :authorized_official)
      expect(matching_invitation.count).to eq 1
    end

    it 'sends an invitation email on success' do
      mailer = double(InvitationMailer)
      expect(InvitationMailer).to receive(:with)
        .with(invitation: instance_of(Invitation), given_name: params[0], family_name: params[1])
        .and_return(mailer)
      expect(mailer).to receive(:invite_ao).and_return(mailer)
      expect(mailer).to receive(:deliver_now)
      service.create_invitation(*params, organization_npi)
    end

    it 'logs on success' do
      organization = instance_double(ProviderOrganization)
      expect(ProviderOrganization).to receive(:find_or_create_by).with(npi: organization_npi).and_return(organization)

      invitation_id = 123
      invitation = instance_double(Invitation)
      expect(invitation).to receive(:id).and_return(invitation_id)
      expect(Invitation).to receive(:create).and_return(invitation)

      mailer = double(InvitationMailer)
      expect(InvitationMailer).to receive(:with).with(invitation:, given_name: params[0], family_name: params[1])
                                                .and_return(mailer)
      expect(mailer).to receive(:invite_ao).and_return(mailer)
      expect(mailer).to receive(:deliver_now)

      allow(Rails.logger).to receive(:info)
      expect(Rails.logger).to receive(:info).with(['Authorized Official invited',
                                                   { actionContext: LoggingConstants::ActionContext::Registration,
                                                     actionType: LoggingConstants::ActionType::AoInvited,
                                                     invitation: invitation_id }])
      service.create_invitation(*params, organization_npi)
    end
  end
end
