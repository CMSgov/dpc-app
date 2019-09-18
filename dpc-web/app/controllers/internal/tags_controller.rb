# frozen_string_literal: true

module Internal
  class TagsController < ApplicationController
    before_action :authenticate_internal_user!

    def index
      @tags = Tag.all
    end

    def create
      @tag = Tag.new tag_params
      if @tag.save
        flash[:notice] = 'Tag created.'
      else
        flash[:alert] = "Tag could not be created. Errors: #{@tag.errors.full_messages.join(', ')}"
      end
      redirect_to internal_tags_path
    end

    def destroy
      @tag = Tag.find(params[:id])
      if @tag.destroy
        flash[:notice] = 'Tag deleted.'
      else
        flash[:alert] = "Tag could not be deleted. Errors:#{@tag.errors.full_messages.join(', ')}"
      end
      redirect_to internal_tags_path
    end

    private

    def tag_params
      params.fetch(:tag).permit(:name)
    end
  end
end
