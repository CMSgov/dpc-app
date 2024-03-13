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

ActiveRecord::Schema[7.1].define(version: 2024_03_07_213217) do
  # These are extensions that must be enabled in order to support this database
  enable_extension "plpgsql"

  create_table "ao_org_links", force: :cascade do |t|
    t.integer "user_id", null: false
    t.integer "provider_organization_id", null: false
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["user_id", "provider_organization_id"], name: "index_ao_org_links_on_user_id_and_provider_organization_id", unique: true
  end

  create_table "cd_org_links", force: :cascade do |t|
    t.integer "user_id", null: false
    t.integer "provider_organization_id", null: false
    t.integer "invitation_id", null: false
    t.datetime "disabled_at"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
  end

  create_table "invitations", force: :cascade do |t|
    t.bigint "provider_organization_id", null: false
    t.bigint "invited_by_id"
    t.integer "invitation_type", null: false
    t.string "invited_given_name"
    t.string "invited_family_name"
    t.string "invited_phone"
    t.string "invited_email"
    t.string "verification_code"
    t.datetime "cancelled_at"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
  end

  create_table "provider_organizations", force: :cascade do |t|
    t.string "dpc_api_organization_id"
    t.string "name"
    t.string "npi", null: false
    t.bigint "terms_of_service_accepted_by_id"
    t.datetime "terms_of_service_accepted_at"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["dpc_api_organization_id"], name: "index_provider_organizations_on_dpc_api_organization_id", unique: true
    t.index ["npi"], name: "index_provider_organizations_on_npi", unique: true
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
    t.index ["email"], name: "index_users_on_email", unique: true
    t.index ["reset_password_token"], name: "index_users_on_reset_password_token", unique: true
  end

  add_foreign_key "ao_org_links", "provider_organizations"
  add_foreign_key "ao_org_links", "users"
  add_foreign_key "cd_org_links", "invitations"
  add_foreign_key "cd_org_links", "provider_organizations"
  add_foreign_key "cd_org_links", "users"
  add_foreign_key "invitations", "provider_organizations"
  add_foreign_key "invitations", "users", column: "invited_by_id"
  add_foreign_key "provider_organizations", "users", column: "terms_of_service_accepted_by_id"
end
