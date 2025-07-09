fn main() {
    println!("cargo:rerun-if-changed=packaging/windows/resources.rc");
    embed_resource::compile("packaging/windows/resources.rc", embed_resource::NONE).manifest_optional().unwrap();
}
