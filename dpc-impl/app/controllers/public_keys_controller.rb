# frozen_string_literal: true

class PublicKeysController < ApplicationController
  before_action :authenticate_user!

  def new
    @org_id = org_id
  end

  def create
    @org_id = org_id
    return render_error('Required values missing.', org_id: @org_id) if missing_key

    manager = PublicKeyManager.new(imp_id: imp_id, org_id: @org_id)

    if org_name_present?(params)
      new_public_key = manager.create_system(
        org_name: params[:org_name],
        public_key: params[:public_key],
        snippet_signature: params[:snippet_signature]
      )
      binding.pry
    else
      create_public_key(manager, params)
    end

    if new_public_key[:response]
    else
      render_error(new_public_key[:message], @org_id)
    end
  end

  def index
  end

  private

  def create_public_key(manager, params)
    new_public_key = manager.create_public_key(
      public_key: params[:public_key],
      label: params[:label],
      snippet_signature: params[:snippet_signature]
    )
  end

  def org_id
    params[:org_id]
  end

  def org_name_present?(params)
    params[:org_name].present?
  end
end
