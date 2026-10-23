# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'dpc rake tasks', type: :task do
  describe 'dpc:invite_ao' do
    let(:task) { Rake::Task['dpc:invite_ao'] }
    let(:service) { instance_double(AoInvitationService) }
    let(:organization) { ProviderOrganization.new(id: 123, name: 'Test Org') }
    let(:token) { SecureRandom.base58(Invitation::TOKEN_LENGTH) }
    let(:invitation) do
      Invitation.new(id: 456, token:, provider_organization: organization, invitation_type: :authorized_official)
    end

    before do
      task.reenable
      allow(AoInvitationService).to receive(:new).and_return(service)
      allow($stdout).to receive(:write)
    end

    context 'when INVITE is provided and invitation is created successfully' do
      before do
        stub_const('ENV', ENV.to_h.merge('INVITE' => 'Bob,Hoskins,bob@example.com,1111111111'))
        allow(service).to receive(:create_invitation)
          .with('Bob', 'Hoskins', 'bob@example.com', '1111111111')
          .and_return(invitation)
      end

      it 'calls AoInvitationService with the correct arguments' do
        expect(service).to receive(:create_invitation)
          .with('Bob', 'Hoskins', 'bob@example.com', '1111111111')
        task.invoke
      end

      it 'outputs a success message with the organization name' do
        expect { task.invoke }.to output(
          include('Invitation created successfully for Test Org')
        ).to_stdout
      end

      context 'in development environment' do
        before do
          allow(Rails).to receive(:env).and_return(ActiveSupport::StringInquirer.new('development'))
        end

        it 'outputs the invitation URL' do
          expect { task.invoke }.to output(
            include("http://localhost:3100/organizations/123/invitations/456/#{token}/accept")
          ).to_stdout
        end
      end

      context 'in test environment' do
        before do
          allow(Rails).to receive(:env).and_return(ActiveSupport::StringInquirer.new('test'))
          stub_const('ENV', ENV.to_h.merge('ENV' => 'test', 'HOST_NAME' => 'portal.test.dpc.cmscloud.local'))
        end

        it 'does not include localhost in invitation URL for deployed env' do
          expect { task.invoke }.not_to output(
            include('http://localhost:3100')
          ).to_stdout
        end

        it 'does include hostname in invitation URL' do
          expect { task.invoke }.to output(
            include('portal.test.dpc.cmscloud.local')
          ).to_stdout
        end
      end

      context 'in prod environment' do
        before do
          allow(Rails).to receive(:env).and_return(ActiveSupport::StringInquirer.new('production'))
          stub_const('ENV', ENV.to_h.merge('ENV' => 'prod'))
        end

        it 'creates the invitation without computing or printing its URL' do
          expect(invitation).not_to receive(:invite_url)
          expect { task.invoke }.to output("Invitation created successfully for Test Org\n").to_stdout
        end
      end
    end

    context 'when AoInvitationServiceError is raised' do
      before do
        stub_const('ENV', ENV.to_h.merge('INVITE' => 'Bob,Hoskins,bob@example.com,1111111111'))
        allow(service).to receive(:create_invitation)
          .and_raise(AoInvitationServiceError, 'org not found')
      end

      it 'outputs the error message' do
        expect { task.invoke }.to output(
          include('Unable to create invitation: org not found')
        ).to_stdout
      end

      it 'does not raise an error' do
        expect { task.invoke }.not_to raise_error
      end
    end
  end
end
