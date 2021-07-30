# frozen_string_literal: true

class PublicKeysController < ApplicationController
  before_action :authenticate_user!

  def new
    @org_id = org_id
  end

  def create
    @org_id = org_id

    manager = PublicKeyManager.new(imp_id: imp_id, org_id: @org_id)

    new_public_key = manager.create_public_key(
      public_key: params[:public_key],
      label: params[:label],
      snippet_signature: params[:snippet_signature]
    )

    if new_public_key[:response]
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
