# frozen_string_literal: true

# Higher-level wrapper over the Rails session for managing one or more
# concurrent CSP (Credential Service Provider) authentication sessions.
#
# Token data for each CSP is stored under a single nested hash keyed by CSP
# code, while the currently-active CSP code is tracked separately:
#
#   session[:csp_sessions] = {
#     "login_dot_gov" => { "token" => "...", "token_exp" => <Time> },
#     "id_me"         => { "token" => "...", "token_exp" => <Time> }
#   }
#   session[:csp] = "login_dot_gov" # the current CSP
class CspSession
  SESSIONS_KEY = :csp_sessions
  CURRENT_KEY  = :csp
  USER_KEY = :user

  # Wrap the Rails session
  def initialize(session)
    @session = session
  end

  # --- writing -------------------------------------------------------------

  # Store (or replace) a CSP session and mark it current. Returns the CSP code.
  def store(csp:, token:, token_exp:)
    code = csp.to_s
    data = sessions
    data[code] = { 'token' => token, 'token_exp' => token_exp }
    @session[SESSIONS_KEY] = data
    @session[CURRENT_KEY] = code
    code
  end

  # Mark an already-known CSP as current without changing its token.
  def activate(csp)
    @session[CURRENT_KEY] = csp.to_s
  end

  # CSP session could result in a user being logged in, so we store the user in the session as well.
  def store_user(user)
    @session[USER_KEY] = user
  end

  def clear_user
    @session.delete(USER_KEY)
  end

  def user
    @session[USER_KEY]
  end

  # --- reading -------------------------------------------------------------

  def current
    @session[CURRENT_KEY]
  end

  def token(csp = current)
    sessions.dig(csp.to_s, 'token')
  end

  def token_exp(csp = current)
    sessions.dig(csp.to_s, 'token_exp')
  end

  # A CSP is "active" when it has a token that has not yet expired.
  def active?(csp = current)
    inactive_reason(csp).nil?
  end

  # Returns a symbol indicating why the CSP session is not active, or nil if it is active.
  def inactive_reason(csp = current)
    return :no_session if csp.blank?
    return :no_token unless token(csp).present?
    return :no_token_exp unless token_exp(csp).present?
    return :expired_token unless token_exp(csp) > Time.now

    nil
  end

  # Codes of every CSP that currently has a live (non-expired) token.
  def active_csps
    sessions.keys.select { |code| active?(code) }
  end

  def any_active?
    active_csps.any?
  end

  def stored?(csp)
    sessions.key?(csp.to_s)
  end

  # --- clearing / logout ---------------------------------------------------

  # Remove a single CSP session. If it was the current CSP, fall back to
  # another still-active CSP (or nil).
  def clear(csp)
    code = csp.to_s
    data = sessions
    data.delete(code)
    @session[SESSIONS_KEY] = data
    @session[CURRENT_KEY] = active_csps.last if current == code
    nil
  end

  # Remove every CSP session (full logout).
  def clear_all
    @session.delete(SESSIONS_KEY)
    @session.delete(CURRENT_KEY)
    nil
  end

  private

  def sessions
    @session[SESSIONS_KEY] ||= {}
  end
end
