const { exec } = require("child_process");
var root = __dirname+"/../"

var gradle = process.platform === "win32" ? "./gradlew.exe" : "./gradlew"
process.chdir(root)
exec(gradle+" :common:generateTypeScript")
