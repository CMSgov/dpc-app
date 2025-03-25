# frozen_string_literal: true

require 'rails_helper'

RSpec.describe CheckConfigCompleteJob, type: :job do
  describe :perform do
    context 'With ID' do
      it 'should call org with explicit id' do
        mock_org = instance_double(ProviderOrganization)
        allow(ProviderOrganization).to receive(:find).with(:fake_id).and_return(mock_org)
        expect(mock_org).to receive(:check_config_complete)
        CheckConfigCompleteJob.perform_now(:fake_id)
      end
    end
    context 'Without ID' do
      it 'should kick off check of all incomplete orgs' do
        checked_mocks = []
        3.times do
          mock_org = instance_double(ProviderOrganization)
          expect(mock_org).to receive(:check_config_complete)
          checked_mocks << mock_org
        end
        expect(ProviderOrganization).to receive(:where).with(config_complete: false).and_return(checked_mocks)
        CheckConfigCompleteJob.perform_now
      end
    end
  end
end
