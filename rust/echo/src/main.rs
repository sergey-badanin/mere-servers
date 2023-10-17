extern crate core;

use std::io;
use std::io::{Read, Write};
use std::net::{TcpListener, TcpStream};

use log::{info, Level, LevelFilter, Metadata, Record, SetLoggerError};

use echo_server::ThreadPool;

static LOGGER: SimpleLogger = SimpleLogger;

pub fn init() -> Result<(), SetLoggerError> {
    log::set_logger(&LOGGER).map(|()| log::set_max_level(LevelFilter::Info))
}

fn main() -> io::Result<()> {
    if let Err(err) = init() {
        panic!("Error while initializing logger: {:?}", err);
    }

    let host = "0.0.0.0";
    let port = 8089;
    let pool_size = 4;

    info!("Start listening on port: {port}");
    let listener = TcpListener::bind((host, port))?;
    let pool = ThreadPool::new(pool_size);

    for stream in listener.incoming() {
        info!("New incoming connection");

        pool.execute(|| handle_client(stream.unwrap()).unwrap());
    }
    Ok(())
}

fn handle_client(mut stream: TcpStream) -> io::Result<()> {
    let sock_addr = stream.peer_addr()?;
    info!("Handling connection: {sock_addr}");
    let mut buf = [0; 1024];
    while let Ok(n) = stream.read(&mut buf) {
        if n == 0 {
            info!("No data received, connection is to be closed");
            return Ok(());
        }
        info!("Bytes read from stream: {n}");
        stream.write(&buf[..n])?;
    }
    Ok(())
}

struct SimpleLogger;

impl log::Log for SimpleLogger {
    fn enabled(&self, metadata: &Metadata) -> bool {
        metadata.level() <= Level::Info
    }

    fn log(&self, record: &Record) {
        if self.enabled(record.metadata()) {
            println!("{} - {}", record.level(), record.args());
        }
    }

    fn flush(&self) {}
}
