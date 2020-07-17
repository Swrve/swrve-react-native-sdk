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

  s.platforms    = { :ios => "9.0" }
  s.requires_arc = true

  s.dependency "React"
  s.dependency "SwrveSDK", "6.5.3"
end

