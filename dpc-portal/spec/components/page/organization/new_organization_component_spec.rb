# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::Organization::NewOrganizationComponent, type: :component do
  include ComponentSupport
  describe 'html' do
    subject(:html) do
      render_inline(component)
      normalize_space(rendered_content)
    end

    let(:component) { described_class.new('') }

    before do
      render_inline(component)
    end

    context 'New form' do
      it 'should match header' do
        header = <<~HTML
          <h1>Add new organization</h1>
          <h2>Look up NPI</h2>
        HTML
        is_expected.to include(normalize_space(header))
      end

      it 'should match form tag' do
        form_tag = ['<form class="usa-form" id="new-organization-form"',
                    %(action="http://test.host/portal/organizations"),
                    'accept-charset="UTF-8" method="post">'].join(' ')
        is_expected.to include(form_tag)
      end

      it 'should have empty npi stanza' do
        npi_field = <<~HTML
          <div class="margin-bottom-4">
            <label class="usa-label" for="npi">What is your Type 2 NPI?</label>
            <input type="text" name="npi" id="npi" maxlength="10" class="usa-input" />
          </div>
        HTML
        is_expected.to include(normalize_space(npi_field))
      end
    end

    context 'Errors' do
      let(:component) { described_class.new("can't be blank") }
      it 'should have errored npi stanza' do
        npi_field = <<~HTML
          <p style="color: #b50909;">can't be blank</p>
        HTML
        is_expected.to include(normalize_space(npi_field))
      end
    end
  end
end
