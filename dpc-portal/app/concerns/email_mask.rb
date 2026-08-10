# frozen_string_literal: true

# Shared email masking function
module EmailMask
  def self.masked(email)
    return email if email.blank?

    name, domain = email.split('@')
    return email unless domain

    "#{name[0, 4]}#{'*' * 6}@#{domain}"
  end
end
