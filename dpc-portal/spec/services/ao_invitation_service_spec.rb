# frozen_string_literal: true

require 'spec_helper'
require 'rails_helper'

describe AoInvitationService do
  describe 'create invitation' do
    it 'verifies the npi' do
      expect(cpi_api_gw_client).to receive(:org_info)
        .with(organization_npi)
        .and_return({
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
                    })
    end
    it 'does not create if org with npi exists'
    it 'creates org wich PECOS name'
    it 'creates invitation'
    it 'sends email'
  end
end
