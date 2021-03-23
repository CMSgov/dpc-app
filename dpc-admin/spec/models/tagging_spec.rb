# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Tagging, type: :model do
  subject { create :tagging }

  describe 'factory' do
    it { is_expected.to be_valid }
  end

  describe 'validations' do
    it 'requires a tag' do
      subject.tag = nil
      expect(subject).not_to be_valid
    end

    it 'requires a taggable' do
      subject.taggable = nil
      expect(subject).not_to be_valid
    end
  end
end
