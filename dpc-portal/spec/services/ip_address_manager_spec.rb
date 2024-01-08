# frozen_string_literal: true

require 'rails_helper'

RSpec.describe IpAddressManager do
  include DpcClientSupport

  describe '#create_ip_address' do
    before(:all) do
      @ip_address_params = { label: 'Public IP 1', ip_address: '136.226.19.87' }
    end

    context 'with valid params' do
      context 'successful API request' do
        it 'responds true' do
          api_id = SecureRandom.uuid
          response = { 'id' => '570f7a71-0e8f-48a1-83b0-c46ac35d6ef3' }
          stub_self_returning_api_client(message: :create_ip_address,
                                         response: response,
                                         with: [api_id, { params: @ip_address_params }])

          manager = IpAddressManager.new(api_id)

          new_ip_address = manager.create_ip_address(**@ip_address_params)

          expect(new_ip_address[:response]).to eq(true)
          expect(new_ip_address[:message]).to eq(response)
        end
      end

      context 'failed API request' do
        it 'responds false' do
          api_id = SecureRandom.uuid
          response = { 'id' => nil }
          stub_self_returning_api_client(message: :create_ip_address,
                                         success: false,
                                         response: response,
                                         with: [api_id, { params: @ip_address_params }])

          manager = IpAddressManager.new(api_id)

          new_ip_address = manager.create_ip_address(**@ip_address_params)

          expect(new_ip_address[:response]).to eq(false)
          expect(new_ip_address[:message]).to eq(response)
        end
      end
    end

    context 'with invalid params' do
      context 'label over 25 characters' do
        it 'response with error' do
          api_id = SecureRandom.uuid
          @ip_address_params[:label] = 'aaaaabbbbbcccccdddddeeeeefffff'
          stub_self_returning_api_client(message: :create_ip_address,
                                         response: { 'id' => '570f7a71-0e8f-48a1-83b0-c46ac35d6ef3' },
                                         with: [api_id, { params: @ip_address_params }])

          manager = IpAddressManager.new(api_id)

          new_ip_address = manager.create_ip_address(**@ip_address_params)

          expect(new_ip_address[:response]).to eq(false)
          expect(new_ip_address[:message]).to eq('Label cannot be over 25 characters.')
        end
      end

      context 'invalid IP' do
        it 'response with error' do
          api_id = SecureRandom.uuid
          @ip_address_params[:ip_address] = '333.333.333.333'
          stub_self_returning_api_client(message: :create_ip_address,
                                         response: { 'id' => '570f7a71-0e8f-48a1-83b0-c46ac35d6ef3' },
                                         with: [api_id, { params: @ip_address_params }])

          manager = IpAddressManager.new(api_id)

          new_ip_address = manager.create_ip_address(**@ip_address_params)

          expect(new_ip_address[:response]).to eq(false)
          expect(new_ip_address[:message]).to eq('Invalid IP address.')
        end
      end
    end
  end

  describe '#delete_ip_address' do
    context 'successful API request' do
      it 'responds true' do
        api_id = SecureRandom.uuid
        addr_guid = SecureRandom.uuid
        stub_self_returning_api_client(message: :delete_ip_address,
                                       with: [api_id, addr_guid])

        manager = IpAddressManager.new(api_id)
        response = manager.delete_ip_address(id: addr_guid)

        expect(response).to be true
      end
    end

    context 'failed API request' do
      it 'responds false' do
        api_id = SecureRandom.uuid
        addr_guid = SecureRandom.uuid
        stub_self_returning_api_client(message: :delete_ip_address,
                                       success: false,
                                       with: [api_id, addr_guid])

        manager = IpAddressManager.new(api_id)
        response = manager.delete_ip_address(id: addr_guid)

        expect(response).to be false
      end
    end
  end

  describe '#ip_addresses' do
    context 'successful API request' do
      it 'returns array of IP addresses' do
        api_id = SecureRandom.uuid

        addresses = [{ 'id' => SecureRandom.uuid }]
        stub_self_returning_api_client(message: :get_ip_addresses,
                                       response: { 'entities' => addresses },
                                       with: [api_id])

        manager = IpAddressManager.new(api_id)
        expect(manager.ip_addresses).to eq(addresses)
      end
    end

    context 'failed API request' do
      it 'returns empty array' do
        api_id = SecureRandom.uuid

        stub_self_returning_api_client(message: :get_ip_addresses,
                                       success: false,
                                       response: { error: 'Bad request' },
                                       with: [api_id])

        manager = IpAddressManager.new(api_id)
        expect(manager.ip_addresses).to eq([])
        expect(manager.errors).to eq([{ error: 'Bad request' }])
      end
    end
  end
end
