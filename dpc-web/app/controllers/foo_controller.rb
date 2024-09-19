# frozen_string_literal: true

class FooController < ApplicationController
  layout 'public-key-new'
  before_action :authenticate_user!
  before_action :organization_enabled?, except: :download_snippet
  rescue_from ActiveRecord::RecordNotFound, with: :unauthorized

  def xnew
    @organization = current_user.organizations.find(params[:organization_id])
  end

  def xdestroy
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

  def xcreate
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
      redirect_to authenticated_root_path
    else
      render_error new_public_key[:message]
    end
  end

  def xdownload_snippet
    send_file 'public/snippet.txt', type: 'application/zip', status: 202
  end

  def xorganization_enabled?
    @organization = current_user.organizations.find(params[:organization_id])
    @reg_org = @organization.reg_org

    return if @reg_org.present? && @reg_org.enabled == true

    redirect_to root_path
  end

  private

  def xrender_error(msg)
    flash[:alert] = msg
    render :new
  end

  def xmissing_params
    params[:public_key].blank?
  end

  def xlabel_length
    params[:label].length > 25
  end

  def xunauthorized
    flash[:error] = 'Unauthorized'
    redirect_to authenticated_root_path
  end
end
