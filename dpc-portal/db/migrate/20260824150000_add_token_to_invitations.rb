# frozen_string_literal: true

require 'active_support/core_ext/securerandom'

# Add a random token to Invitation URLs, it will be required by every route in the flow.
class AddTokenToInvitations < ActiveRecord::Migration[8.0]
  # Local model so the backfill is not affected by later changes to Invitation.
  class MigrationInvitation < ActiveRecord::Base
    self.table_name = 'invitations'
  end

  def up
    add_column :invitations, :token, :string, limit: 24

    MigrationInvitation.reset_column_information
    MigrationInvitation.where(token: nil).find_each do |invitation|
      token = nil
      token = SecureRandom.base58(24) while token.nil? || MigrationInvitation.exists?(token: token)
      invitation.update_column(:token, token)
    end

    change_column_null :invitations, :token, false
    add_index :invitations, :token, unique: true
  end

  def down
    remove_index :invitations, :token
    remove_column :invitations, :token
  end
end
