extends Control

var currentVersion = "latest-version"

var launcher_data_path: String = "user://launcher_data.json"

var versions: Dictionary = {}

func _ready():
    var file = File.new()
    if file.file_exists(launcher_data_path):
        file.open(launcher_data_path, File.READ)
        var json = JSON.parse(file.get_as_text())
        loadVersionsDict(json["versions"])
    else:
        $CenterPlay/MarginPlay/Play.hide()
    
    $VersionListRequest.request("https://www.martomate.com/api/hexacraft/versions")

func saveLauncherData():
    var file = File.new()
    file.open("user://launcher_data.json", File.WRITE)
    var jsonStr = JSON.print({"versions": versions.values()})
    file.store_string(jsonStr)

func _on_Play_pressed():
    if ensureVersionIsDownloaded(currentVersion):
        startGame(currentVersion)

func startGame(versionID):
    print("success!")

func ensureVersionIsDownloaded(versionID):
    var version = versions[versionID]
    var dir = Directory.new()
    print(dir.open("user://"))
    
    var base = "user://versions/" + version.name
    
    if !dir.dir_exists(base):
        dir.make_dir(base)
        
        $VersionDownloader.download_file = "user://temp/tmp.zip"
        $VersionDownloader.request(version.download_url)
        $CenterPlay/MarginPlay/Play.hide()
        $CenterPlay/MarginPlay/Progress.show()
        $CenterPlay/MarginPlay/Progress.max_value = version.download_size
        $DownloadProgress.start()
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
        loadVersionsDict(JSON.parse(body.get_string_from_utf8()))
        $CenterPlay/MarginPlay/Play.show()
        saveLauncherData()
    else:
        print(str("Could not download latest version list. Result was: ", result))

func loadVersionsDict(p: JSONParseResult):
    if typeof(p.result) == TYPE_ARRAY:
        for v in p.result:
            var id = v["id"]
            versions[id] = GameVersion.new(id, v["name"], v["release_date"], v["url"], v["fileSize"])
        
        var sortedVersions = Array(versions.values())
        sortedVersions.sort_custom(GameVersionSorter, "sort")
        
        var versionSelector = $MarginContainer/HBoxContainer/Versions
        versionSelector.clear()
        
        versions["latest-version"] = sortedVersions[0]
        versionSelector.add_item(str("Latest version (", sortedVersions[0].name, ")"))
        versionSelector.set_item_metadata(0, "latest-version")
        
        for v in sortedVersions:
            versionSelector.add_item(v.name)
            versionSelector.set_item_metadata(versionSelector.get_item_count() - 1, v.id)
        
        versionSelector.select(0)

    else:
        print("unexpected results")

class GameVersionSorter:
    static func sort(a, b):
        return a.release_date > b.release_date


func _on_VersionDownloader_request_completed(result, response_code, headers, body):
    startGame(currentVersion)


func _on_DownloadProgress_timeout():
    $CenterPlay/MarginPlay/Progress.value = $VersionDownloader.get_downloaded_bytes()
