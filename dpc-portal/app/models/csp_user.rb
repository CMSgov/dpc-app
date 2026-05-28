# frozen_string_literal: true

# Simple class for linking users to CSPs
class CspUser < ApplicationRecord
  belongs_to :user
  belongs_to :csp
  has_many :user_emails

  def add_or_activate_new_email(new_emails)
    existing_emails = user_emails
    new_emails&.uniq&.each do |new_email|
      existing_email = existing_emails.find do |user_email|
        user_email.email == new_email
      end

      if existing_email.nil?
        # Add this email
        UserEmail.create!(csp_user: self, email: new_email, active: true)
      else
        # Potentially activate this email
        activate_email(existing_email)
      end
    end
  end

  def deactivate_old_email(new_emails)
    # Don't deactivate existing emails if new_emails is empty
    return if new_emails.nil? || new_emails.empty?

    # If an existing email is no longer in the list provided by the CSP, deactivate it.
    user_emails&.each do |existing_email|
      unless new_emails&.include?(existing_email.email)
        existing_email.update!(active: false, deactivated_at: Time.current, reactivated_at: nil)
      end
    end
  end

  def activate_email(user_email)
    return unless user_email.active == false

    user_email.update!(active: true, deactivated_at: nil, reactivated_at: Time.current)
  end
end
