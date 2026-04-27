# frozen_string_literal: true

# Simple class for linking users to CSPs
class CspUser < ApplicationRecord
  belongs_to :user
  belongs_to :csp
end
