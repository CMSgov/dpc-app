# frozen_string_literal: true

# Class for holding user email information from a CSP
class UserEmail < ApplicationRecord
  belongs_to :csp_user
end
