# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Core::Navigation::StepIndicatorComponent, type: :component do
  describe 'three steps' do
    let(:component) { described_class.new(steps, index) }
    let(:steps) { ['Step One', 'Step Two', 'Step Three'] }

    before { render_inline(component) }
    context 'index 0' do
      let(:index) { 0 }
      it 'should render step content in order' do
        expected_text = ['Step One', 'Step Two not completed', 'Step Three not completed']
        nodes = page.all('.usa-step-indicator__segment')
        expect(nodes.map { |node| node.text.strip }).to eq expected_text
      end

      it 'should render two not completed' do
        nodes = page.all('.usa-step-indicator__segments .usa-sr-only')
        expect(nodes.length).to eq 2
        nodes.each { |node| expect(node.text).to eq 'not completed' }
      end

      it 'should render step 1 of 3' do
        expected_text = %w[Step 1 of 3 Step One]
        node = page.find('.usa-step-indicator__heading')
        result_text = node.text.split.map(&:strip)
        expect(result_text).to eq expected_text
      end
    end
    context 'index 1' do
      let(:index) { 1 }
      it 'should render step content in order' do
        expected_text = ['Step One completed', 'Step Two', 'Step Three not completed']
        nodes = page.all('.usa-step-indicator__segment')
        expect(nodes.map { |node| node.text.strip }).to eq expected_text
      end

      it 'should render two not completed' do
        nodes = page.all('.usa-step-indicator__segments .usa-sr-only')
        expect(nodes.length).to eq 2
        expect(nodes.first.text).to eq 'completed'
        expect(nodes.last.text).to eq 'not completed'
      end

      it 'should render step 2 of 3' do
        expected_text = %w[Step 2 of 3 Step Two]
        node = page.find('.usa-step-indicator__heading')
        result_text = node.text.split.map(&:strip)
        expect(result_text).to eq expected_text
      end
    end

    context 'index 2' do
      let(:index) { 2 }
      it 'should render step content in order' do
        expected_text = ['Step One completed', 'Step Two completed', 'Step Three']
        nodes = page.all('.usa-step-indicator__segment')
        expect(nodes.map { |node| node.text.strip }).to eq expected_text
      end

      it 'should render two not completed' do
        nodes = page.all('.usa-step-indicator__segments .usa-sr-only')
        expect(nodes.length).to eq 2
        nodes.each { |node| expect(node.text).to eq 'completed' }
      end

      it 'should render step 3 of 3' do
        expected_text = %w[Step 3 of 3 Step Three]
        node = page.find('.usa-step-indicator__heading')
        result_text = node.text.split.map(&:strip)
        expect(result_text).to eq expected_text
      end
    end
    context 'index 3' do
      let(:index) { 3 }
      it 'should render step content in order' do
        expected_text = ['Step One completed', 'Step Two completed', 'Step Three completed']
        nodes = page.all('.usa-step-indicator__segment')
        expect(nodes.map { |node| node.text.strip }).to eq expected_text
      end

      it 'should render two not completed' do
        nodes = page.all('.usa-step-indicator__segments .usa-sr-only')
        expect(nodes.length).to eq 3
        nodes.each { |node| expect(node.text).to eq 'completed' }
      end

      it 'should not render step indicator' do
        page.assert_no_selector('.usa-step-indicator__heading')
      end
    end
  end
end
