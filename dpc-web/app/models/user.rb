# frozen_string_literal: true

require 'csv'

class User < ApplicationRecord
  include OrganizationTypable
  has_one :dpc_registration, inverse_of: :user
  has_many :taggings, as: :taggable
  has_many :tags, through: :taggings
  has_many :organization_user_assignments
  has_many :organizations, through: :organization_user_assignments

  before_save :requested_num_providers_to_zero_if_blank

  # Include default devise modules. Others available are:
  # :confirmable, :lockable,
  # :trackable, and :omniauthable, :recoverable,
  devise :database_authenticatable, :rememberable,
         :validatable, :trackable, :registerable,
         :timeoutable, :recoverable

  enum requested_organization_type: ORGANIZATION_TYPES

  validates :requested_organization_type, inclusion: { in: ORGANIZATION_TYPES.keys }
  validates :last_name, :first_name, presence: true
  validates :requested_organization, presence: true
  validates :requested_num_providers, numericality: { only_integer: true, greater_than_or_equal_to: 0, allow_nil: true }
  validates :address_1, presence: true
  validates :city, presence: true
  validates :state, inclusion: { in: Address::STATES.keys.map(&:to_s) }
  validates :zip, format: { with: /\A\d{5}(?:\-\d{4})?\z/ }
  validates :agree_to_terms, inclusion: {
    in: [true], message: 'you must agree to the terms of service to create an account'
  }

  scope :assigned, -> { left_joins(:organization_user_assignments).where('organization_user_assignments.id IS NOT NULL')}
  scope :unassigned, -> { left_joins(:organization_user_assignments).where('organization_user_assignments.id IS NULL') }
  scope :assigned_non_vendor, -> do
    joins(:organizations).where('organizations.organization_type <> :vendor', vendor: ORGANIZATION_TYPES['health_it_vendor'])
  end
  scope :assigned_vendor, -> do
    joins(:organizations).where('organizations.organization_type = :vendor', vendor: ORGANIZATION_TYPES['health_it_vendor'])
  end

  def self.to_csv
    attrs = %w[id first_name last_name email requested_organization requested_organization_type
               address_1 address_2 city state zip agree_to_terms requested_num_providers created_at updated_at]

    CSV.generate(headers: true) do |csv|
      csv << attrs
      all.each do |user|
        csv << user.attributes.values_at(*attrs)
      end
    end
  end

  def name
    "#{first_name} #{last_name}"
  end

  def primary_organization
    organizations.first
  end

  private

  def requested_num_providers_to_zero_if_blank
    self.requested_num_providers = 0 if requested_num_providers.blank?
  end
end
