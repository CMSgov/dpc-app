# frozen_string_literal: true

# A service that checks with a CSP whether a person's identity verification passed, using the access token received
# at login
#
# After a CSP login we don't want to infer "did this person actually pass identity verification?" from the shape of
# the token/redirect. For CSPs that expose a verification-session API, #verify asks the CSP and turns its answer into
# a plain Hash (see below) with an outcome of :verified, :cancelled, :failed or :unavailable. #verify doesn't raise: if
# the CSP can't be asked, the outcome is :unavailable and failure_reason says why. The rest of the app
# (CspController, CspErrorHandling) handles the outcomes generically.
#
# A CSP only needs an adapter: a module that describes how to talk to it (see CspVerification::Clear). It responds to:
#   session_url(access_token)     where to GET the verification session (the access token is sent as the Bearer)
#   status_from(body)             the CSP's status, as a string
#   reason_from(body)             the CSP's failure reason, if it gave one
#   outcome_for(status, reason)   :verified, :cancelled, :failed or :unavailable
#   details_from(body)            what gets stored: { session_id:, created_at:, completed_at:, checks: }
# then add it to #adapter_for. CSPs without an adapter keep the existing behavior (a missing id_token is treated as a
# cancellation).
class CspVerification
  # What #verify returns: a plain Hash with these keys. Together they are what CspUserVerification stores.
  #   outcome:        :verified, :cancelled, :failed or :unavailable. Anything but :verified is passed to csp#failure
  #                   as params[:message]
  #   status:         the CSP's own status string
  #   reason:         the CSP's own failure reason, if it gave one (for outcome_for and for logging)
  #   session_id:     the CSP's id for the verification session
  #   created_at:     Time the CSP created the session. Stored as the row's created_at, so it is when the session
  #                   began rather than when we recorded it
  #   completed_at:   Time the session completed, if it has
  #   checks:         array of { name:, status:, status_details:, value:, reason_codes: }
  # When the CSP couldn't be asked, there is only outcome: :unavailable and failure_reason (one of FAILURE_REASONS).

  FAILURE_REASONS = %w[missing_access_token invalid_access_token invalid_verification_id unauthorized forbidden
                       not_found rate_limited server_error request_failed invalid_response].freeze

  # Raised inside the service with a reason from FAILURE_REASONS; #verify turns it into an :unavailable result
  class Error < StandardError; end

  # Asks the CSP what happened to the verification, using the access token received at login
  def verify(csp, access_token)
    adapter = adapter_for(csp)
    raise ArgumentError, "No verification adapter for #{csp.inspect}" unless adapter

    build_verification(adapter, get_session(adapter.session_url(access_token), access_token))
  rescue Error => e
    handle_error(e, csp)
  end

  # Whether the CSP has a verification adapter, i.e. whether #verify can be used for it
  def supported?(csp)
    !adapter_for(csp).nil?
  end

  private

  def adapter_for(csp)
    { 'clear' => Clear }[csp.to_s]
  end

  def build_verification(adapter, body)
    status = adapter.status_from(body).to_s
    reason = adapter.reason_from(body)
    { outcome: adapter.outcome_for(status, reason), status:, reason: }.merge(adapter.details_from(body))
  end

  def handle_error(error, csp)
    Rails.logger.error "CSP verification unavailable (csp=#{csp}): #{error.message}"
    { outcome: :unavailable, failure_reason: error.message }
  end

  def get_session(session_uri, access_token)
    headers = { 'Authorization' => "Bearer #{access_token}", 'Accept' => 'application/json' }
    handle_response(Net::HTTP.get_response(URI(session_uri), headers))
  rescue Timeout::Error, SocketError, SystemCallError, IOError, OpenSSL::SSL::SSLError => e
    Rails.logger.error "Could not reach CSP verification endpoint: #{e.class}"
    raise Error, 'request_failed'
  end

  def handle_response(response)
    case response
    when Net::HTTPSuccess then JSON.parse(response.body)
    when Net::HTTPUnauthorized then raise Error, 'unauthorized'
    when Net::HTTPForbidden then raise Error, 'forbidden'
    when Net::HTTPNotFound then raise Error, 'not_found'
    when Net::HTTPTooManyRequests then raise Error, 'rate_limited'
    else
      Rails.logger.error "CSP verification error: #{response.code}"
      raise Error, 'server_error'
    end
  rescue JSON::ParserError
    raise Error, 'invalid_response'
  end

  # Adapter for CLEAR's Get Verification Session (OAuth variant): GET /v1/verification_sessions/{verification_id} with
  # the access token as Bearer. The verification_id is a claim in the access token.
  #
  # The claim is read *without* verifying the token's signature. That is safe here because the id is only used to
  # build the request path (and is format-checked first), and CLEAR authenticates the Bearer token itself.
  # Everything we act on comes from CLEAR's response, never from the unverified claims.
  module Clear
    VERIFICATION_ID_FORMAT = /\Averify_[A-Za-z0-9]+\z/

    # https://docs.clearme.com/docs/status-descriptions
    STATUS_OUTCOMES = {
      'success' => :verified,
      'manual_success' => :verified,
      'canceled' => :cancelled,
      'fail' => :failed,
      'expired' => :failed, # the session ran out before the person finished verifying
      'manual_fail' => :failed,
      'deferred' => :failed
      # awaiting_manual_review, awaiting_user_input, processing_data (and anything new) => :unavailable
    }.freeze

    def self.session_url(access_token)
      "#{CLEAR_CONFIG[:verification_session_endpoint].to_s.chomp('/')}/#{verification_id(access_token)}"
    end

    def self.status_from(body) = body['status']

    # status_details is documented on CLEAR's status page; status_reason.code appears in the API response schema.
    def self.reason_from(body) = body['status_details'] || body.dig('status_reason', 'code')

    # "canceled" with a reason (ineligible, ip_blocked, authentication_error...) was not the user choosing to exit.
    def self.outcome_for(status, reason)
      outcome = STATUS_OUTCOMES.fetch(status, :unavailable)
      outcome == :cancelled && reason.present? ? :failed : outcome
    end

    # What we store about the session. Timestamps are epoch seconds. Only the checks' names and results are kept, never
    # their additional_details (e.g. watchlist hits) or any of the user's traits.
    def self.details_from(body)
      { session_id: body['id'],
        created_at: time_from(body['created_at']),
        completed_at: time_from(body['completed_at']),
        checks: Array(body['checks']).map { |check| check_from(check) } }
    end

    def self.verification_id(access_token)
      raise Error, 'missing_access_token' if access_token.blank?

      id = JWT.decode(access_token, nil, false).first['verification_id']
      raise Error, 'invalid_verification_id' unless VERIFICATION_ID_FORMAT.match?(id.to_s)

      id
    rescue JWT::DecodeError
      raise Error, 'invalid_access_token'
    end

    def self.check_from(check)
      { name: check['name'], status: check['status'], status_details: check['check_result'],
        value: check['value'], reason_codes: check['reason_codes'] }
    end

    # CLEAR sends 0 (or nothing) for a time that hasn't happened yet
    def self.time_from(epoch)
      Time.at(epoch).utc if epoch.to_i.positive?
    end

    private_class_method :verification_id, :check_from, :time_from
  end
end
