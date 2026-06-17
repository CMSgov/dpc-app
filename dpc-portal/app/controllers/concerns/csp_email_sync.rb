# frozen_string_literal: true

# Handles CSP emails
module CspEmailSync
  def sync_csp_emails(csp_user, new_emails, primary_email)
    return unless csp_user

    existing_emails = csp_user.user_emails

    # Scan through all of the emails from the CSP and add or update as necessary.
    ActiveRecord::Base.transaction do
      add_or_activate_new_email(csp_user, new_emails, existing_emails)
      deactivate_old_email(new_emails, existing_emails)
      update_primary_email(csp_user, primary_email)
    end
  end

  private

  def add_or_activate_new_email(csp_user, new_emails, existing_emails)
    new_emails&.each do |new_email|
      existing_email = existing_emails.find { |e| e.email == new_email }
      existing_email ? activate_email(existing_email) : UserEmail.create!(csp_user:, email: new_email, active: true)
    end
  end

  def deactivate_old_email(new_emails, existing_emails)
    # If an existing email is no longer in the list provided by the CSP, deactivate it.
    existing_emails&.each do |existing_email|
      next if new_emails&.include?(existing_email.email)

      existing_email.update!(active: false, deactivated_at: Time.current, reactivated_at: nil)
    end
  end

  def activate_email(user_email)
    return if user_email.active?

    user_email.update!(active: true, deactivated_at: nil, reactivated_at: Time.current)
  end

  def update_primary_email(csp_user, primary_email)
    current_email = csp_user.user_emails.find_by(email: primary_email)
    current_email&.update!(primary: true) unless current_email&.primary?
  end
end
