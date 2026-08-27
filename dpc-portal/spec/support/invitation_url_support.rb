# frozen_string_literal: true

module InvitationUrlSupport
  # `org` may be a ProviderOrganization or a bare id.
  # `invitation` is the model object not invitation id
  # `suffix` is the flow step
  # ('accept', 'confirm', ...) and may carry a query string.
def invitation_url_for(org, invitation, suffix = '')
  base = "/organizations/#{org.respond_to?(:id) ? org.id : org}/invitations/#{invitation.id}/#{invitation.token}"
  suffix = suffix.to_s
  suffix.empty? ? base : "#{base}/#{suffix}"
end

  # A token that matches Invitation::TOKEN_FORMAT but belongs to no invitation.
  def unmatched_invitation_token
    SecureRandom.base58(Invitation::TOKEN_LENGTH)
  end
end
