use std::collections::VecDeque;

use bytes::Bytes;
use tokio::sync::Mutex;
use zeromq::{
    DealerSocket, SocketOptions, ZmqError, ZmqMessage, ZmqResult, prelude::*, util::PeerIdentity,
};

pub struct ClientSocket {
    socket: Mutex<DealerSocket>,
    received_frames: Mutex<VecDeque<Bytes>>,
}

impl ClientSocket {
    pub fn new(client_id: Vec<u8>) -> Self {
        let mut options = SocketOptions::default();
        options.peer_identity(PeerIdentity::try_from(client_id).unwrap());

        Self {
            socket: Mutex::new(DealerSocket::with_options(options)),
            received_frames: Mutex::new(VecDeque::new()),
        }
    }

    pub async fn connect(&self, host: &str, port: u16) -> ZmqResult<()> {
        self.socket
            .lock()
            .await
            .connect(&format!("tcp://{host}:{port}"))
            .await
    }

    pub async fn send(&self, data: Vec<u8>) -> ZmqResult<()> {
        self.socket.lock().await.send(ZmqMessage::from(data)).await
    }

    pub async fn receive(&self) -> ZmqResult<Vec<u8>> {
        let mut received_frames = self.received_frames.lock().await;
        if received_frames.is_empty() {
            let msg = loop {
                match self.socket.lock().await.recv().await {
                    Ok(data) => break data,
                    Err(ZmqError::Codec(err)) => {
                        // the server probably sent an unknown command, so we ignore it
                        eprintln!("Client socket: Got codec error: {:?}", err);
                    }
                    Err(err) => {
                        eprintln!("Client socket: Got error: {:?}", err);
                        return Err(err);
                    }
                };
            };
            for frame in msg.into_vec() {
                received_frames.push_back(frame);
            }
        }
        Ok(received_frames.pop_front().unwrap().to_vec())
    }
}
