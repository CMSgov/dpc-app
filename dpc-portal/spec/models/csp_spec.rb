# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Csp, type: :model do
  it 'has many csp_users and users' do
    csp = create(:csp)
    user = create(:user)
    csp_user = create(:csp_user, user:, csp:)
    expect(csp.csp_users).to eq [csp_user]
    expect(csp.users).to eq [user]
  end
end
