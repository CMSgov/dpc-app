# frozen_string_literal: true

require 'rails_helper'

RSpec.describe InputSanitization do
  # Create a minimal anonymous controller to test the concern in isolation
  let(:controller) do
    Class.new do
      include InputSanitization
    end.new
  end

  describe '#sanitize_uid' do
    it 'returns a valid UUID' do
      expect(controller.send(:sanitize_uid, '550e8400-e29b-41d4-a716-446655440000'))
        .to eq('550e8400-e29b-41d4-a716-446655440000')
    end

    it 'returns a valid alphanumeric id' do
      expect(controller.send(:sanitize_uid, 'abc123')).to eq('abc123')
    end

    it 'strips whitespace' do
      expect(controller.send(:sanitize_uid, '  abc123  ')).to eq('abc123')
    end

    it 'returns nil for blank input' do
      expect(controller.send(:sanitize_uid, '')).to be_nil
      expect(controller.send(:sanitize_uid, nil)).to be_nil
    end

    it 'returns nil for path traversal attempts' do
      expect(controller.send(:sanitize_uid, '../../etc/passwd')).to be_nil
    end

    it 'returns nil for input exceeding 64 characters' do
      expect(controller.send(:sanitize_uid, 'a' * 65)).to be_nil
    end

    it 'returns nil for input with special characters' do
      expect(controller.send(:sanitize_uid, '<script>alert(1)</script>')).to be_nil
    end
  end
end
