# frozen_string_literal: true

class User < ApplicationRecord
  include OrganizationTypable
  has_one :dpc_registration, inverse_of: :user
  has_many :taggings, as: :taggable
  has_many :tags, through: :taggings

  before_save :num_providers_to_zero_if_blank

  # Include default devise modules. Others available are:
  # :confirmable, :lockable,
  # :trackable, and :omniauthable, :recoverable,
  devise :database_authenticatable, :rememberable,
         :validatable, :trackable, :registerable,
         :timeoutable, :recoverable

  validates :last_name, :first_name, presence: true
  validates :organization, presence: true
  validates :num_providers, numericality: { only_integer: true, greater_than_or_equal_to: 0, allow_nil: true }
  validates :address_1, presence: true
  validates :city, presence: true
  validates :state, inclusion: { in: Address::STATES.keys.map(&:to_s) }
  validates :zip, format: { with: /\A\d{5}(?:\-\d{4})?\z/ }
  validates :agree_to_terms, inclusion: {
    in: [true], message: 'you must agree to the terms of service to create an account'
  }

  def name
    "#{first_name} #{last_name}"
  end

  private

  def num_providers_to_zero_if_blank
    self.num_providers = 0 if num_providers.blank?
  end
end
