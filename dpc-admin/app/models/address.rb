# frozen_string_literal: true

class Address < ApplicationRecord
  belongs_to :addressable, polymorphic: true

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

  enum :address_use, {
    'work' => 0,
    'home' => 1,
    'temp' => 2,
    'old' => 3
  }

  enum :address_type, {
    'postal' => 0,
    'physical' => 1,
    'both' => 2
  }

  validates_presence_of :street, :city, :state, :zip, :addressable
  validates :state, inclusion: { in: STATES.keys.map(&:to_s) }
  validates :zip, format: { with: /\A\d{5}(?:\-\d{4})?\z/ }
end
