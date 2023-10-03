package main

import (
	"context"
	"io"
	"log"
	"net"
)

func main() {
	ln, err := net.Listen("tcp", ":8091")
	if err != nil {
		log.Fatalf("Error bootstrapping server: %v", err)
	}
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	for {
		conn, err := ln.Accept()
		if err != nil {
			log.Printf("Error accepting connection: %v", err)
		}
		go func(ctx context.Context, conn net.Conn) {
			serveConn(ctx, conn)
		}(ctx, conn)
	}
}

func serveConn(ctx context.Context, conn net.Conn) {
	log.Printf("Start serving connection: %v", conn.RemoteAddr().String())
	buf := make([]byte, 1024)
	for {
		select {
		case <-ctx.Done():
			log.Printf("Gracefully shutting down, stop serving connection: %v", conn.RemoteAddr().String())
			return
		default:
			err := echoConn(conn, buf)
			if err != nil {
				log.Printf("Stop serving connection: %v, due to error %v", conn.RemoteAddr().String(), err)
				return
			}
		}
	}
}

func echoConn(conn net.Conn, buf []byte) error {
	num, readErr := conn.Read(buf)
	_, writeErr := conn.Write(buf[:num])

	if readErr == io.EOF { //This redundancy is left here deliberately
		return readErr
	} else if readErr != nil {
		return readErr
	}
	if writeErr != nil {
		return writeErr
	}
	return nil
}
