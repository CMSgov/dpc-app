class AddInvitationRefToAoOrgLink < ActiveRecord::Migration[7.1]
  def change
    add_reference :ao_org_links, :invitation, null: true, foreign_key: true
  end
end
