# frozen_string_literal: true

module MatchHtmlFragment
  class MatchHtmlFragment
    attr_accessor :failure_message

    def initialize(expected_html)
      @expected_fragment =
        Nokogiri::HTML::DocumentFragment.parse(expected_html.chomp)
      @failure_message = ''
    end

    def matches?(actual_html)
      actual_fragment =
        Nokogiri::HTML::DocumentFragment.parse(actual_html.chomp)

      equal_nodes? @expected_fragment, actual_fragment
    end

    def description
      "match fragment starting with #{@expected_fragment.to_s.lines.first}"
    end

    private

    def equal_nodes?(expected, actual) # rubocop:disable Metrics/CyclomaticComplexity,Metrics/PerceivedComplexity
      return false unless expected.instance_of?(actual.class)
      return false unless equal_names?(expected, actual)
      return false unless no_extra_attributes?(expected, actual)
      return false unless no_missing_attributes?(expected, actual)
      return false unless equal_attributes?(expected, actual)
      return false unless equal_content?(expected, actual)
      return false unless equal_child_count?(expected, actual)
      return false unless equal_children?(expected, actual)

      true
    end

    def equal_classes?(expected, actual)
      return true if expected.instance_of?(actual.class)

      fail "Expected #{actual.class} to be a #{expected.class}.", expected
    end

    def equal_names?(expected, actual)
      return true unless expected.is_a?(Nokogiri::XML::Element)

      return true if expected.name == actual.name

      fail "Expected tag #{actual.name} to be a #{expected.name}.", expected
    end

    def no_extra_attributes?(expected, actual)
      extra_attributes = actual.attributes.keys - expected.attributes.keys
      return true if extra_attributes.empty?

      fail("Unexpected attributes #{extra_attributes}", expected)
    end

    def no_missing_attributes?(expected, actual)
      missing_attributes = expected.attributes.keys - actual.attributes.keys
      return true if missing_attributes.empty?

      fail("Missing attributes #{missing_attributes}", expected)
    end

    def equal_attributes?(expected, actual)
      expected.attributes.each_key do |key|
        if expected.attributes[key].value != actual.attributes[key].value
          return fail(
            "Expected #{key} to be '#{expected.attributes[key].value}. " \
            "Got '#{actual.attributes[key].value}' instead.",
            expected
          )
        end
      end

      true
    end

    def equal_content?(expected, actual)
      return true unless expected.is_a? Nokogiri::XML::Text

      expected_content = expected.content.strip.gsub(/\s+/, ' ')
      actual_content = actual.content.strip.gsub(/\s+/, ' ')

      return true if expected_content.strip == actual_content.strip

      fail(
        "Expected content '#{expected_content}'. " \
        "Got '#{actual_content}' instead.",
        expected
      )
    end

    def equal_child_count?(expected, actual)
      expected_count = expected.children.count(&:present?)
      actual_count = actual.children.count(&:present?)

      return true if expected_count == actual_count

      fail(
        "Expected #{expected_count} children. " \
        "Got #{actual_count} instead.",
        expected
      )
    end

    def equal_children?(expected, actual)
      expected_children = expected.children.reject(&:blank?)
      actual_children = actual.children.reject(&:blank?)

      expected_children.each_with_index do |expected_child, i|
        return false unless equal_nodes?(expected_child, actual_children[i])
      end
    end

    def fail(message, node)
      @failure_message = message
      @failure_message += " (at #{node.css_path})"
      false
    end
  end

  def match_html_fragment(html)
    MatchHtmlFragment.new(html)
  end
end

RSpec.configure do |config|
  config.include MatchHtmlFragment
end
