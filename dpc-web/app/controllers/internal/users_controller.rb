# frozen_string_literal: true

require 'csv'

module Internal
  class UsersController < ApplicationController
    before_action :authenticate_internal_user!

    def index
      results = BaseSearch.new(params: params, scope: params[:org_type]).results
      @tags = Tag.all

      @users = results.order('users.created_at DESC').page params[:page]
      render layout: 'table_index'
    end

    def show
      @user = User.find(id_param)
    end

    def edit
      @user = User.find(id_param)
      @organizations = Organization.all
    end

    def update
      @user = User.find(id_param)
      @organizations = Organization.all
      if @user.update user_params
        flash[:notice] = 'User successfully updated.'
        redirect_to internal_user_url(@user)
      else
        flash[:alert] = "Please correct errors: #{model_error_string(@user)}"
        render :edit
      end
    end

    def destroy
      @user = User.find(id_param)
      @user.destroy

      if @user.destroy
        flash[:notice] = 'User successfully deleted.'
        redirect_to root_path
      else
        flash[:alert] = 'Unable to delete user.'
      end
    end

    def download
      users = params[:users]
      users_obj = JSON.parse(users, object_class: User)
      respond_to do |format|
        filename = "users-#{Time.now.strftime('%Y%m%dT%H%M')}.csv"
        format.csv { send_data csv_convert(users_obj), filename: filename }
      end
    end

    private

    ATTRS = %w[id first_name last_name email requested_organization requested_organization_type
      address_1 address_2 city state zip agree_to_terms requested_num_providers created_at updated_at].freeze
  
    # html escape these fields for XSS protection
    ESCAPED_ATTRS = %w[first_name last_name requested_organization address_1 address_2 city].freeze
  
    def csv_convert(users)
      CSV.generate(headers:true) do |csv|
        csv << ATTRS
        users.each do |user|
          attributes = user.attributes
          escaped_attributes = attributes.map do |k, v|
            if ESCAPED_ATTRS.include? k
              v = ERB::Util.html_escape(v)
            end
  
            [k, v]
          end.to_h
          csv << escaped_attributes.values_at(*ATTRS)
        end
      end
    end

    def user_params
      params.require(:user).permit(:first_name, :last_name, :email, :organization_ids)
    end
  end
end
