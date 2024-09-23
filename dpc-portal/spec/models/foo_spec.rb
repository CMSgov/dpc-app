# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Foo, type: :model do
  let(:foo) { Foo.new }
  it 'should something a' do
    expect(foo.something('a')).to eq 'a'
  end
  it 'should something b' do
    expect(foo.something('b')).to eq 'b'
  end
  it 'should something else d' do
    expect(foo.something_else('d')).to eq 'd'
  end
  it 'should something else c' do
    expect(foo.something_else('c')).to eq 'c'
  end
  it 'should something third e' do
    expect(foo.something_third('e')).to eq 'e'
  end
  it 'should something third f' do
    expect(foo.something_third('f')).to eq 'f'
  end
end
