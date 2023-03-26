# AI Generated Docs
<!-- Plugin description -->
This plugin use the OpenAI API, specifically the gpt-3.5-turbo model, to generate documentation for functions in Kotlin and Java based on their source code.
<!-- Plugin description end -->

# How to run
## Installation
(assuming you already cloned the repo)
### Sandboxed IntelliJ instance
1. Open this repo in IntelliJ
2. Run the Gradle task "Run Plugin". This will open a sandboxes, fresh IntelliJ IDEA instance with the plugin installed

### In your own version of IntelliJ
1. Run `./gradlew shadowJar`
2. Use IntelliJ's "Install Plugin from Disk..." option (Setting > Plugins > Settings Cog > Install Plugin from Disk...) to install `ai-generated-docs-intellij-0.0.1-all.jar` in `./build/libs`

NOTE: It is important you install the JAR whose name has `all` in it, as it includes the 3rd party libraries the plugin needs (to e.g. talk to the OpenAI API).

## Running
1. Set the environment variable `OPENAI_API_KEY` to your OpenAI API Key
2. Place your cursor caret inside the name of the method you want to document 
3. Right click, then click "Show Context Actions", followed by "Generate documentation with OpenAI" (this will only show up if a Javadoc comment for your method is not already present) 
4. Wait, and your documentation will be generated!

# Notes
- Make sure you are happy sending your source code to OpenAI's server before running this plugin
- If the OpenAI API doesn't respond in the correct format, an error message will be shown in a pop-up, and you'll need to try again.
- Super-long functions - e.g. >1000 lines - will result in a plugin exception due to length limits on the OpenAI API

# Interested in more?
[Checkout my CV](https://issuu.com/ksammi/docs/kishan_sambhi_-_cv)