# frozen_string_literal: true

class TaggingsController < ApplicationController
  before_action :authenticate_internal_user!

  def create
    @tagging = Tagging.new tagging_params
    if @tagging.save
      flash[:notice] = 'Tag added.'
    else
      flash[:alert] = "Tag could not be added. Errors:#{model_error_string(@tagging)}"
    end

    redirect_to taggable_path
  end

  def destroy
    @tagging = Tagging.find(id_param)
    if @tagging.destroy
      flash[:notice] = 'Tag removed.'
    else
      flash[:alert] = "Tag could not be removed. Errors:#{model_error_string(@tagging)}"
    end

    redirect_to taggable_path
  end

  private

  def taggable_path
    if @tagging.taggable_type == 'User'
      user_path(id: @tagging.taggable_id)
    elsif @tagging.taggable_type == 'Organization'
      organization_path(id: @tagging.taggable_id)
    end
  end

  def tagging_params
    params.fetch(:tagging).permit(:tag_id, :taggable_id, :taggable_type)
  end
end
