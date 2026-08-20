# frozen_string_literal: true

# Handles routing CSP user responses to the correct flow
module CspExistingAccount
  extend ActiveSupport::Concern
  include CspAccountLookup

  private

  def handle_csp_user_response(csp_user, auth)
    csp_user ? handle_existing_user(csp_user, auth) : handle_unlinked_account(auth)
  end

  def handle_existing_user(csp_user, auth)
    return render_add_email(csp_user, auth) unless email_match?(csp_user, auth)

    check_csp_session
    sync_and_redirect(csp_user, auth)
  end

  def handle_unlinked_account(auth)
    orig_csp_user = existing_account(auth)
    return render_link_account(primary_email(auth), orig_csp_user) if orig_csp_user.present?

    check_csp_session
    csp_user = current_user&.csp_user_for(auth.provider)
    sync_and_redirect(csp_user, auth)
  end

  def email_match?(csp_user, auth)
    csp_user.user_emails.empty? || csp_user.user_emails.map(&:email).include?(primary_email(auth))
  end

  def render_add_email(csp_user, auth)
    log_event(:info, 'User has existing account associated with different email',
              action_context: LoggingConstants::ActionContext::Authentication,
              action_type: LoggingConstants::ActionType::MergeUserAccountEmail,
              user_identifier: auth.uid,
              csp: auth.provider)
    render(Page::ExistingAccount::AddEmailComponent.new(csp_user.user.email, csp_user.csp.name,
                                                        update_path(id: csp_user.id,
                                                                    csp: csp_user.csp.id,
                                                                    all_emails: all_emails(auth),
                                                                    primary_email: primary_email(auth))))
  end

  def render_link_account(email, csp_user)
    original_csp = csp_user.csp.name
    log_event(:info, 'User has existing account associated with different CSP',
              action_context: LoggingConstants::ActionContext::Authentication,
              action_type: LoggingConstants::ActionType::MergeUserAccountCsp,
              user_identifier: csp_user.uuid,
              csp: original_csp)
    render(Page::ExistingAccount::LinkAccountComponent.new(email, original_csp))
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
    all_ssns = all_user_info.values.map { |info| ssn(info) }
    raise CspUtils::SsnMismatchError, 'SSN mismatch' unless all_ssns.uniq.one?
  end

  def ssn(info)
    auth_hash?(info) ? ssn_from_auth_hash(info) : ssn_from_user_info(info)
  end

  def auth_hash?(info)
    info.dig('extra', 'raw_info').present?
  end

  def ssn_from_auth_hash(auth)
    auth.dig('extra', 'raw_info', 'social_security_number') ||
      auth.dig('extra', 'raw_info', 'SSN') ||
      auth.dig('extra', 'raw_info', 'ssn')
  end

  def ssn_from_user_info(user_info)
    user_info['social_security_number'] || user_info['SSN'] || user_info['ssn']
  end

  def create_csp_user
    csp_session.active_csps.each do |csp_name|
      next if csp_name == csp_session.current

      csp = Csp.find_by!(name: csp_name)
      uuid = auth_uuid(csp_name)
      csp_user = CspUser.find_or_create_by(user: current_user, csp:, uuid:)
      next unless csp_user.previously_new_record?

      log_event(:info, 'CSP user created',
                action_context: LoggingConstants::ActionContext::Registration,
                action_type: LoggingConstants::ActionType::CspUserCreated,
                csp: csp_name,
                user_identifier: csp_user.uuid)
    end
  end

  def auth_uuid(csp_name)
    all_user_info[csp_name]['uid'] || all_user_info[csp_name]['sub']
  end
end
