# frozen_string_literal: true

class TagsController < ApplicationController
  before_action :authenticate_internal_user!

  def index
    @tags = Tag.all
    render layout: 'table_index'
  end

  def create
    @tag = Tag.new tag_params
    if @tag.save
      flash[:notice] = 'Tag created.'
    else
      flash[:alert] = "Tag could not be created. Errors: #{model_error_string(@tag)}"
    end
    redirect_to tags_path
  end

  def destroy
    @tag = Tag.find(id_param)
    if @tag.destroy
      flash[:notice] = 'Tag deleted.'
    else
      flash[:alert] = "Tag could not be deleted. Errors:#{model_error_string(@tag)}"
    end
    redirect_to tags_path
  end

  private

  def tag_params
    params.require(:tag).permit(:name)
  end
end
