# Enable ViewComponent::Preview files to be loaded as preview.rb when
# stored in nested directories within app/components.
module ViewComponentSidecarDirectories
        PREVIEW_GLOB = "**/{preview.rb,*_preview.rb}"
  
        def self.extended(base)
          base.singleton_class.prepend(ClassMethods)
        end
  
        module ClassMethods
          def load_previews
            Array(preview_paths).each do |preview_path|
              Dir["#{preview_path}/#{PREVIEW_GLOB}"].sort.each { |file| require_dependency file }
            end
          end
  
          def preview_name
            name.sub(/(::Preview|Preview)$/, "").underscore
          end
        end
  end

ActiveSupport.on_load(:view_component) do
    ViewComponent::Preview.extend ViewComponentSidecarDirectories
  end