# This file is auto-generated from the current state of the database. Instead
# of editing this file, please use the migrations feature of Active Record to
# incrementally modify your database, and then regenerate this schema definition.
#
# This file is the source Rails uses to define your schema when running `bin/rails
# db:schema:load`. When creating a new database, `bin/rails db:schema:load` tends to
# be faster and is potentially less error prone than running all of your
# migrations from scratch. Old migrations may fail to apply correctly if those
# migrations use external dependencies or application code.
#
# It's strongly recommended that you check this file into your version control system.

ActiveRecord::Schema[7.2].define(version: 2025_03_18_133706) do
  # These are extensions that must be enabled in order to support this database
  enable_extension "plpgsql"

  create_table "ao_org_links", force: :cascade do |t|
    t.integer "user_id", null: false
    t.integer "provider_organization_id", null: false
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.bigint "invitation_id"
    t.boolean "verification_status", default: true, null: false
    t.integer "verification_reason"
    t.datetime "last_checked_at", default: -> { "CURRENT_TIMESTAMP" }, null: false
    t.index ["invitation_id"], name: "index_ao_org_links_on_invitation_id"
    t.index ["user_id", "provider_organization_id"], name: "index_ao_org_links_on_user_id_and_provider_organization_id", unique: true
  end

  create_table "audits", force: :cascade do |t|
    t.integer "auditable_id"
    t.string "auditable_type"
    t.integer "associated_id"
    t.string "associated_type"
    t.integer "user_id"
    t.string "user_type"
    t.string "username"
    t.string "action"
    t.text "audited_changes"
    t.integer "version", default: 0
    t.string "comment"
    t.string "remote_address"
    t.string "request_uuid"
    t.datetime "created_at"
    t.index ["associated_type", "associated_id"], name: "associated_index"
    t.index ["auditable_type", "auditable_id", "version"], name: "auditable_index"
    t.index ["created_at"], name: "index_audits_on_created_at"
    t.index ["request_uuid"], name: "index_audits_on_request_uuid"
    t.index ["user_id", "user_type"], name: "user_index"
  end

  create_table "cd_org_links", force: :cascade do |t|
    t.integer "user_id", null: false
    t.integer "provider_organization_id", null: false
    t.integer "invitation_id", null: false
    t.datetime "disabled_at"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
  end

  create_table "credential_audit_logs", force: :cascade do |t|
    t.bigint "user_id"
    t.string "dpc_api_credential_id", null: false
    t.integer "credential_type", null: false
    t.integer "action", null: false
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["dpc_api_credential_id"], name: "index_credential_audit_logs_on_dpc_api_credential_id"
    t.index ["user_id"], name: "index_credential_audit_logs_on_user_id"
  end

  create_table "invitations", force: :cascade do |t|
    t.bigint "provider_organization_id", null: false
    t.bigint "invited_by_id"
    t.integer "invitation_type", null: false
    t.string "invited_given_name"
    t.string "invited_family_name"
    t.string "invited_email"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.integer "status"
  end

  create_table "provider_organizations", force: :cascade do |t|
    t.string "dpc_api_organization_id"
    t.string "name"
    t.string "npi", null: false
    t.bigint "terms_of_service_accepted_by_id"
    t.datetime "terms_of_service_accepted_at"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.integer "verification_status"
    t.integer "verification_reason"
    t.datetime "last_checked_at"
    t.boolean "config_complete", default: false
    t.index ["dpc_api_organization_id"], name: "index_provider_organizations_on_dpc_api_organization_id", unique: true
    t.index ["npi"], name: "index_provider_organizations_on_npi", unique: true
  end

  create_table "sessions", force: :cascade do |t|
    t.string "session_id", null: false
    t.text "data"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["session_id"], name: "index_sessions_on_session_id", unique: true
    t.index ["updated_at"], name: "index_sessions_on_updated_at"
  end

  create_table "users", force: :cascade do |t|
    t.string "email", default: "", null: false
    t.string "encrypted_password", default: "", null: false
    t.string "reset_password_token"
    t.datetime "reset_password_sent_at"
    t.datetime "remember_created_at"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.string "given_name"
    t.string "family_name"
    t.string "provider", limit: 50, default: "", null: false
    t.string "uid", limit: 50, default: "", null: false
    t.string "pac_id"
    t.integer "verification_status"
    t.integer "verification_reason"
    t.datetime "last_checked_at"
    t.index ["email"], name: "index_users_on_email", unique: true
    t.index ["reset_password_token"], name: "index_users_on_reset_password_token", unique: true
  end

  add_foreign_key "ao_org_links", "invitations"
  add_foreign_key "ao_org_links", "provider_organizations"
  add_foreign_key "ao_org_links", "users"
  add_foreign_key "cd_org_links", "invitations"
  add_foreign_key "cd_org_links", "provider_organizations"
  add_foreign_key "cd_org_links", "users"
  add_foreign_key "invitations", "provider_organizations"
  add_foreign_key "invitations", "users", column: "invited_by_id"
  add_foreign_key "provider_organizations", "users", column: "terms_of_service_accepted_by_id"
end
