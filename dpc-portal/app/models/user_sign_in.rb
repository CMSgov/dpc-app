# frozen_string_literal: true

# A login record for a user
class UserSignIn < ApplicationRecord
  belongs_to :user
  belongs_to :csp
end
