# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Baz, type: :model do
  let(:baz) { Baz.new }
  it 'should something_baz a' do
    expect(baz.something_baz('a')).to eq 'a'
  end
  it 'should something_baz b' do
    expect(baz.something_baz('b')).to eq 'b'
  end
  it 'should something_baz else d' do
    expect(baz.something_baz_else('d')).to eq 'd'
  end
  it 'should something_baz else c' do
    expect(baz.something_baz_else('c')).to eq 'c'
  end
  it 'should something_baz third e' do
    expect(baz.something_baz_third('e')).to eq 'e'
  end
  it 'should something_baz third f' do
    expect(baz.something_baz_third('f')).to eq 'f'
  end
end
