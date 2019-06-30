extends HBoxContainer

# Declare member variables here. Examples:
# var a = 2
# var b = "text"

# Called when the node enters the scene tree for the first time.
func _ready():
    pass # Replace with function body.

# Called every frame. 'delta' is the elapsed time since the previous frame.
#func _process(delta):
#    pass


func _on_Play_pressed():
    var list = $VersionListPanel/ScrollContainer/VersionList
    
    if !game_version_downloaded():
        download_game_version()
    run_game(version)
    pass # Replace with function body.


func _on_Refresh_pressed():
    pass # Replace with function body.


func _on_AddManually_pressed():
    pass # Replace with function body.
