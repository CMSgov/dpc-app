# frozen_string_literal: true

# Class for defining CSPs
class Csp < ApplicationRecord
  # The portal should not be able to modify CSPs
  def readonly?
    true
  end

  before_destroy { raise ActiveRecord::ReadOnlyRecord }
end
