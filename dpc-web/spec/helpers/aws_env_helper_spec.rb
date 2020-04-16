# frozen_string_literal: true

require 'rails_helper'

RSpec.describe AwsEnvHelper, type: :helper do
  describe '#aws_environment' do
    context 'successful AWS_ENV check' do
      it 'returns true if AWS_ENV equals prod-sbx' do
        allow(ENV).to receive(:[]).with('AWS_ENV').and_return('prod-sbx')

        expect(prod_sbx?).to be true
      end
    end

    context 'unsuccessful AWS_ENV check' do
      it 'returns false if AWS_ENV does not equal prod-sbx' do
        allow(ENV).to receive(:[]).with('AWS_ENV').and_return('production')

        expect(prod_sbx?).to be false
      end
    end
  end
end
