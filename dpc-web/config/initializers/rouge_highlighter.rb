module RougeHighlighter
  class Highlight
    def initialize
      @formatter = Rouge::Formatters::HTML.new(css_class: 'highlight')
      @lexer = Rouge::Lexers::Shell.new
    end

    def render(source)
      @formatter.format(@lexer.lex(source))
    end
  end

  ::HighlightSource = Highlight.new
end
