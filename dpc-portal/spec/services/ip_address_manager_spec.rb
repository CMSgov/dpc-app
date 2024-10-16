# frozen_string_literal: true

require 'rails_helper'

RSpec.describe IpAddressManager do
  include DpcClientSupport

  let(:api_id) { SecureRandom.uuid }
  let(:manager) { IpAddressManager.new(api_id) }
  describe '#create_ip_address' do
    let(:ip_address_params) { { label: 'Public IP 1', ip_address: '136.226.19.87' } }

    context 'with valid params' do
      context 'successful API request' do
        it 'responds true' do
          response = { 'id' => '570f7a71-0e8f-48a1-83b0-c46ac35d6ef3' }
          stub_self_returning_api_client(message: :create_ip_address,
                                         response:,
                                         with: [api_id, { params: ip_address_params }])

          new_ip_address = manager.create_ip_address(**ip_address_params)

          expect(new_ip_address[:response]).to eq(true)
          expect(new_ip_address[:message]).to eq(response)
        end
      end

      context 'failed API request' do
        it 'responds false' do
          response = { 'id' => nil }
          stub_self_returning_api_client(message: :create_ip_address,
                                         success: false,
                                         response:,
                                         with: [api_id, { params: ip_address_params }])

          new_ip_address = manager.create_ip_address(**ip_address_params)

          expect(new_ip_address[:response]).to eq(false)
          expect(new_ip_address[:message]).to eq(response)
        end
        it 'indicates when too many ip addresses reached' do
          response = 'Max Ips for organization reached: 8'
          stub_self_returning_api_client(message: :create_ip_address,
                                         success: false,
                                         response:,
                                         with: [api_id, { params: ip_address_params }])

          new_ip_address = manager.create_ip_address(**ip_address_params)

          expect(new_ip_address[:response]).to eq(false)
          expect(new_ip_address[:errors]).to eq(root: 'Max Ips for organization reached')
        end
      end
    end

    context 'with invalid params' do
      it 'has errors on all missing fields' do
        new_ip_address = manager.create_ip_address(ip_address: '', label: '')
        expect(new_ip_address[:response]).to eq(false)
        error_msg = 'Cannot be blank'
        expect(new_ip_address[:errors]).to eq(label: error_msg, ip_address: error_msg)
      end
      context 'label over 25 characters' do
        it 'response with error' do
          ip_address_params[:label] = 'aaaaabbbbbcccccdddddeeeeefffff'
          stub_self_returning_api_client(message: :create_ip_address,
                                         response: { 'id' => '570f7a71-0e8f-48a1-83b0-c46ac35d6ef3' },
                                         with: [api_id, { params: ip_address_params }])

          new_ip_address = manager.create_ip_address(**ip_address_params)

          expect(new_ip_address[:response]).to eq(false)
          expect(new_ip_address[:errors]).to eq(label: 'Label must be 25 characters or fewer')
        end
      end

      context 'invalid IP' do
        it 'response with error' do
          ip_address_params[:ip_address] = '333.333.333.333'
          stub_self_returning_api_client(message: :create_ip_address,
                                         response: { 'id' => '570f7a71-0e8f-48a1-83b0-c46ac35d6ef3' },
                                         with: [api_id, { params: ip_address_params }])

          new_ip_address = manager.create_ip_address(**ip_address_params)

          expect(new_ip_address[:response]).to eq(false)
          expect(new_ip_address[:errors]).to eq(ip_address: 'invalid IP address')
        end
      end
    end
  end

  describe '#delete_ip_address' do
    context 'successful API request' do
      it 'responds true' do
        addr_guid = SecureRandom.uuid
        stub_self_returning_api_client(message: :delete_ip_address,
                                       with: [api_id, addr_guid])

        response = manager.delete_ip_address(id: addr_guid)

        expect(response).to be true
      end
    end

    context 'failed API request' do
      it 'responds false' do
        addr_guid = SecureRandom.uuid
        stub_self_returning_api_client(message: :delete_ip_address,
                                       success: false,
                                       with: [api_id, addr_guid])

        response = manager.delete_ip_address(id: addr_guid)

        expect(response).to be false
      end
    end
  end

  describe '#ip_addresses' do
    context 'successful API request' do
      it 'returns array of IP addresses' do

        addresses = [{ 'id' => SecureRandom.uuid }]
        stub_self_returning_api_client(message: :get_ip_addresses,
                                       response: { 'entities' => addresses },
                                       with: [api_id])

        expect(manager.ip_addresses).to eq(addresses)
      end
    end

    context 'failed API request' do
      it 'returns empty array' do
        response = '{"code":400,"message":"Bad request"}'
        stub_self_returning_api_client(message: :get_ip_addresses,
                                       success: false,
                                       response:,
                                       with: [api_id])

        expect(manager.ip_addresses).to eq([])
        expect(manager.errors).to eq({ root: 'Unable to process request' })
      end
    end
  end
end
