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
            <div class="margin-bottom-5">← <a href="/organizations/#{org.path_id}">Back to organization</a></div>
            <h1>Add public key</h1>
            <section class="box">
              <div>
                <p>Public keys verify that client token requests are coming from an authorized application.</p>
                <form action="/organizations/#{org.path_id}/public_keys" accept-charset="UTF-8" method="post">
                  <div class="margin-bottom-4">
                    <label class="usa-label" for="label">Public key label</label>
                    <span id="label_hint" class="text-base-darker">Choose a label that will be easy to identify.</span>
                    <input type="text" name="label" id="label" maxlength="25" class="usa-input" aria-describedby="label_hint" />
                  </div>
                  <div class="margin-bottom-4">
                    <label class="usa-label" for="public_key">Public key</label>
                    <span id="public_key_hint" class="text-base-darker">Enter a new public key. It must include "BEGIN PUBLIC KEY" and "END PUBLIC KEY" tags from your public.pem file.</span>
                    <textarea name="public_key" id="public_key" class="usa-textarea" aria-describedby="public_key_hint">
                    </textarea>
                  </div>
                  <div class="margin-bottom-4">
                    <label class="usa-label" for="snippet_signature">Signature snippet</label>
                    <span id="snippet_signature_hint" class="text-base-darker">Enter a signature snipped. This snippet must yield "Verified Ok" results to generate the signature.sig file.</span>
                    <textarea name="snippet_signature" id="snippet_signature" class="usa-textarea" aria-describedby="snippet_signature_hint">
                    </textarea>
                  </div>
                  <input type="submit" name="commit" value="Add key" class="usa-button" data-test="form:submit" data-disable-with="Add key" />
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
            <span id="label_error_msg" class="usa-error-message" role="alert">Bad Label</span>
            <input type="text" name="label" id="label" maxlength="25" class="usa-input usa-input--error" aria-describedby="label_hint" />
          HTML
          is_expected.to include(normalize_space(bad_label))
        end
      end
      context 'public key' do
        let(:errors) { { public_key: 'Bad Public Key' } }
        it 'should show error' do
          bad_public_key = <<~HTML
            <span id="public_key_error_msg" class="usa-error-message" role="alert">Bad Public Key</span>
            <textarea name="public_key" id="public_key" class="usa-textarea usa-input--error" aria-describedby="public_key_hint">
          HTML
          is_expected.to include(normalize_space(bad_public_key))
        end
      end
      context 'snippet signature' do
        let(:errors) { { snippet_signature: 'Bad Snippet Signature' } }
        it 'should show error' do
          bad_snippet_signature = <<~HTML
            <span id="snippet_signature_error_msg" class="usa-error-message" role="alert">Bad Snippet Signature</span>
            <textarea name="snippet_signature" id="snippet_signature" class="usa-textarea usa-input--error" aria-describedby="snippet_signature_hint">
          HTML
          is_expected.to include(normalize_space(bad_snippet_signature))
        end
      end
    end
  end
end
