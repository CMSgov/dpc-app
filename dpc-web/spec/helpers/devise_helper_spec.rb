# frozen_string_literal: true

require 'rails_helper'

RSpec.describe DeviseHelper, type: :helper do
  describe '#devise_error_messages!' do
    it 'returns nothing if notice and alert are blank' do
      expect(helper.devise_error_messages!).to eq(nil)
    end

    it 'returns notice_msg if notice present' do
      flash[:notice] = 'This is a notice'

      expect(helper.devise_error_messages!).to eq(
        <<-HTML
      <div class="ds-c-alert ds-u-margin-bottom--5">
        <div class="ds-c-alert__body">
          <h3 class="ds-c-alert__heading">This is a notice</h3>
        </div>
      </div>
        HTML
      )
    end

    it 'returns email or password incorrect if alert present' do
      flash[:alert] = 'Your email or password is incorrect.'

      expect(helper.devise_error_messages!).to eq(
        <<-HTML
    <div class="ds-c-alert ds-c-alert--error">
      <div class="ds-c-alert__body">
        <h3 class="ds-c-alert__heading">Your email or password is incorrect.</h3>
      </div>
    </div>
        HTML
      )
    end
  end
end