# frozen_string_literal: true

# Handles merging existing emails and accounts
module CspExistingAccount
  extend ActiveSupport::Concern

  private

  def handle_csp_user_response(csp_user, auth)
    return render_add_email(csp_user, auth) if csp_user && !email_match?(csp_user, auth)

    if csp_user.nil?
      orig_csp_user = existing_account(auth)
      return render_link_account(primary_email(auth), orig_csp_user.csp.name) if orig_csp_user.present?
    end

    check_csp_session
    sync_and_redirect(csp_user, auth)
  end

  def email_match?(csp_user, auth)
    csp_user.user_emails.empty? || csp_user.user_emails.map(&:email).include?(primary_email(auth))
  end

  def name_match?(user, auth)
    info = auth.extra.raw_info
    user.given_name.casecmp?(info.given_name) && user.family_name.casecmp?(info.family_name)
  end

  def existing_account(auth)
    csp_users = matching_csp_users(auth)
    return nil if csp_users.empty?

    csp_users.first
  end

  def matching_csp_users(auth)
    emails = other_csp_emails(auth)
    return [] if emails.empty?

    matching, mismatched = emails.map(&:csp_user).uniq.partition { |c| name_match?(c.user, auth) }
    log_name_mismatch(mismatched) if mismatched.any?
    validate_unique_match(matching, emails)
    matching
  end

  def other_csp_emails(auth)
    UserEmail.includes(csp_user: %i[user csp])
             .where(email: [primary_email(auth), *all_emails(auth)])
             .reject { |e| e.csp_user.csp.name == auth.provider.to_s }
  end

  def log_name_mismatch(csp_users)
    csp_users.each do |csp_user|
      Rails.logger.info(['Email match found but name does not match',
                         { actionContext: LoggingConstants::ActionContext::Authentication,
                           actionType: LoggingConstants::ActionType::NameMismatch,
                           csp: csp_user.csp.name }])
    end
  end

  def validate_unique_match(csp_users, emails)
    return unless csp_users.many?

    matching_emails = emails.map(&:email).uniq.join(',')
    raise CspUtils::MultiUserMatchError, "too many matching users | #{matching_emails}"
  end

  def render_add_email(csp_user, auth)
    Rails.logger.info(['User has existing account associated with different email',
                       { actionContext: LoggingConstants::ActionContext::Authentication,
                         actionType: LoggingConstants::ActionType::MergeUserAccountEmail,
                         **csp_log_context }])
    render(Page::ExistingAccount::AddEmailComponent.new(csp_user.user.email, csp_user.csp.name,
                                                        update_path(id: csp_user.id,
                                                                    csp: csp_user.csp.id,
                                                                    all_emails: all_emails(auth),
                                                                    primary_email: primary_email(auth))))
  end

  def render_link_account(email, csp)
    Rails.logger.info(['User has existing account associated with different CSP',
                       { actionContext: LoggingConstants::ActionContext::Authentication,
                         actionType: LoggingConstants::ActionType::MergeUserAccountCsp,
                         csp: }])
    render(Page::ExistingAccount::LinkAccountComponent.new(email, csp))
  end

  def check_csp_session
    return if current_user.nil? || csp_session.active_csps.one?

    verify_account_match
    create_csp_user
  end

  def all_user_info
    @all_user_info ||= UserInfoService.new.all_user_info(csp_session)
  end

  def verify_account_match
    all_ssns = all_user_info.values.map { |user_info| ssn(user_info) }
    raise CspUtils::SsnMismatchError, 'SSN mismatch' unless all_ssns.uniq.one?
  end

  def ssn(user_info)
    user_info.dig('extra', 'raw_info', 'social_security_number') ||
      user_info.dig('extra', 'raw_info', 'SSN') ||
      user_info.dig('extra', 'raw_info', 'ssn')
  end

  def create_csp_user
    csp_session.active_csps.each do |csp_name|
      csp = Csp.find_by!(name: csp_name)
      uuid = all_user_info[csp_name]['uid']
      CspUser.find_or_create_by(user: current_user, csp:, uuid:)
    end
  end
end
