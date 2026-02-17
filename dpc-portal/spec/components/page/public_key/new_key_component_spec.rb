# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::PublicKey::NewKeyComponent, type: :component do
  include ComponentSupport
  describe 'html' do
    subject(:html) do
      render_inline(component)
      normalize_space(rendered_content)
    end

    before do
      render_inline(component)
    end

    let(:org) { ComponentSupport::MockOrg.new }

    context 'no errors' do
      let(:component) { described_class.new(org, errors: {}) }
      let(:expected_html) do
        <<~HTML
          <div>
            <div class="margin-bottom-5">← <a href="/portal/organizations/#{org.path_id}">Back to organization</a></div>
            <h1>Add public key</h1>
            <section class="box">
              <div>
                <p>Public keys verify that client token requests are coming from an authorized application.</p>
                <form action="/portal/organizations/#{org.path_id}/public_keys" accept-charset="UTF-8" method="post">
                  <div class="margin-bottom-4">
                    <label class="usa-label" for="label">Public key label</label>
                    <p class="text-base-darker">Choose a label that will be easy to identify.</p>
                    <input type="text" name="label" id="label" maxlength="25" class="usa-input" />
                  </div>
                  <div class="margin-bottom-4">
                    <label class="usa-label" for="public_key">Public key</label>
                    <p class="text-base-darker">Enter a new public key. It must include "BEGIN PUBLIC KEY" and "END PUBLIC KEY" tags from your public.pem file.</p>
                    <textarea name="public_key" id="public_key" class="usa-textarea">
                    </textarea>
                  </div>
                  <div class="margin-bottom-4">
                    <label class="usa-label" for="snippet_signature">Signature snippet</label>
                    <p class="text-base-darker">Enter a signature snippet. This snippet must yield "Verified Ok" results to generate the signature.sig file.</p>
                    <textarea name="snippet_signature" id="snippet_signature" class="usa-textarea">
                    </textarea>
                  </div>
                  <input type="submit" name="commit" value="Add key" class="usa-button" data-test="form:submit" data-disable-with="Add key" />
                  <a class="usa-button usa-button--outline" href="/portal/organizations/#{org.path_id}">Cancel</a>
                </form>
                <p class="margin-top-5"><a href="https://dpc.cms.gov/docsV1">View API Documentation</a></p>
              </div>
            </section>
          </div>
        HTML
      end

      it { is_expected.to match_html_fragment(expected_html) }
    end
    context 'with error on' do
      let(:component) { described_class.new(org, errors:) }
      context 'label' do
        let(:errors) { { label: 'Bad Label' } }
        it 'should show error' do
          bad_label = <<~HTML
            <p id="label_error_msg" style="color: #b50909;">Bad Label</p>
            <input type="text" name="label" id="label" maxlength="25" class="usa-input usa-input--error" />
          HTML
          is_expected.to include(normalize_space(bad_label))
        end
      end
      context 'public key' do
        let(:errors) { { public_key: 'Bad Public Key' } }
        it 'should show error' do
          bad_public_key = <<~HTML
            <p style="color: #b50909;">Bad Public Key</p>
            <textarea name="public_key" id="public_key" class="usa-textarea usa-input--error">
          HTML
          is_expected.to include(normalize_space(bad_public_key))
        end
      end
      context 'snippet signature' do
        let(:errors) { { snippet_signature: 'Bad Snippet Signature' } }
        it 'should show error' do
          bad_snippet_signature = <<~HTML
            <p style="color: #b50909;">Bad Snippet Signature</p>
            <textarea name="snippet_signature" id="snippet_signature" class="usa-textarea usa-input--error">
          HTML
          is_expected.to include(normalize_space(bad_snippet_signature))
        end
      end
    end
  end
end
