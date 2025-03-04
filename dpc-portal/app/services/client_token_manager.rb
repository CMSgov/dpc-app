# frozen_string_literal: true

# Manages client tokens for an organization
class ClientTokenManager
  include CredentialManager

  def create_client_token(label: nil)
    return { response: false, errors: @errors } if invalid_input?(label)

    api_client = DpcClient.new
    api_client.create_client_token(api_id, params: { label: })

    unless api_client.response_successful?
      Rails.logger.error "Failed to create client token: #{api_client.response_body}"
      @errors[:root] = SERVER_ERROR_MSG
    end

    { response: api_client.response_successful?,
      message: api_client.response_body,
      errors: }
  end

  def delete_client_token(params)
    api_client = DpcClient.new
    api_client.delete_client_token(api_id, params[:id])
    api_client.response_successful?
  end

  def client_tokens
    api_client = DpcClient.new
    api_client.get_client_tokens(api_id)
    if api_client.response_successful?
      api_client.response_body['entities']
    else
      []
    end
  end

  private

  def invalid_input?(label)
    if label && label.length > 25
      @errors[:label] = 'Label must be 25 characters or fewer.'
      @errors[:root] = 'Label is too long.'
    elsif label.blank?
      @errors[:label] = "Label can't be blank."
      @errors[:root] = "Fields can't be blank."
    end
    @errors.present?
  end
end
