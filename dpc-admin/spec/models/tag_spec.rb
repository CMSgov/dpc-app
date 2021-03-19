# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Tag, type: :model do
  subject { create :tag }

  describe 'factory' do
    it { is_expected.to be_valid }
  end

  describe 'validations' do
    it 'requires a name' do
      subject.name = nil
      expect(subject).not_to be_valid
    end

    it 'requires a unique name' do
      create(:tag, name: 'Same')
      tag = build(:tag, name: 'Same')
      expect(tag).not_to be_valid
    end
  end
end
