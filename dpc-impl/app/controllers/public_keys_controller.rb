# frozen_string_literal: true

class PublicKeysController < ApplicationController
  before_action :authenticate_user!

  def new
    @org_id = org_id
  end

  def create
    @org_id = org_id
    return render_error('Required values missing.', @org_id) if missing_key

    manager = PublicKeyManager.new(imp_id: imp_id, org_id: @org_id)

    new_public_key = manager.create_public_key(
      org_name: params[:org_name],
      label: params[:label],
      public_key: params[:public_key],
      snippet_signature: params[:snippet_signature]
    )

    if new_public_key[:response]
      flash[:notice] = 'Public Key Added'
      redirect_to provider_orgs_path(org_id: @org_id)
    else
      render_error(new_public_key[:message], @org_id)
    end
  end

  def index
  end

  private

  def org_id
    params[:org_id]
  end
end
