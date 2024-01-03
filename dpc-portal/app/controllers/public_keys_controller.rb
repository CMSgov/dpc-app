# frozen_string_literal: true

# Handles public key requests
class PublicKeysController < ApplicationController
  before_action :load_organization

  def new
    render Page::PublicKey::NewKeyComponent.new(@organization)
  end

  # rubocop:disable Metrics/AbcSize
  def create
    return render_error('Required values missing.') if missing_params
    return render_error('Label cannot be over 25 characters') if label_length

    manager = PublicKeyManager.new(params[:organization_id])

    new_public_key = manager.create_public_key(
      public_key: params[:public_key],
      label: params[:label],
      snippet_signature: params[:snippet_signature]
    )

    if new_public_key[:response]
      flash[:notice] = 'Public key successfully created.'
      redirect_to organization_path(params[:organization_id])
    else
      render_error 'Public key could not be created.'
    end
  end
  # rubocop:enable Metrics/AbcSize

  def destroy
    manager = PublicKeyManager.new(params[:organization_id])
    if manager.delete_public_key(params)
      flash[:notice] = 'Public key successfully deleted.'
      redirect_to organization_path(params[:organization_id])
    else
      flash[:alert] = 'Public key could not be deleted.'
    end
  end

  def download_snippet
    send_file 'public/snippet.txt', type: 'application/zip', status: 202
  end

  private

  def load_organization
    @organization = case ENV.fetch('ENV', nil)
                    when 'prod-sbx'
                      redirect_to root_url
                    when 'test'
                      Organization.new('6a1dbf47-825b-40f3-b81d-4a7ffbbdc270')
                    when 'dev'
                      Organization.new('78d02106-2837-4d07-8c51-8d73332aff09')
                    else
                      Organization.new(params[:organization_id])
                    end
  end

  def render_error(msg)
    flash[:alert] = msg
    render Page::PublicKey::NewKeyComponent.new(@organization)
  end

  def missing_params
    params[:public_key].blank?
  end

  def label_length
    params[:label].length > 25
  end
end
