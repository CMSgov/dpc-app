# frozen_string_literal: true

require 'rails_helper'

# Specs in this file have access to a helper object that includes
# the PagesHelper. For example:
#
# describe PagesHelper do
#   describe "string concat" do
#     it "concats two strings with spaces" do
#       expect(helper.concat_strings("this","that")).to eq("this that")
#     end
#   end
# end

RSpec.describe Internal::UsersHelper, type: :helper do
  describe '#available_tags' do
    it 'only shows tags that the user does not have' do
      user = create(:user)
      tag = create(:tag)
      user.tags << tag

      available_tag = create(:tag)

      expect(helper.available_tags(user)).to match_array([available_tag])
    end
  end
end
