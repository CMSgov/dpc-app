# frozen_string_literal: true

# Creates and destroys public keys for an organization
class PublicKeysController < ApplicationController
  def new
    @organization_id = params[:organization_id]
  end

  def destroy
    manager = PublicKeyManager.new(api_id: params[:organization_id])
    if manager.delete_public_key(id: params[:id])
      flash[:notice] = 'Public token successfully deleted.'
      redirect_to organization_path(params[:organization_id])
    else
      render_error 'Public token could not be deleted.'
    end
  end

  # rubocop:disable Metrics/AbcSize, Metrics/MethodLength
  def create
    return render_error('Required values missing.') if missing_params
    return render_error('Label cannot be over 25 characters') if label_length

    manager = PublicKeyManager.new(api_id: params[:organization_id])

    new_public_key = manager.create_public_key(
      public_key: params[:public_key],
      label: params[:label],
      snippet_signature: params[:snippet_signature]
    )

    if new_public_key[:response]
      redirect_to portal_path
    else
      render_error new_public_key[:message]
    end
  end
  # rubocop:enable Metrics/AbcSize, Metrics/MethodLength

  def download_snippet
    send_file 'public/snippet.txt', type: 'application/zip', status: 202
  end

  private

  def render_error(msg)
    flash[:alert] = msg
    render :new
  end

  def missing_params
    params[:public_key].blank?
  end

  def label_length
    params[:label].length > 25
  end
end
