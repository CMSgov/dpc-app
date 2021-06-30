# frozen_string_literal: true

class User < ApplicationRecord

  def name
    "#{first_name} #{last_name}"
  end
end
