# frozen_string_literal: true

# Handles public key requests
class PublicKeysController < ApplicationController
  before_action :authenticate_user!
  before_action :check_user_verification
  before_action :load_organization
  before_action :require_can_access
  before_action :tos_accepted

  def new
    render Page::PublicKey::NewKeyComponent.new(@organization)
  end

  # rubocop:disable Metrics/AbcSize
  def create
    manager = PublicKeyManager.new(@organization.dpc_api_organization_id)

    new_public_key = manager.create_public_key(
      public_key: params[:public_key],
      label: params[:label],
      snippet_signature: params[:snippet_signature]
    )

    if new_public_key[:response]
      CheckConfigCompleteJob.perform_later(@organization.id) unless @organization.config_complete
      log_credential_action(:public_key, new_public_key.dig(:message, 'id'), :add)
      flash[:success] = 'Public key created successfully.'
      redirect_to organization_path(@organization, credential_start: true)
    else
      @errors = new_public_key[:errors]
      render_error @errors[:root] || 'Invalid encoding'
    end
  end
  # rubocop:enable Metrics/AbcSize

  def destroy
    manager = PublicKeyManager.new(@organization.dpc_api_organization_id)
    if manager.delete_public_key(params)
      log_credential_action(:public_key, params[:id], :remove)
      flash[:success] = 'Public key deleted successfully.'
      redirect_to organization_path(@organization, credential_start: true)
    else
      flash[:alert] = 'Public key could not be deleted.'
    end
  end

  def download_snippet
    send_file 'public/snippet.txt', type: 'application/zip', status: 202
  end

  private

  def render_error(msg)
    flash[:alert] = msg
    render Page::PublicKey::NewKeyComponent.new(@organization, errors: @errors)
  end

  def missing_params
    params[:public_key].blank?
  end

  def label_length
    params[:label].length > 25
  end
end
