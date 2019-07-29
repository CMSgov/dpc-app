# frozen_string_literal: true

class User < ApplicationRecord
  has_one :dpc_registration, inverse_of: :user

  STATES = {
    AK: 'Alaska', AL: 'Alabama', AR: 'Arkansas', AZ: 'Arizona',
    CA: 'California', CO: 'Colorado', CT: 'Connecticut',
    DC: 'District of Columbia', DE: 'Delaware', FL: 'Florida',
    GA: 'Georgia', HI: 'Hawaii', IA: 'Iowa', ID: 'Idaho',
    IL: 'Illinois', IN: 'Indiana', KS: 'Kansas', KY: 'Kentucky',
    LA: 'Louisiana', MA: 'Massachusetts', MD: 'Maryland', ME: 'Maine',
    MI: 'Michigan', MN: 'Minnesota', MO: 'Missouri', MS: 'Mississippi',
    MT: 'Montana', NC: 'North Carolina', ND: 'North Dakota',
    NE: 'Nebraska', NH: 'New Hampshire', NJ: 'New Jersey',
    NM: 'New Mexico', NV: 'Nevada', NY: 'New York', OH: 'Ohio',
    OK: 'Oklahoma', OR: 'Oregon', PA: 'Pennsylvania', RI: 'Rhode Island',
    SC: 'South Carolina', SD: 'South Dakota', TN: 'Tennessee', TX: 'Texas',
    UT: 'Utah', VA: 'Virginia', VT: 'Vermont', WA: 'Washington',
    WI: 'Wisconsin', WV: 'West Virginia', WY: 'Wyoming', AS: 'American Samoa',
    GU: 'Guam', PR: 'Puerto Rico', VI: 'Virgin Islands'
  }.freeze

  # Include default devise modules. Others available are:
  # :confirmable, :lockable,
  # :trackable, and :omniauthable, :recoverable,
  devise :database_authenticatable, :rememberable,
         :validatable, :trackable, :registerable,
         :timeoutable

  enum organization_type: {
    primary_care_clinic: 0, speciality_clinic: 1,
    multispecialty_clinic: 2, inpatient_facility: 3,
    emergency_room: 4, urgent_care: 5,
    academic_facility: 6, health_it_vendor: 7,
    other: 8
  }

  validates :last_name, :first_name, presence: true
  validates :organization, presence: true
  validates :organization_type, inclusion: { in: organization_types.keys }
  validates :address_1, presence: true
  validates :city, presence: true
  validates :state, inclusion: { in: STATES.keys.map(&:to_s) }
  validates :zip, format: { with: /\A\d{5}(?:\-\d{4})?\z/ }
  validates :agree_to_terms, inclusion: {
    in: [true], message: 'you must agree to the terms of service to create an account'
  }

  def name
    "#{first_name} #{last_name}"
  end
end
