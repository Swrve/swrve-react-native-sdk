require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "SwrvePlugin"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors      = package["author"]

  s.source       = { :git => package["repository"]["url"], :tag => "#{s.version}" }
  s.source_files = "ios/**/*.{h,m,mm,swift,cpp}"
  s.public_header_files = "ios/**/*.h"

  s.platforms    = { :ios => min_ios_version_supported }
  s.requires_arc = true

  install_modules_dependencies(s)

# Use optimistic operator to get all minor versions up to (but excluding) the next major version. 
# Do not add a second dot and number for a patch. Eg: 
# Do something like "~> 9.0". 
# DO NOT do something like "~> 9.0.0"
  s.dependency "SwrveSDK", "~> 10.0"

end
