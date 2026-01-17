use std::collections::VecDeque;

use bytes::Bytes;
use tokio::sync::Mutex;
use zeromq::{
    DealerSocket, RouterSocket, SocketOptions, ZmqError, ZmqMessage, ZmqResult, prelude::*,
    util::PeerIdentity,
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

pub struct ServerSocket {
    socket: Mutex<RouterSocket>,
    received_frames: Mutex<VecDeque<Bytes>>,
    cancel_token: tokio_util::sync::CancellationToken,
}

impl Drop for ServerSocket {
    fn drop(&mut self) {
        self.cancel();
    }
}

impl ServerSocket {
    pub fn new() -> Self {
        Self {
            socket: Mutex::new(RouterSocket::new()),
            received_frames: Mutex::new(VecDeque::new()),
            cancel_token: tokio_util::sync::CancellationToken::new(),
        }
    }

    pub async fn bind(&self, port: u16) -> ZmqResult<()> {
        self.socket
            .lock()
            .await
            .bind(&format!("tcp://0.0.0.0:{port}"))
            .await?;
        Ok(())
    }

    pub async fn receive(&self) -> ZmqResult<Vec<u8>> {
        let mut received_frames = self.received_frames.lock().await;
        if received_frames.is_empty() {
            let msg = loop {
                let mut socket = self.socket.lock().await;

                match tokio::select! {
                    _ = self.cancel_token.cancelled() => {
                        return Err(ZmqError::Other("cancelled"));
                    }
                    res = socket.recv() => res,
                } {
                    Ok(data) => break data,
                    Err(ZmqError::Codec(err)) => {
                        // the client probably sent an unknown command, so we ignore it
                        eprintln!("Server socket: Got codec error: {:?}", err);
                    }
                    Err(err) => {
                        eprintln!("Server socket: Got error: {:?}", err);
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

    pub async fn send(&self, client_id: Vec<u8>, data: Vec<u8>) -> ZmqResult<()> {
        let mut msg = ZmqMessage::from(client_id);
        msg.push_back(data.into());
        self.socket.lock().await.send(msg).await
    }

    pub fn cancel(&self) {
        if !self.cancel_token.is_cancelled() {
            self.cancel_token.cancel();
        }
    }
}

#[cfg(test)]
mod tests {
    use zeromq::{ZmqError, ZmqResult};

    use crate::zmq;

    #[tokio::test]
    async fn roundtrip() -> ZmqResult<()> {
        let client_id = vec![b'A', b'B', b'C'];

        let server = zmq::ServerSocket::new();
        server.bind(1234).await?;

        let client = zmq::ClientSocket::new(client_id.clone());
        client.connect("localhost", 1234).await?;

        // Send message from client to server
        client.send(vec![1, 2, 3]).await?;
        
        let client_id_msg = server.receive().await?;
        let data_msg = server.receive().await?;

        assert_eq!(client_id_msg, vec![b'A', b'B', b'C']);
        assert_eq!(data_msg, vec![1, 2, 3]);

        // Send message from server to client
        server.send(client_id.clone(), vec![4, 5, 6]).await?;
        
        let data_msg = client.receive().await?;
        assert_eq!(data_msg, vec![4, 5, 6]);

        // Shutdown server which should immediately cancel receive calls
        let server_rx = server.receive();
        server.cancel();

        match server_rx.await {
            Err(ZmqError::Other("cancelled")) => {}
            res => panic!("got {res:?}"),
        };

        Ok(())
    }

    #[tokio::test]
    async fn roundtrip_2_clients() -> ZmqResult<()> {
        let client_id1 = vec![b'A', b'B', b'C'];
        let client_id2 = vec![b'A', b'B', b'D'];

        let server = zmq::ServerSocket::new();
        server.bind(1235).await?;

        let client1 = zmq::ClientSocket::new(client_id1.clone());
        client1.connect("localhost", 1235).await?;

        // Send message from client to server
        client1.send(vec![1, 2, 3]).await?;
        
        let client_id_msg = server.receive().await?;
        let data_msg = server.receive().await?;

        assert_eq!(client_id_msg, vec![b'A', b'B', b'C']);
        assert_eq!(data_msg, vec![1, 2, 3]);

        let client2 = zmq::ClientSocket::new(client_id2.clone());
        client2.connect("localhost", 1235).await?;

        // Send message from client to server
        client2.send(vec![1, 2, 4]).await?;
        
        let client_id_msg = server.receive().await?;
        let data_msg = server.receive().await?;

        assert_eq!(client_id_msg, vec![b'A', b'B', b'D']);
        assert_eq!(data_msg, vec![1, 2, 4]);

        // Send message from server to client
        server.send(client_id1.clone(), vec![4, 5, 6]).await?;
        server.send(client_id2.clone(), vec![4, 5, 7]).await?;
        
        let data_msg = client1.receive().await?;
        assert_eq!(data_msg, vec![4, 5, 6]);
        
        let data_msg = client2.receive().await?;
        assert_eq!(data_msg, vec![4, 5, 7]);

        // Shutdown server which should immediately cancel receive calls
        let server_rx = server.receive();
        server.cancel();

        match server_rx.await {
            Err(ZmqError::Other("cancelled")) => {}
            res => panic!("got {res:?}"),
        };

        Ok(())
    }
}
