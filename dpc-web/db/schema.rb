# This file is auto-generated from the current state of the database. Instead
# of editing this file, please use the migrations feature of Active Record to
# incrementally modify your database, and then regenerate this schema definition.
#
# This file is the source Rails uses to define your schema when running `rails
# db:schema:load`. When creating a new database, `rails db:schema:load` tends to
# be faster and is potentially less error prone than running all of your
# migrations from scratch. Old migrations may fail to apply correctly if those
# migrations use external dependencies or application code.
#
# It's strongly recommended that you check this file into your version control system.

ActiveRecord::Schema.define(version: 2020_09_09_153523) do

  # These are extensions that must be enabled in order to support this database
  enable_extension "plpgsql"

  create_table "addresses", force: :cascade do |t|
    t.string "street", null: false
    t.string "street_2"
    t.string "city", null: false
    t.string "state", null: false
    t.string "zip", null: false
    t.string "addressable_type", null: false
    t.bigint "addressable_id", null: false
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.integer "address_use", default: 0, null: false
    t.integer "address_type", default: 0, null: false
    t.index ["addressable_type", "addressable_id"], name: "index_addresses_on_addressable_type_and_addressable_id"
  end

  create_table "fhir_endpoints", force: :cascade do |t|
    t.string "name", null: false
    t.integer "status", null: false
    t.string "uri", null: false
    t.integer "organization_id"
    t.integer "registered_organization_id"
    t.index ["registered_organization_id"], name: "index_fhir_endpoints_on_registered_organization_id"
  end

  create_table "old_passwords", force: :cascade do |t|
    t.string "encrypted_password", null: false
    t.string "password_archivable_type", null: false
    t.integer "password_archivable_id", null: false
    t.string "password_salt"
    t.datetime "created_at"
    t.index ["password_archivable_type", "password_archivable_id"], name: "index_password_archivable"
  end

  create_table "organization_user_assignments", force: :cascade do |t|
    t.integer "organization_id", null: false
    t.integer "user_id", null: false
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["organization_id", "user_id"], name: "index_org_user_assignments_on_organization_id_and_user_id", unique: true
  end

  create_table "organizations", force: :cascade do |t|
    t.string "name", null: false
    t.integer "organization_type", null: false
    t.integer "num_providers", default: 0
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.string "npi"
    t.string "vendor"
  end

  create_table "registered_organizations", force: :cascade do |t|
    t.integer "organization_id", null: false
    t.string "api_id"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.string "api_endpoint_ref"
    t.boolean "enabled", default: true, null: false
    t.index ["organization_id"], name: "index_registered_organizations_on_organization_id"
  end

  create_table "taggings", force: :cascade do |t|
    t.integer "tag_id"
    t.string "taggable_type"
    t.bigint "taggable_id"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["taggable_type", "taggable_id"], name: "index_taggings_on_taggable_type_and_taggable_id"
  end

  create_table "tags", force: :cascade do |t|
    t.string "name"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
  end

  create_table "the_resources", force: :cascade do |t|
    t.datetime "password_changed_at"
    t.index ["password_changed_at"], name: "index_the_resources_on_password_changed_at"
  end

  create_table "users", force: :cascade do |t|
    t.string "first_name", null: false
    t.string "last_name", null: false
    t.string "requested_organization", null: false
    t.integer "requested_organization_type", null: false
    t.string "address_1", null: false
    t.string "address_2", default: ""
    t.string "city", null: false
    t.string "state", null: false
    t.string "zip", null: false
    t.boolean "agree_to_terms", null: false
    t.string "email", default: "", null: false
    t.string "encrypted_password", default: "", null: false
    t.integer "sign_in_count", default: 0, null: false
    t.datetime "current_sign_in_at"
    t.datetime "last_sign_in_at"
    t.inet "current_sign_in_ip"
    t.inet "last_sign_in_ip"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.integer "requested_num_providers", default: 0
    t.string "reset_password_token"
    t.datetime "reset_password_sent_at"
    t.string "confirmation_token"
    t.datetime "confirmed_at"
    t.datetime "confirmation_sent_at"
    t.datetime "password_changed_at"
    t.index ["confirmation_token"], name: "index_users_on_confirmation_token", unique: true
    t.index ["created_at"], name: "index_users_on_created_at"
    t.index ["email"], name: "index_users_on_email", unique: true
    t.index ["last_name", "first_name"], name: "index_users_on_last_name_and_first_name"
    t.index ["requested_organization"], name: "index_users_on_requested_organization"
    t.index ["reset_password_token"], name: "index_users_on_reset_password_token", unique: true
  end

end
