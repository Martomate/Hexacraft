class_name GameVersion

var id: String
var name: String
var release_date: String
var download_url: String
var file_to_run: String

func _init(id: String, name: String, release_date: String, download_url: String, file_to_run: String):
    self.id = id
    self.name = name
    self.release_date = release_date
    self.download_url = download_url
    self.file_to_run = file_to_run

func asDict():
    var dict = {}
    
    dict["id"] = id
    dict["name"] = name
    dict["release_date"] = release_date
    dict["url"] = download_url
    dict["file_to_run"] = file_to_run
    
    return dict
