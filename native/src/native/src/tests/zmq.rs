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
