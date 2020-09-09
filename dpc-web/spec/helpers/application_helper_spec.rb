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

  describe '#banner_title' do
    it 'it sets the banner title content correctly' do
      expect(view.content_for?(:banner_title)).to eq false
      helper.banner_title('test banner title')
      expect(view.content_for(:banner_title)).to eq 'test banner title'
    end
  end

  describe '#current_class?' do
    it 'returns active if current class' do
      expect(helper.current_class?('')). to eq 'ds-c-tabs__item--active'
    end

    it 'returns empty string if not current class' do
      expect(helper.current_class?('not the path')). to eq ''
    end
  end

  describe '#meta_tag' do
    it 'it sets the meta tag correctly' do
      expect(view.content_for?(:meta_test)).to eq false
      helper.meta_tag('test', 'test meta tag')
      expect(view.content_for(:meta_test)).to eq 'test meta tag'
    end
  end

  describe '#yield_meta_tag' do
    it 'it gets the meta tag correctly if set' do
      expect(view.content_for?(:meta_test)).to eq false
      view.content_for(:meta_test, 'test meta tag')
      expect(helper.yield_meta_tag('test', 'default text')).to eq 'test meta tag'
    end

    it 'it defaults the text if not set' do
      expect(view.content_for?(:meta_test)).to eq false
      expect(helper.yield_meta_tag('test', 'default text')).to eq 'default text'
    end
  end

  describe '#tabs_set' do
    it 'it sets the tabs correctly' do
      expect(view.content_for?(:tabs_set)).to eq false
      helper.tabs_set('test tabs')
      expect(view.content_for(:tabs_set)).to eq 'test tabs'
    end
  end

  describe '#formatted_datestr' do
    it 'returns `No date` if string is blank' do
      expect(helper.formatted_datestr(nil)).to eq 'No date'
      expect(helper.formatted_datestr('')).to eq 'No date'
    end

    it 'returns the correctly formatted datetime' do
      dt = '2020-01-31T12:30:01'

      expect(helper.formatted_datestr(dt)).to eq '01/31/2020 at 12:30PM UTC'
    end
  end
end
