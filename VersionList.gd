extends VBoxContainer

export (PackedScene) var ListItemType

var _selected_version: GameVersion = null

func _ready():
    remove_child($VersionListItem)
    $RequestVersionList.request("https://www.martomate.com/api/hexacraft/versions")

func _on_RequestVersionList_request_completed(result, response_code, headers, body):
    var p = JSON.parse(body.get_string_from_utf8())
    if typeof(p.result) == TYPE_ARRAY:
        for v in p.result:
            var item = ListItemType.instance()
            print(v)
            var gameVersion = GameVersion.new(v["id"], v["name"], v["release_date"], v["url"])
            item.fillWithData(gameVersion)
            add_child(item)
    else:
        print("unexpected results")
        
func selected_version():
    return _selected_version