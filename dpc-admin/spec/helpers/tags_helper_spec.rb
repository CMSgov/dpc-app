# frozen_string_literal: true

require 'rails_helper'

# Specs in this file have access to a helper object that includes
# the Internal::TagsHelper. For example:
#
# describe Internal::TagsHelper do
#   describe "string concat" do
#     it "concats two strings with spaces" do
#       expect(helper.concat_strings("this","that")).to eq("this that")
#     end
#   end
# end
RSpec.describe TagsHelper, type: :helper do
  describe '#confirm_text' do
    it 'uses tagging number with confirm text' do
      tag = create(:tag)
      create_list(:tagging, 2, tag:)
      expect(helper.confirm_text(tag)).to eq('Are you sure? 2 records have this tag.')
    end
  end

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
