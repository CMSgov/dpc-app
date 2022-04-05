# frozen_string_literal: true

require 'csv'

class User < ApplicationRecord
  include OrganizationTypable
  include Taggable

  has_many :taggings, as: :taggable
  has_many :tags, through: :taggings
  has_many :organization_user_assignments, dependent: :destroy
  has_many :organizations, through: :organization_user_assignments

  # Include default devise modules. Others available are:
  # :confirmable, :lockable,
  # :trackable, and :omniauthable, :recoverable,
  # devise :database_authenticatable, :async,
  #        :validatable, :trackable, :registerable,
  #        :timeoutable, :recoverable, :confirmable,
  #        :password_expirable, :password_archivable
  enum requested_organization_type: ORGANIZATION_TYPES

  validates :requested_organization_type, inclusion: { in: ORGANIZATION_TYPES.keys }
  validates :email, presence: true, domain_exists: true
  validates :last_name, :first_name, presence: true

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
end
