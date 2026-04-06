use crate::server::request::NetworkPacket;
use crate::zmq::ServerSocket;
use std::sync::Arc;
use std::time::Duration;

pub use state::GameState;

mod nbt;
mod request;
mod response;
mod state;
mod world;

pub trait RequestHandler {
    fn handle(&self, client_id: u64, packet: NetworkPacket) -> Option<nbt::Tag>;
}

pub trait GracefulShutdown {
    fn initiate(&self);
    fn done(&self) -> bool;
}

pub struct GameServer<H> {
    socket: Arc<ServerSocket>,
    handler: H,
}

impl<H> GameServer<H> {
    pub async fn start(port: u16, handler: H) -> Self {
        let socket = ServerSocket::new();
        socket.bind(port).await.unwrap();
        Self {
            socket: Arc::new(socket),
            handler,
        }
    }
}

impl<H: GracefulShutdown> GameServer<H> {
    pub async fn shutdown(&self) {
        self.handler.initiate();
        loop {
            if self.handler.done() {
                break;
            }
            tokio::time::sleep(Duration::from_millis(10)).await;
        }
    }
}

impl<H: RequestHandler> GameServer<H> {
    pub async fn run_receiver(&self) {
        loop {
            let client_id_bytes = self.socket.receive().await.unwrap();
            let message = self.socket.receive().await.unwrap();

            match decode_request(&client_id_bytes, &message) {
                Err(err) => eprintln!("{err}"),
                Ok((client_id, packet)) => {
                    if let Some(response) = self.handler.handle(client_id, packet) {
                        self.socket
                            .send(client_id_bytes, response.to_binary())
                            .await
                            .unwrap();
                    }
                }
            }
        }
    }
}

fn decode_request(
    client_id_bytes: &[u8],
    message_bytes: &[u8],
) -> Result<(u64, NetworkPacket), String> {
    let client_id =
        decode_client_id(client_id_bytes).map_err(|err| format!("Got invalid client id: {err}"))?;
    let packet =
        decode_message(message_bytes).map_err(|err| format!("Got invalid message: {err}"))?;
    Ok((client_id, packet))
}

fn decode_client_id(client_id_bytes: &[u8]) -> Result<u64, &'static str> {
    String::from_utf8(client_id_bytes.to_vec())
        .map_err(|_| "client id was not utf8")?
        .parse::<u64>()
        .map_err(|_| "client id was not a positive integer")
}

fn decode_message(message_bytes: &[u8]) -> Result<NetworkPacket, String> {
    let (_, tag) = nbt::Tag::from_binary(message_bytes)?;

    let packet = match tag {
        nbt::Tag::Map(vs) => {
            if vs.len() != 1 {
                Err("packet did not have exactly 1 field")?;
            }
            let (name, tag) = &vs[0];
            NetworkPacket::decode(name, tag.clone())?
        }
        _ => Err("packet was not a Map tag")?,
    };

    Ok(packet)
}
