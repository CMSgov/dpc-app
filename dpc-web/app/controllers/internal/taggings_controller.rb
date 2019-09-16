# frozen_string_literal: true

module Internal
  class TaggingsController < ApplicationController
    before_action :authenticate_internal_user!

    def create
      @tagging = Tagging.new tagging_params
      if @tagging.save
        flash[:notice] = 'Tag added.'
      else
        flash[:alert] = "Tag could not be added. Errors:#{@tagging.errors.full_messages.join(', ')}"
      end
      redirect_to taggable_path
    end

    private

    # Right now only users are taggable
    def taggable_path
      internal_user_path(id: tagging_params[:taggable_id])
    end

    def tagging_params
      params.fetch(:tagging).permit(:tag_id, :taggable_id, :taggable_type)
    end
  end
end
