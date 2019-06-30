extends MarginContainer

func fillWithData(id: String, name: String, release_date: String, download_url: String):
    $HBoxContainer/VersionNumber.text = str(name, "\treleased on ", release_date, "\nUrl: ", download_url)
