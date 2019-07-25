class IgController < ApplicationController
  def index
    render file: 'public/ig/index.html', layout: false
  end
end