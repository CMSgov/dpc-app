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
RSpec.describe Internal::TagsHelper, type: :helper do
  describe '#confirm_text' do
    it 'uses tagging number with confirm text' do
      tag = create(:tag)
      create_list(:tagging, 2, tag: tag)
      expect(helper.confirm_text(tag)).to eq('Are you sure? 2 records have this tag.')
    end
  end
end
