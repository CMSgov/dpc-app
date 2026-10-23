class MakeCspVerificationIndexNonUnique < ActiveRecord::Migration[8.0]
  # Every response from a CSP is kept as its own row, so a verification session can have several
  def change
    name = 'index_csp_user_verifications_on_csp_and_verification_id'
    remove_index :csp_user_verifications, %i[csp_id csp_verification_id], unique: true, name: name
    add_index :csp_user_verifications, %i[csp_id csp_verification_id], name: name
  end
end
