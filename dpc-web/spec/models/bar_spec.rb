# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Bar, type: :model do
  let(:bar) { Bar.new }
  it 'should something_bar a' do
    expect(bar.something_bar('a')).to eq 'a'
  end
  it 'should something_bar b' do
    expect(bar.something_bar('b')).to eq 'b'
  end
  it 'should something_bar else d' do
    expect(bar.something_bar_else('d')).to eq 'd'
  end
  it 'should something_bar else c' do
    expect(bar.something_bar_else('c')).to eq 'c'
  end
end
