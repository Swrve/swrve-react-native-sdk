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
  s.source_files = "ios/**/*.{h,c,m,swift}"
  s.public_header_files = "ios/**/*.h"

  s.platforms    = { :ios => "10.0" }
  s.requires_arc = true

  s.dependency "React-Core"
  s.dependency "SwrveGeoSDK", "5.0.1"
  s.dependency "SwrveSDKCommon", "7.4.0"
end
