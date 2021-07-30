# frozen_string_literal: true

class SystemsController < ApplicationController
  before_action :authenticate_user!

  def create
    @org_id = org_id
    return render_error('Required values missing.', @org_id) if missing_key

    manager = PublicKeyManager.new(imp_id: imp_id, org_id: @org_id)

    new_system = manager.create_public_key(
      org_name: params[:org_name],
      public_key: params[:public_key],
      snippet_signature: params[:snippet_signature]
    )
  end
end