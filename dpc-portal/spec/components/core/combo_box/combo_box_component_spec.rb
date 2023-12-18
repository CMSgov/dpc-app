# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Core::ComboBox::ComboBoxComponent, type: :component do
    describe 'html' do
        subject(:html) do
            render_inline(component)
            rendered_content
        end

        context 'when has no options' do
            let(:component) { described_class.new(label: 'label', id: 'id', options:[], on_change: 'on_change') }
            let(:expected_html) do
                <<~HTML
                <label class="usa-label" for="id">label</label>
                <div class="usa-combo-box">
                <select class="usa-select" name="id" id="id" onchange="on_change">
                    <option value>label</option>
                </select>
                </div>
                HTML
            end

            before do
                render_inline(component)
            end
        
            it { is_expected.to match_html_fragment(expected_html) }
        end

        context 'when has one option' do
            let(:component) { described_class.new(label: 'label', id: 'id', options:['option1'], on_change: 'on_change') }
            let(:expected_html) do
                <<~HTML
                <label class="usa-label" for="id">label</label>
                <div class="usa-combo-box">
                <select class="usa-select" name="id" id="id" onchange="on_change">
                    <option value>label</option>
                    <option value="option1">option1</option>
                  </select>
                </div>
                HTML
            end

            before do
                render_inline(component)
            end
        
            it { is_expected.to match_html_fragment(expected_html) }
        end

        context 'when has multiple options' do
            let(:component) { described_class.new(label: 'label', id: 'id', options:['option1', 'option2'], on_change: 'on_change') }
            let(:expected_html) do
                <<~HTML
                <label class="usa-label" for="id">label</label>
                <div class="usa-combo-box">
                <select class="usa-select" name="id" id="id" onchange="on_change">
                    <option value>label</option>
                    <option value="option1">option1</option>
                    <option value="option2">option2</option>
                  </select>
                </div>
                HTML
            end

            before do
                render_inline(component)
            end
        
            it { is_expected.to match_html_fragment(expected_html) }
        end
    end
end
