require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = package["name"]
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["repository"]["baseUrl"]
  s.license      = package["license"]
  s.authors      = { package["author"]["name"] => package["author"]["email"]}

  s.source       = { :git => package["repository"]["url"], :tag => "#{s.version}" }
  s.source_files = "ios/**/*.{h,m,swift}"
  s.public_header_files = "ios/**/*.h"

  s.platforms    = { :ios => "10.0" }
  s.requires_arc = true

  s.dependency "React-Core"
# Use optimistic operator to get all minor versions up to (but excluding) the next major version. 
# Do not add a second dot and number for a patch. Eg: 
# Do something like "~> 8.0". 
# DO NOT do something like "~> 8.0.0"
  s.dependency "SwrveSDK", "~> 8.0"

end
