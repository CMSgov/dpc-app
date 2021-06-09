# frozen_string_literal: true

class User < ApplicationRecord
  include ApiErrorSimplify

  before_create :create_api_imp, if: -> { no_imp_id? }
  before_create :check_impl

  # Include default devise modules. Others available are:
  # :confirmable, :lockable, :timeoutable, :trackable and :omniauthable
  devise :database_authenticatable, :registerable,
         :recoverable, :validatable, :trackable, 
         :timeoutable, :confirmable, :password_expirable,
         :password_archivable, :invitable, :expirable

  validates :first_name, :last_name, :implementer, presence: true
  validates :email, domain_exists: true
  validate :password_complexity
  validates :agree_to_terms, inclusion: {
    in: [true], message: 'you must agree to the terms of service to create an account'
  }

  def check_impl
    @host = self.invited_by_id

    return if @host.nil?

    @user = self
    @invite = User.where(id: @host).first
    user_id = @user.implementer_id
    invite_id = @invite.implementer_id
    user_imp = @user.implementer
    invite_imp = @invite.implementer

    if user_id != invite_id
      @user.implementer_id = invite_id
    end

    if user_imp != invite_imp
      @user.implementer = invite_imp
    end
  end

  def create_api_imp
    api_request = api_service.create_implementer(implementer)

    api_response = api_request.response_body

    if api_request.response_successful?
      self.implementer_id = api_response[:id]
      api_response
    else
      action = 'registered'
      msg = api_simplify(api_response)
      api_error(action, msg)
      throw(:abort)
    end
  end

  def no_imp_id?
    implementer_id.blank?
  end

  def name
    "#{first_name} #{last_name}"
  end

  def client_orgs
    api_client = ApiClient.new
    api_client.get_client_orgs(self.implementer_id)
    if api_client.response_successful?
      api_client.response_body.sort_by{|e| e[:created_at]}.reverse
    else
      false
    end
  end

  private

  def api_service
    @api_service ||= ApiClient.new
  end

  def api_error(action, msg)
    errors.add(:base, "couldn't be #{action} with DPC's API: #{msg}")
  end

  def password_complexity
    return if password.nil?

    password_regex = /(?=.*\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[!@\#\$\&*\-])/

    return if password.match? password_regex

    errors.add :password, 'must include at least one number, one lowercase letter,
                           one uppercase letter, and one special character (!@#$&*-)'
  end
end
