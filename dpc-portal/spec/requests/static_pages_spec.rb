# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'StaticPages', type: :request do
  describe 'GET /system-use-agreement' do
    it 'renders the header component' do
      get '/system-use-agreement'
      expect(response.body).to include(
        '<span class="usa-logo--text text-no-underline text-black padding-left-1">Data at the Point of Care</span>'
      )
    end

    it 'renders the text body' do
      get '/system-use-agreement'
      expect(response.body).to include('<h1>Data at the Point of Care – System Use Agreement</h1>')
    end
  end
end
