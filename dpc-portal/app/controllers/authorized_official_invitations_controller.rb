# frozen_string_literal: true

# Handles acceptance of Authorized Official invitations
class AuthorizedOfficialInvitationsController < ApplicationController
  before_action :load_organization
  def accept
    render inline: '<h1>Not implemented</h1>', layout: :default, status: :not_implemented
  end
end
