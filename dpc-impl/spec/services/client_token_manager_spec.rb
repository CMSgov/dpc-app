# frozen_string_literal: true

require 'rails_helper'

RSpec.describe ClientTokenManager do
  describe '#create_client_token' do
    before(:each) do
      @user = create(:user)
      @imp_id = @user.implementer_id
      @org_id = "77233b56-555c-4c29-8d29-dc7c615c8c31"
    end

    context 'successful API request' do
      it 'respons true with @client_token set' do
        token = { 'token' => 'exampleToken' }
        api_client = instance_double(ApiClient)
        allow(ApiClient).to receive(:new).and_return(api_client)
        allow(api_client).to receive(:create_client_token)
          .with(@imp_id, @org_id, label: 'Test Token 1')
          .and_return(api_client)
        allow(api_client).to receive(:response_successful?).and_return(true)
        allow(api_client).to receive(:response_body).and_return(token)

        manager = ClientTokenManager.new(imp_id: @imp_id, org_id: @org_id)
        expect(manager.create_client_token(label: 'Test Token 1')).to eq(true)
        expect(manager.client_token).to eq(token)
      end
    end
  end
end