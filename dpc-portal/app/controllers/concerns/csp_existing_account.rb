# frozen_string_literal: true

module CspExistingAccount
  extend ActiveSupport::Concern

  private

  def handle_csp_user_response(csp_user, auth)
    return render_add_email(csp_user, auth) if csp_user && !email_match?(csp_user, auth)

    orig_csp_user = existing_account(auth)
    return render_link_account(primary_email(auth), orig_csp_user.csp) if orig_csp_user.present?

    # check for other active CSPs, fetch userinfo for each and verify all SSNs match, create CspUser for current_user
    check_csp_session(orig_csp_user, auth.uid)

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
    email = UserEmail.includes(csp_user: :user).find_by(email: primary_email(auth))
    return nil unless email

    csp_user = email.csp_user
    return nil unless csp_user.csp == auth.provider.to_sym

    csp_user if name_match?(csp_user.user, auth)
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
                         **csp_log_context }])
    render(Page::ExistingAccount::LinkAccountComponent.new(email, csp))
  end

  def check_csp_session(user, uid)
    return if csp_session.active_csps.one?

    verify_account_match
    create_csp_user(user, uid)
  end

  def verify_account_match
    all_user_info = UserInfoService.new.all_user_info(csp_session)
    all_ssns = all_user_info.values.map { |user_info| ssn(user_info) }
    raise 'SSN mismatch' unless all_ssns.uniq.one?
  end

  def ssn(user_info)
    user_info['social_security_number'] || user_info['ssn']
  end

  def create_csp_user(user, uuid)
    CspUser.find_or_create_by!(user:, csp: csp_session.current, uuid:)
  end
end
