extends Control

const MARTO_LIB_VERSION = "1.0"
var libFilePath

var currentVersion = "latest-version"

var launcher_data_path: String = "user://launcher_data.json"

var versions: Dictionary = {}

func _ready():
    libFilePath = str(OS.get_user_data_dir(), "/libs/martolib_", MARTO_LIB_VERSION, ".jar")
    
    var file = File.new()
    if file.file_exists(launcher_data_path):
        file.open(launcher_data_path, File.READ)
        var fileContents = file.get_as_text()
        var json = JSON.parse(fileContents)
        currentVersion = json.result["current-version"]
        loadVersionsDict(json.result["versions"])
    else:
        $CenterPlay/MarginPlay/Play.hide()
    
    $VersionListRequest.request("https://www.martomate.com/api/hexacraft/versions")

func saveLauncherData():
    var file = File.new()
    file.open("user://launcher_data.json", File.WRITE)
    var versionList = []
    for v in versions.keys():
        if v != "latest-version":
            versionList.append(versions[v].asDict());
    var jsonStr = JSON.print({
        "current-version": currentVersion,
        "versions": versionList
    })
    file.store_string(jsonStr)

func _on_Play_pressed():
    saveLauncherData()
    if ensureVersionIsDownloaded(currentVersion):
        startGame(currentVersion)

func startGame(versionID):
    var version = versions[versionID]
    var dir = str(OS.get_user_data_dir(), "/versions/", version.name)
    
    var executable = '/bin/sh'
    var flag = '-c'
    if OS.get_name() == 'Windows':
        executable = 'CMD.exe'
        flag = '/C'
    
    OS.execute(executable, [flag, str('cd ', dir, ' && java -jar \"', version.file_to_run, "\"")], false)
    get_tree().quit()

func ensureVersionIsDownloaded(versionID):
    var version = versions[versionID]
    var dir = Directory.new()
    dir.open("user://")
    
    var base = "user://versions/" + version.name
    
    if !dir.dir_exists(base):
        var error = dir.make_dir_recursive(base)
        if error != OK:
            print(str("Could not make dir. Error: ", error))
        
        $VersionDownloader.download_file = "user://tmp.zip"
        
        $CenterPlay/MarginPlay/Progress.value = 0
        
        $CenterPlay/MarginPlay/Play.hide()
        $CenterPlay/MarginPlay/Progress.show()
        $DownloadProgress.start()
        
        var requestStatus = $VersionDownloader.request(version.download_url)
        if requestStatus != OK:
            print("Request failed with status: " + requestStatus)
        else:
            print("Requesting " + version.download_url)
        return false
    else:
        return true

func _on_Versions_item_selected(ID):
    var versionSelector = $MarginContainer/HBoxContainer/Versions
    currentVersion = versionSelector.get_item_metadata(ID)

func _on_Settings_pressed():
    pass # Replace with function body.


func _on_VersionListRequest_request_completed(result, response_code, headers, body):
    if result == HTTPRequest.RESULT_SUCCESS:
        loadVersionsDict(JSON.parse(body.get_string_from_utf8()).result)
        $CenterPlay/MarginPlay/Play.show()
        saveLauncherData()
    else:
        print("Could not download latest version list. Result was: ", result, ", response_code", response_code)

func loadVersionsDict(p: Array):
    if typeof(p) == TYPE_ARRAY:
        versions.clear()
        for v in p:
            var version = GameVersion.new(v["id"], v["name"], v["release_date"], v["url"], v["file_to_run"])
            versions[version.id] = version
        
        var sortedVersions = Array(versions.values())
        sortedVersions.sort_custom(GameVersionSorter, "sort")
        
        var versionSelector = $MarginContainer/HBoxContainer/Versions
        versionSelector.clear()
        
        versions["latest-version"] = sortedVersions[0]
        versionSelector.add_item(str("Latest (", sortedVersions[0].name, ")"))
        versionSelector.set_item_metadata(0, "latest-version")
        if (currentVersion == "latest-version"):
            versionSelector.select(0)
        
        for v in sortedVersions:
            versionSelector.add_item(v.name)
            versionSelector.set_item_metadata(versionSelector.get_item_count() - 1, v.id)
            if (currentVersion == v.id):
                versionSelector.select(versionSelector.get_item_count() - 1)

    else:
        print("ERROR: The retrieved game versions were not in an array")

class GameVersionSorter:
    static func sort(a, b):
        return a.name > b.name


func _on_VersionDownloader_request_completed(result, response_code, headers, body):
    if result != HTTPRequest.RESULT_SUCCESS:
        print("Failed to downloadload version. Result: ", result, ", response_code: ", response_code)
    $DownloadProgress.stop()
    updateVersionDownloadProgress()
    
    ensureMartoLibIsDownloaded()

func ensureMartoLibIsDownloaded():
    var libDir = Directory.new()
    libDir.open("user://")
    if libDir.file_exists(libFilePath):
        unzipAndStartGame()
    else:
        print("Downloading MartoLib")
        libDir.make_dir("libs")
        $MartoLibDownloader.download_file = libFilePath
        $MartoLibDownloader.request("https://www.martomate.com/api/libs/martolib")

func _on_DownloadProgress_timeout():
    updateVersionDownloadProgress()

func updateVersionDownloadProgress():
    var maxBytes = $VersionDownloader.get_body_size()
    if maxBytes != -1:
        var downloadedBytes = $VersionDownloader.get_downloaded_bytes()
        $CenterPlay/MarginPlay/Progress.max_value = maxBytes
        $CenterPlay/MarginPlay/Progress.value = downloadedBytes


func _on_MartoLibDownloader_request_completed(result, response_code, headers, body):
    if result != HTTPRequest.RESULT_SUCCESS:
        print("Failed to downloadload MartoLib. Result: ", result, ", response_code: ", response_code)
    unzipAndStartGame()

func unzipAndStartGame():
    unzipDownloadedVersionFile()
    
    $CenterPlay/MarginPlay/Play.show()
    $CenterPlay/MarginPlay/Progress.hide()
    startGame(currentVersion)

func unzipDownloadedVersionFile():
    var filePath = str(OS.get_user_data_dir(), "/tmp.zip")
    var output = []
    OS.execute("java", ["-jar", libFilePath, "unzip", filePath, str(OS.get_user_data_dir(), "/versions/", versions[currentVersion].name)], true, output)
    if !output.empty() && output[0] != "":
        print("Failed to unzip version file. Error: ", output)
    
    Directory.new().remove(filePath)
    