# frozen_string_literal: true

class PublicKeysController < ApplicationController
  layout 'public-key-new'
  before_action :authenticate_user!
  before_action :organization_enabled?, except: :download_snippet
  rescue_from ActiveRecord::RecordNotFound, with: :unauthorized

  def new
    @organization = current_user.organizations.find(params[:organization_id])
  end

  def destroy
    @organization = current_user.organizations.find(params[:organization_id])
    reg_org = @organization.registered_organization

    manager = PublicKeyManager.new(registered_organization: reg_org)
    if manager.delete_public_key(id: params[:id])
      flash[:notice] = 'Public token successfully deleted.'
      redirect_to root_path
    else
      render_error 'Public token could not be deleted.'
    end
  end

  def create
    @organization = current_user.organizations.find(params[:organization_id])
    return render_error('Required values missing.') if missing_params
    return render_error('Label cannot be over 25 characters') if label_length

    reg_org = @organization.registered_organization
    manager = PublicKeyManager.new(registered_organization: reg_org)

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

  def download_snippet
    send_file 'public/snippet.txt', type: 'application/zip', status: 202
  end

  def organization_enabled?
    @organization = current_user.organizations.find(params[:organization_id])
    @reg_org = @organization.reg_org

    return false if @reg_org.present? && @reg_org.enabled == true

    redirect_to root_path
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

  def unauthorized
    flash[:error] = 'Unauthorized'
    redirect_to portal_path
  end
end
