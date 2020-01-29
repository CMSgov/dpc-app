# This file is auto-generated from the current state of the database. Instead
# of editing this file, please use the migrations feature of Active Record to
# incrementally modify your database, and then regenerate this schema definition.
#
# Note that this schema.rb definition is the authoritative source for your
# database schema. If you need to create the application database on another
# system, you should be using db:schema:load, not running all the migrations
# from scratch. The latter is a flawed and unsustainable approach (the more migrations
# you'll amass, the slower it'll run and the greater likelihood for issues).
#
# It's strongly recommended that you check this file into your version control system.

ActiveRecord::Schema.define(version: 2020_01_27_141604) do

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

  create_table "delayed_jobs", force: :cascade do |t|
    t.integer "priority", default: 0, null: false
    t.integer "attempts", default: 0, null: false
    t.text "handler", null: false
    t.text "last_error"
    t.datetime "run_at"
    t.datetime "locked_at"
    t.datetime "failed_at"
    t.string "locked_by"
    t.string "queue"
    t.datetime "created_at"
    t.datetime "updated_at"
    t.index ["priority", "run_at"], name: "delayed_jobs_priority"
  end

  create_table "fhir_endpoints", force: :cascade do |t|
    t.string "name", null: false
    t.integer "status", null: false
    t.string "uri", null: false
    t.integer "organization_id"
    t.integer "registered_organization_id"
    t.index ["registered_organization_id"], name: "index_fhir_endpoints_on_registered_organization_id"
  end

  create_table "internal_users", force: :cascade do |t|
    t.string "email", default: ""
    t.string "encrypted_password", default: "", null: false
    t.datetime "remember_created_at"
    t.integer "sign_in_count", default: 0, null: false
    t.datetime "current_sign_in_at"
    t.datetime "last_sign_in_at"
    t.inet "current_sign_in_ip"
    t.inet "last_sign_in_ip"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.string "provider"
    t.string "uid"
    t.string "name"
    t.string "github_nickname"
    t.index ["uid", "provider"], name: "index_internal_users_on_uid_and_provider", unique: true
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
    t.integer "api_environments", default: [], array: true
    t.string "npi"
    t.string "vendor"
  end

  create_table "registered_organizations", force: :cascade do |t|
    t.integer "organization_id", null: false
    t.string "api_id"
    t.integer "api_env", null: false
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.string "api_endpoint_ref"
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
    t.datetime "remember_created_at"
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
    t.index ["created_at"], name: "index_users_on_created_at"
    t.index ["email"], name: "index_users_on_email", unique: true
    t.index ["last_name", "first_name"], name: "index_users_on_last_name_and_first_name"
    t.index ["requested_organization"], name: "index_users_on_requested_organization"
    t.index ["reset_password_token"], name: "index_users_on_reset_password_token", unique: true
  end

end
