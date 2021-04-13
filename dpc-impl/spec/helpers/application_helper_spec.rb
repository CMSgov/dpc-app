# frozen_string_literal: true

require 'rails_helper'

RSpec.describe ApplicationHelper, type: :helper do
  describe '#title' do
    it 'it sets the page title content correctly' do
      expect(view.content_for?(:title)).to eq false
      helper.title('test page title')
      expect(view.content_for(:title)).to eq 'test page title'
    end
  end

  describe '#yield_meta_tag' do
    it 'it gets the meta tag correctly if set' do
      expect(view.content_for?(:meta_test)).to eq false
      view.content_for(:meta_test, 'test meta tag')
      expect(helper.yield_meta_tag('test', 'default text')).to eq 'test meta tag'
    end
  end
end
