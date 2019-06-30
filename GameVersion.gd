class_name GameVersion

var id: String
var name: String
var release_date: String
var download_url: String
var download_size: int

func _init(id: String, name: String, release_date: String, download_url: String, download_size: int):
    self.id = id
    self.name = name
    self.release_date = release_date
    self.download_url = download_url
    self.download_size = download_size
