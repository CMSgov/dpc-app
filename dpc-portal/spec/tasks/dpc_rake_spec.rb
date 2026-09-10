# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'dpc rake tasks', type: :task do
  describe 'dpc:invite_ao' do
    let(:task) { Rake::Task['dpc:invite_ao'] }
    let(:service) { instance_double(AoInvitationService) }
    let(:organization) { instance_double(ProviderOrganization, name: 'Test Org', id: 'org-123') }
    let(:token) { SecureRandom.base58(Invitation::TOKEN_LENGTH) }
    let(:invitation) do
      instance_double(Invitation, id: 'inv-456', token:, provider_organization: organization)
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
            include("http://localhost:3100/organizations/org-123/invitations/inv-456/#{token}/accept")
          ).to_stdout
        end
      end

      context 'in non-development environment' do
        before do
          allow(Rails).to receive(:env).and_return(ActiveSupport::StringInquirer.new('test'))
        end

        it 'does not output the invitation URL' do
          expect { task.invoke }.not_to output(
            include('http://localhost:3100')
          ).to_stdout
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
