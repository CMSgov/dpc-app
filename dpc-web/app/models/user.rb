# frozen_string_literal: true

require 'csv'

class User < ApplicationRecord
  include OrganizationTypable
  include Taggable

  has_many :taggings, as: :taggable
  has_many :tags, through: :taggings
  has_many :organization_user_assignments, dependent: :destroy
  has_many :organizations, through: :organization_user_assignments

  before_save :requested_num_providers_to_zero_if_blank

  # Include default devise modules. Others available are:
  # :confirmable, :lockable,
  # :trackable, and :omniauthable, :recoverable,
  devise :database_authenticatable, :async,
         :validatable, :trackable, :registerable,
         :timeoutable, :recoverable, :confirmable,
         :password_expirable, :password_archivable

  enum requested_organization_type: ORGANIZATION_TYPES

  validate :password_complexity
  validates :requested_organization_type, inclusion: { in: ORGANIZATION_TYPES.keys }
  validates :email, presence: true, domain_exists: true
  validates :last_name, :first_name, presence: true
  validates :requested_organization, presence: true
  validates :requested_num_providers, numericality: { only_integer: true, greater_than_or_equal_to: 0,
                                                      allow_nil: true },
                                      if: -> { health_it_vendor? },
                                      on: :create
  validates :requested_num_providers, numericality: { only_integer: true, greater_than: 0 },
                                      unless: -> { health_it_vendor? },
                                      on: :create
  validates :address_1, presence: true
  validates :city, presence: true
  validates :state, inclusion: { in: Address::STATES.keys.map(&:to_s) }
  validates :zip, format: { with: /\A\d{5}(?:\-\d{4})?\z/ }
  validates :agree_to_terms, inclusion: {
    in: [true], message: 'you must agree to the terms of service to create an account'
  }

  scope :assigned, -> do
    left_joins(:organization_user_assignments).where('organization_user_assignments.id IS NOT NULL')
  end

  scope :unassigned, -> do
    left_joins(:organization_user_assignments).where('organization_user_assignments.id IS NULL')
  end

  scope :assigned_provider, -> do
    joins(:organizations)
      .where('organizations.organization_type <> :vendor', vendor: ORGANIZATION_TYPES['health_it_vendor'])
  end

  scope :assigned_vendor, -> do
    joins(:organizations)
      .where('organizations.organization_type = :vendor', vendor: ORGANIZATION_TYPES['health_it_vendor'])
  end

  scope :vendor, -> do
    left_joins(organization_user_assignments: :organization)
      .where('organizations.organization_type = :vendor OR users.requested_organization_type = :vendor',
             vendor: ORGANIZATION_TYPES['health_it_vendor'])
  end

  scope :provider, -> do
    left_joins(organization_user_assignments: :organization)
      .where('organizations.organization_type <> :vendor OR
             (users.requested_organization_type <> :vendor AND organization_user_assignments.id IS NULL)',
             vendor: ORGANIZATION_TYPES['health_it_vendor'])
  end

  scope :by_keyword, ->(keyword) do
    where(
      'LOWER(users.first_name) LIKE :keyword OR LOWER(users.last_name) LIKE :keyword OR
      LOWER(users.email) LIKE :keyword',
      keyword: "%#{keyword.downcase}%"
    )
  end

  ATTRS = %w[id first_name last_name email requested_organization requested_organization_type
             address_1 address_2 city state zip agree_to_terms requested_num_providers created_at
             updated_at tags].freeze

  # html escape these fields for XSS protection
  ESCAPED_ATTRS = %w[first_name last_name requested_organization address_1 address_2 city tags].freeze

  def self.to_csv(user_ids)
    users = User.find(user_ids)
    CSV.generate(headers: true) do |csv|
      csv << ATTRS
      users.each do |user|
        attributes = user.attributes
        attributes['tags'] = user.tags.map(&:name)
        escaped_attributes = attributes.to_h do |k, v|
          if ESCAPED_ATTRS.include? k
            v = ERB::Util.html_escape(v)

            if k == 'tags'
              v.gsub!('&quot;', '')
              v.delete!('[')
              v.delete!(']')
            end
          end

          [k, v]
        end
        csv << escaped_attributes.values_at(*ATTRS)
      end
    end
  end

  def name
    "#{first_name} #{last_name}"
  end

  def primary_organization
    organizations.first
  end

  def unassigned?
    organization_user_assignments.count.zero?
  end

  private

  def password_complexity
    return if password.nil?

    password_regex = /(?=.*\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[!@\#\$\&*\-])/

    return if password.match? password_regex

    errors.add :password, 'must include at least one number, one lowercase letter,
                           one uppercase letter, and one special character (!@#$&*-)'
  end

  def requested_num_providers_to_zero_if_blank
    self.requested_num_providers = 0 if requested_num_providers.blank?
  end
end
