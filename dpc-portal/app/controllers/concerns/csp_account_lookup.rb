# frozen_string_literal: true

# Handles lookup and matching of existing CSP accounts
module CspAccountLookup
  extend ActiveSupport::Concern

  private

  def find_csp_user(csp)
    auth = auth_details
    csp_user = CspUser.find_by(uuid: auth&.uid, csp: csp)

    if csp_user.nil?
      log_event(:error, 'No CspUser found for CSP authentication',
                action_context: LoggingConstants::ActionContext::Authentication,
                action_type: LoggingConstants::ActionType::CspUserNotFound,
                user_identifier: auth&.uid,
                csp: auth&.provider || csp.name)
    end

    csp_user
  end

  def existing_account(auth)
    csp_users = matching_csp_users(auth)
    csp_users.first
  end

  def matching_csp_users(auth)
    emails = other_csp_emails(auth)
    return [] if emails.empty?

    csp_users = filter_active_csp_users(emails)
    matching, mismatched = csp_users.partition { |csp_user| name_match?(csp_user.user, auth) }

    log_name_mismatch(mismatched) if mismatched.any?
    validate_unique_match(matching, emails)
    matching
  end

  def filter_active_csp_users(emails)
    emails.map(&:csp_user).uniq.reject { |c| csp_session.active_csps.include?(c.csp.name) }
  end

  def other_csp_emails(auth)
    UserEmail.includes(csp_user: %i[user csp])
             .where(email: [primary_email(auth), *all_emails(auth)])
             .reject { |e| e.csp_user.csp.name == auth.provider.to_s }
  end

  def name_match?(user, auth)
    info = auth.extra.raw_info
    user.given_name.casecmp?(info.given_name) &&
      user.family_name.casecmp?(info.family_name)
  end

  def validate_unique_match(csp_users, emails)
    return unless csp_users.many?

    matching_emails = emails.map(&:email).uniq.join(',')
    raise CspUtils::MultiUserMatchError, "too many matching users | #{matching_emails}"
  end

  def log_name_mismatch(csp_users)
    csp_users.each do |csp_user|
      Rails.logger.info(['Email match found but name does not match',
                         { actionContext: LoggingConstants::ActionContext::Authentication,
                           actionType: LoggingConstants::ActionType::NameMismatch,
                           csp: csp_user.csp.name }])
    end
  end
end
