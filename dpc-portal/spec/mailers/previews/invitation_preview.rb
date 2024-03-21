# frozen_string_literal: true

# Preview all emails at http://localhost:3000/rails/mailers/invitation
class InvitationPreview < ActionMailer::Preview
  def invite_cd
    invited_by = User.new(given_name: 'Robert', family_name: 'Hodges')
    provider_organization = ProviderOrganization.new(id: 2, name: 'Health Hut')
    invitation = Invitation.new(id: 4,
                                invited_given_name: 'Gavin',
                                invited_family_name: 'McCloud',
                                invited_email: 'gm@example.com',
                                invited_by:, provider_organization:)
    InvitationMailer.with(invitation:).invite_cd
  end
end
