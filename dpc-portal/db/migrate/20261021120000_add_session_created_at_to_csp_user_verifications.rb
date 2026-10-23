class AddSessionCreatedAtToCspUserVerifications < ActiveRecord::Migration[8.0]
  def change
    # When the CSP created the verification session, as it reports it. created_at stays Rails' timestamp for when we
    # recorded the session.
    add_column :csp_user_verifications, :session_created_at, :datetime
  end
end
