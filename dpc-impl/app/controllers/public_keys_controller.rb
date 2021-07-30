# frozen_string_literal: true

class PublicKeysController < ApplicationController
  before_action :authenticate_user!

  def new
    @org_id = org_id
    @public_keys = get_public_keys(imp_id, @org_id)
  end

  def create
    @org_id = org_id
    return render_error('Required values missing.', @org_id) if missing_key

    manager = PublicKeyManager.new(imp_id: imp_id, org_id: @org_id)

    if org_name_present?(params)
      new_public_key = manager.create_system(
        org_name: params[:org_name],
        public_key: params[:public_key],
        signature: params[:signature]
      )
    else
      new_public_key = manager.create_public_key(
        public_key: params[:public_key],
        signature: params[:signature]
      )
    end

    if new_public_key[:response]
      redirect_to provider_orgs_path(org_id: @org_id)
    else
      render_error(new_public_key[:message], @org_id)
    end

  end

  def index
  end

  def destroy
    @org_id = org_id
    @key_id = params[:id]

    manager = PublicKeyManager.new(imp_id: imp_id, org_id: @org_id)

    if manager.delete_public_key(@key_id)
      flash[:notice] = 'Public key sucessfully deleted'
      redirect_to provider_orgs_path(org_id: @org_id)
    else
      render_error('Public key could not be deleted.', @org_id)
    end
  end

  private

  def create_public_key(manager, params)
    new_public_key = manager.create_public_key(
      public_key: params[:public_key],
      signature: params[:signature]
    )
  end

  def get_public_keys(imp_id, org_id)
    api_req = tokens_keys_api_req(imp_id, org_id)

    if api_req.class == Hash
      return api_req[:public_keys]
    else
      return []
    end
  end

  def org_id
    params[:org_id]
  end

  def org_name_present?(params)
    params[:org_name].present?
  end

  def tokens_keys_api_req(imp_id, org_id)
    api_service.get_tokens_keys(imp_id, org_id)
  end
end
